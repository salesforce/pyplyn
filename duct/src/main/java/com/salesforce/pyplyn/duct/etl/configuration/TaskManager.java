package com.salesforce.pyplyn.duct.etl.configuration;

import static com.salesforce.pyplyn.util.CollectionUtils.immutableOrEmptySet;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.PollingTransform;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.Transmutation;
import com.salesforce.pyplyn.processor.ExtractProcessor;
import com.salesforce.pyplyn.processor.LoadProcessor;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.parallel.ParallelFailureHandling;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Responsible for keeping track of currently subscribed tasks
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
@Singleton
public class TaskManager<T extends Configuration> {
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    private final Set<ExtractProcessor<? extends Extract>> extractProcessors;
    private final Set<LoadProcessor<? extends Load>> loadProcessors;
    private final ShutdownHook shutdownHook;
    private Injector injector;

    // dedicated schedulers
    private final Scheduler extractScheduler;
    private final Scheduler transformScheduler;
    private final Scheduler loadScheduler;

    private final ConcurrentHashMap<T, Subscription> ACTIVE_SUBSCRIPTIONS = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<T, Disposable> ACTIVE_PUBLISHERS = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<T, Instant> LAST_EXECUTED = new ConcurrentHashMap<>();

    private final CountDownLatch HAS_STARTED_PROCESSING = new CountDownLatch(1);
    private final CountDownLatch HAS_COMPLETED_PROCESSING = new CountDownLatch(1);

    private final AtomicInteger CURRENTLY_PROCESSING_COUNTER = new AtomicInteger(0);
    private AtomicInteger TASK_COUNTER = new AtomicInteger(0);
    private final AtomicLong DROPPED_COUNTER = new AtomicLong();

    private final boolean runOnce;
    private final Random random = new Random(System.nanoTime());
    private final double taskDelayCoefficient;

    /**
     * Class constructor
     */
    @Inject
    public TaskManager(AppConfig config,
            Set<ExtractProcessor<? extends Extract>> extractProcessors,
            Set<LoadProcessor<? extends Load>> loadProcessors,
            ShutdownHook shutdownHook,
            Injector injector) {
        this.extractProcessors = extractProcessors;
        this.loadProcessors = loadProcessors;
        this.runOnce = config.global().runOnce();
        this.shutdownHook = shutdownHook;
        this.injector = injector;
        this.taskDelayCoefficient = config.global().taskDelayCoefficient();

        // prioritize tasks based on their place in the pipeline
        Integer ioPoolSize = config.global().ioPoolsThreadSize();
        extractScheduler = initExtractScheduler(ioPoolSize);
        transformScheduler = initTransformScheduler(ioPoolSize);
        loadScheduler = initLoadScheduler(ioPoolSize);

        // handle irrecoverable errors: allow graceful shutdown
        RxJavaPlugins.setErrorHandler(throwable -> {
            onError(throwable);

            // mark processing as completed after a short delay
            Flowable.timer(1, TimeUnit.SECONDS).doOnNext((i) -> notifyCompleted()).subscribe();
        });
    }

    /**
     * Initializes a scheduler that will be used for offloading IO work performed by {@link Extract}s
     * <p/>
     * <p/> Threads executed on this scheduler have {@link Thread#NORM_PRIORITY}
     * @param ioPoolSize Size of thread pool for this scheduler
     */
    private Scheduler initExtractScheduler(Integer ioPoolSize) {
        ThreadFactory factory = newThreadFactory("TaskManager-Extract-%s", Thread.NORM_PRIORITY);
        ExecutorService executor = Executors.newFixedThreadPool(ioPoolSize, factory);
        shutdownHook.registerExecutor(executor);
        return Schedulers.from(executor);
    }

    /**
     * Initializes a scheduler that will be used for offloading IO work performed by {@link PollingTransform}s
     * <p/>
     * <p/> Threads executed on this scheduler have {@link Thread#NORM_PRIORITY+1}
     * @param ioPoolSize Size of thread pool for this scheduler
     */
    private Scheduler initTransformScheduler(Integer ioPoolSize) {
        ThreadFactory factory = newThreadFactory("TaskManager-Transform-%s", Thread.NORM_PRIORITY + 1);
        ExecutorService executor = Executors.newFixedThreadPool(ioPoolSize, factory);
        shutdownHook.registerExecutor(executor);
        return Schedulers.from(executor);
    }

    /**
     * Initializes a scheduler that will be used for offloading IO work performed by {@link Load}s
     * <p/>
     * <p/> Threads executed on this scheduler have {@link Thread#NORM_PRIORITY}+2
     * @param ioPoolSize Size of thread pool for this scheduler
     */
    private Scheduler initLoadScheduler(Integer ioPoolSize) {
        ThreadFactory factory = newThreadFactory("TaskManager-Load-%s", Thread.NORM_PRIORITY + 2);
        ExecutorService executor = Executors.newFixedThreadPool(ioPoolSize, factory);
        shutdownHook.registerExecutor(executor);
        return Schedulers.from(executor);
    }

    /**
     * Creates a new {@link ThreadFactory}
     *
     * @param nameFormat name format to use
     * @param priority priority that will be assigned to any threads constructed by this factory
     */
    private ThreadFactory newThreadFactory(String nameFormat, int priority) {
        return new ThreadFactoryBuilder().setNameFormat(nameFormat).setDaemon(true).setPriority(priority).build();
    }


    //
    // TASK MANAGEMENT
    //

    /**
     * Inserts or updates a task
     * <p/>
     * <p/> The implementation will take care to:
     * <p/> - add the task to the {@link #ACTIVE_SUBSCRIPTIONS} container when an observer subscribes
     * <p/> - and to remove the task once it's been disposed
     */
    public void upsert(T task) {
        injectMembers(task);

        Disposable disposable = createTask(task)

                // ETL cycle
                .flatMap((T configuration) -> {
                    // EXTRACT

                    // merge all Extract results
                    Flowable<List<List<Transmutation>>> transformed = Flowable.fromIterable(extractProcessors)
                            .parallel()
                            .runOn(extractScheduler)
                            .map(processor -> processor.executeAsync(configuration.extract()), ParallelFailureHandling.ERROR)
                            .flatMap(s -> s)
                            .reduce((list, items) -> Stream.concat(list.stream(), items.stream()).collect(toList()));


                    // TRANSFORM
                    for (Transform transform : configuration.transform()) {
                        // PollingTransforms are executed on a dedicated scheduler
                        if (transform instanceof PollingTransform) {
                            transformed = transformed.flatMap(tr -> transform.applyAsync(tr, transformScheduler));

                        // standard transforms are observed on the computation scheduler
                        } else {
                            transformed = transformed.flatMap(tr -> transform.applyAsync(tr, Schedulers.computation()));
                        }
                    }

                    // LOAD
                    return transformed
                            // process each row individually
                            // TODO: update this to send full dataset to each processor, instead of sending each row individually
                            .flatMap(Flowable::fromIterable)

                            // for each row an loadProcessor combination, apply
                            .flatMap(resultRow -> Flowable.fromIterable(loadProcessors)
                                            .parallel()
                                            .runOn(loadScheduler)
                                            .map(loadProcessor -> loadProcessor.executeAsync(resultRow, configuration.load()), ParallelFailureHandling.RETRY)
                                            .flatMap(s -> s)
                                            .reduce((all, r) -> Stream.concat(all.stream(), r.stream()).collect(toList()))
                            );
                })

                // lifecycle management
                .doFinally(this::hookAfterTaskProcessed)
                .doFinally(CURRENTLY_PROCESSING_COUNTER::decrementAndGet)
                .doOnSubscribe(subscription -> CURRENTLY_PROCESSING_COUNTER.incrementAndGet())

                // handle results and errors
                .doOnNext(results -> logger.info("Got results {} (counter={})", results, TASK_COUNTER.incrementAndGet()))
                .doOnError(this::onError)

                // Process tasks
                .subscribeOn(Schedulers.computation())
                .subscribe();

        // register publisher and dispose previous one (if exists)
        Optional.ofNullable(ACTIVE_PUBLISHERS.put(task, disposable)).ifPresent(Disposable::dispose);
    }

    /**
     * Inject any dependency injected member variables of a task's extract, transform, and load targets using
     * Pyplyn's {@link Injector}.
     * <p>
     * This is necessary because when running in Hazelcast mode Pyplyn can potentially serialize/deserialize
     * configurations, causing injected member variables that were marked as transient to be nulled out. Until
     * a more elegant solution can be implemented this is necessary to make Hazelcast mode compatible
     * with implementations of {@link PollingTransform}.
     * 
     * @param task The task to perform injection on.
     */
    void injectMembers(T task) {
        task.extract().forEach(e -> injector.injectMembers(e));
        task.transform().forEach(t -> injector.injectMembers(t));
        task.load().forEach(l -> injector.injectMembers(l));
    }

    public Flowable<T> createTask(T task) {

        return Flowable.interval(getTaskDelayMillis(task), task.repeatIntervalMillis(), TimeUnit.MILLISECONDS)

                // prevent configurations from running too often
                .onBackpressureDrop(t -> {
                    logger.warn("Task is being dropped due to backpressure. Lifetime dropped tasks: {}.", DROPPED_COUNTER.incrementAndGet());
                })

                .map(i -> task)
                // stop if shutting down
                .takeWhile(t -> !shutdownHook.isShutdown())

                // stop after the first task if only running once
                .takeUntil(ignored -> runOnce)

                // remove subscriptions that are disposed or completed
                .doFinally(() -> ACTIVE_SUBSCRIPTIONS.remove(task))

                // lifecycle management
                .doOnSubscribe(subscription -> Optional.ofNullable(ACTIVE_SUBSCRIPTIONS.put(task, subscription)).ifPresent(Subscription::cancel))
                .doOnNext(disposableTask -> HAS_STARTED_PROCESSING.countDown())

                .delay((item) -> {
                    // get last execution time
                    Instant lastRun = LAST_EXECUTED.computeIfAbsent(task,
                            // or trigger a run by creating an Instant at the point in time where the configuration should have run last
                            t -> Instant.now().minusMillis(task.repeatIntervalMillis()));

                    // compute duration between lastRun and now; D=lastRun-lastRun
                    Duration duration = Duration.between(Instant.now(), lastRun)
                            // subtract the repeat interval; D=repeatInterval-(lastRun-now)
                            .plusMillis(task.repeatIntervalMillis());

                    // D<=0; need to run now
                    if (duration.isNegative() || duration.isZero()) {
                        return Flowable.just(0L);

                        // D>0; need to run in D
                    } else {
                        return Flowable.timer(duration.toMillis(), TimeUnit.MILLISECONDS);
                    }
                })

                // mark the time at which we ran last
                .doOnNext(results -> LAST_EXECUTED.put(task, Instant.now()));
    }

    protected long getTaskDelayMillis(T task) {
        // this is package-private for testing purposes
        // delay the task by a random amount in the interval [0, k * task.repeatIntervalMillis()], where k is the taskDelayCoefficient
        long result = (long)(random.nextDouble() * taskDelayCoefficient * task.repeatIntervalMillis());
        logger.info("Generated task start delay of {} ms. (Repeat ineterval {} ms, configured coefficient {}.)", result,
                task.repeatIntervalMillis(), taskDelayCoefficient);
        return result;
    }

    /**
     * De-register tasks
     */
    public void remove(T task) {
        // if a publisher is present, cancel it and remove it from the map
        ACTIVE_PUBLISHERS.computeIfPresent(task, (key, publisher) -> {
            publisher.dispose();
            return null;
        });

        // if a subscription is present, cancel it and remove it from the map
        ACTIVE_SUBSCRIPTIONS.computeIfPresent(task, (key, subscription) -> {
            subscription.cancel();
            return null;
        });

        // finally remove the last execution time, for memory management
        LAST_EXECUTED.remove(task);
    }

    /**
     * @return all known tasks
     */
    public Set<T> allTasks() {
        ArrayList<T> taskList = Collections.list(ACTIVE_PUBLISHERS.keys());
        return immutableOrEmptySet(new HashSet<>(taskList));
    }


    //
    // LIFECYCLE MANAGEMENT
    //

    /**
     * Log errors
     */
    public void onError(Throwable e) {

        if(e instanceof UndeliverableException){
            e = e.getCause();
        }
        if (e instanceof IOException) {
            logger.info("Likely a network problem or API that throws on cancellation");
        }
        if (e instanceof InterruptedException) {
            logger.info("Some blocking code likely was interrupted by a dispose call");
            return;
        }
        if (e instanceof InterruptedException) {
            logger.info("Some blocking code was interrupted by a dispose call");
            return;
        }
        logger.warn("Unexpected exception", e);
    }

    /**
     * This method enables {@link ConfigurationUpdateManager} to signal that there is no work to begin with,
     *   allowing for a clean shutdown in runOnce mode
     */
    protected void notifyCompleted() {
        HAS_STARTED_PROCESSING.countDown();
        HAS_COMPLETED_PROCESSING.countDown();
    }

    public void completeIfRunningOnceWithoutAnyTasks() {
        if (runOnce) {
            notifyCompleted();
        }
    }

    /**
     * Awaits until all tasks have been scheduled and processed
     * @throws InterruptedException if interrupted while waiting
     */
    public void awaitUntilFinished() throws InterruptedException {
        // wait until processing has started
        HAS_STARTED_PROCESSING.await();
        HAS_COMPLETED_PROCESSING.await();
    }

    /**
     * Called each time a task has been processed
     * <p/> determines if all work has completed (runOnce mode)
     */
    protected void hookAfterTaskProcessed() {
        // if processing has started, all tasks have completed and the subscriptions cancelled
        if (HAS_STARTED_PROCESSING.getCount() == 0 && ACTIVE_SUBSCRIPTIONS.size() == 0 && CURRENTLY_PROCESSING_COUNTER.get() == 0) {
            HAS_COMPLETED_PROCESSING.countDown();
        }
    }
}
