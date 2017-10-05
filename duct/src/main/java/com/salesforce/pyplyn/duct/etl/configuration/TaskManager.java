package com.salesforce.pyplyn.duct.etl.configuration;

import static com.salesforce.pyplyn.util.CollectionUtils.immutableOrEmptySet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.model.*;
import com.salesforce.pyplyn.processor.ExtractProcessor;
import com.salesforce.pyplyn.processor.LoadProcessor;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
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

    private final boolean runOnce;


    /**
     * Class constructor
     */
    @Inject
    public TaskManager(AppConfig config,
                       Set<ExtractProcessor<? extends Extract>> extractProcessors,
                       Set<LoadProcessor<? extends Load>> loadProcessors,
                       ShutdownHook shutdownHook) {
        this.extractProcessors = extractProcessors;
        this.loadProcessors = loadProcessors;
        this.runOnce = config.global().runOnce();
        this.shutdownHook = shutdownHook;

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

    public Flowable<T> createTask(T task) {
        return Flowable.interval(0, task.repeatIntervalMillis(), MILLISECONDS)

                // prevent configurations from running too often
                .onBackpressureDrop()

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
                            // substract the repeat interval; D=repeatInterval-(lastRun-now)
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
