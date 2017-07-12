package com.salesforce.pyplyn.duct.etl.configuration;

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
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
        extractScheduler = initExtractScheduler();
        transformScheduler = initTransformScheduler();
        loadScheduler = initLoadScheduler();
    }

    /**
     * Initializes a scheduler that will be used for offloading IO work performed by {@link Extract}s
     * <p/>
     * <p/> Threads executed on this scheduler have {@link Thread#NORM_PRIORITY}
     */
    private Scheduler initExtractScheduler() {
        ThreadFactory factory = newThreadFactory("TaskManager-Extract-%s", Thread.NORM_PRIORITY);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), factory);
        shutdownHook.registerExecutor(executor);
        return Schedulers.from(executor);
    }

    /**
     * Initializes a scheduler that will be used for offloading IO work performed by {@link PollingTransform}s
     * <p/>
     * <p/> Threads executed on this scheduler have {@link Thread#NORM_PRIORITY+1}
     */
    private Scheduler initTransformScheduler() {
        ThreadFactory factory = newThreadFactory("TaskManager-Transform-%s", Thread.NORM_PRIORITY + 1);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), factory);
        shutdownHook.registerExecutor(executor);
        return Schedulers.from(executor);
    }

    /**
     * Initializes a scheduler that will be used for offloading IO work performed by {@link Load}s
     * <p/>
     * <p/> Threads executed on this scheduler have {@link Thread#NORM_PRIORITY}+2
     */
    private Scheduler initLoadScheduler() {
        ThreadFactory factory = newThreadFactory("TaskManager-Load-%s", Thread.NORM_PRIORITY + 2);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), factory);
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
     * <p/> - add the task to the {@param ACTIVE_SUBSCRIPTIONS} container when an observer subscribes
     * <p/> - and to remove the task once it's been disposed
     */
    public void upsert(T task) {
        Disposable disposable = createObservable(task)
                // do not emit any items until subscribed
                .publish().autoConnect()

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
                .doOnNext(results -> LAST_EXECUTED.put(task, Instant.now()))

                // ETL cycle
                .flatMap(configuration -> {
                    // EXTRACT

                    // merge all Extract results
                    Flowable<List<List<TransformationResult>>> transformed =
                            Flowable.merge(
                                    Flowable.fromIterable(extractProcessors).observeOn(extractScheduler)

                                            // filter out types (Extract[]) that cannot be processed and return an Async result
                                            .map(processor -> processor.executeAsync(configuration.extract()))
                            );

                    // combine all Extract results into a single List<List<TransformationResult>>
                    final List<List<TransformationResult>> extractsCollector = new ArrayList<>();
                    Single<List<List<TransformationResult>>> mergedResultLists = transformed.reduce(extractsCollector, (list, items) -> {
                        list.addAll(items);
                        return list;
                    });
                    transformed = mergedResultLists.toFlowable();


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
                    final List<Boolean> loadCollector = new ArrayList<>();
                    Flowable<List<Boolean>> loadStage = transformed

                            // process each row individually
                            .flatMap(Flowable::fromIterable)

                            // for each row an loadProcessor combination, apply
                            .flatMap(resultRow -> Flowable.fromIterable(loadProcessors).observeOn(loadScheduler)
                                    .map(loadProcessor -> loadProcessor.executeAsync(resultRow, configuration.load()))
                            )

                            // reduce all results into a List<Boolean>
                            .flatMap(s -> s)
                            .reduce(loadCollector, (list, items) -> {
                                list.addAll(items);
                                return list;
                            }).toFlowable();

                    return loadStage;
                })

                // lifecycle management
                .doFinally(this::hookAfterTaskProcessed)
                .doFinally(CURRENTLY_PROCESSING_COUNTER::decrementAndGet)
                .doOnSubscribe(subscription -> CURRENTLY_PROCESSING_COUNTER.incrementAndGet())

                // handle results and errors
                .doOnNext(results -> logger.info("Got results for {}: {}", results, TASK_COUNTER.incrementAndGet()))
                .doOnError(this::onError)

                // Process tasks
                .subscribeOn(Schedulers.computation())
                .subscribe();

        // register publisher and dispose previous one (if exists)
        Optional.ofNullable(ACTIVE_PUBLISHERS.put(task, disposable)).ifPresent(Disposable::dispose);
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
     * @return either an observable stream for the specified task at each interval or a single one (runOnce mode)
     */
    private Flowable<T> createObservable(T task) {
        if (!runOnce) {
            // issue tasks indefinitely
            return Flowable.interval(0, task.repeatIntervalMillis(), MILLISECONDS)
                    .map(i -> task)
                    .takeWhile(t -> !shutdownHook.isShutdown());

        } else {
            // issue task only once
            return Flowable.just(task);
        }
    }

    public void onError(Throwable e) {
        logger.warn("Unexpected exception", e);
    }



    //
    // LIFECYCLE MANAGEMENT
    //

    /**
     * This method enables {@link ConfigurationUpdateManager} to signal that there is no work to begin with,
     *   allowing for a clean shutdown in runOnce mode
     */
    protected void notifyCompleted() {
        HAS_STARTED_PROCESSING.countDown();
        HAS_COMPLETED_PROCESSING.countDown();
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
