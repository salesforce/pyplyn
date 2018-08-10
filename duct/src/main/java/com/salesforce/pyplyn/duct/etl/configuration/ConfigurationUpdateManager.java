package com.salesforce.pyplyn.duct.etl.configuration;

import static com.salesforce.pyplyn.util.CollectionUtils.immutableOrEmptySet;
import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.MigrationEvent;
import com.hazelcast.core.MigrationListener;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.util.Preconditions;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.cluster.Cluster;

import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Updates the current configurations, creating, updating, and removing tasks for each one
 * <p/>
 * <p/> If exceptions occur while processing configurations, the errors are logged and no task is updated.
 * <p/> If any configuration exceptions occur during the program's bootstrap, execution is stopped altogether.
 *
 * TODO: write integration tests for this functionality
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
@Singleton
public class ConfigurationUpdateManager implements Runnable, Provider<Set<Configuration>> {
    protected static final Logger logger = LoggerFactory.getLogger(ConfigurationUpdateManager.class);
    private static final String CONFIGURATION_MAP_KEY = "configurations";

    private final ConfigurationLoader loader;
    private final TaskManager<Configuration> taskManager;
    private final Cluster cluster;
    private final ShutdownHook shutdownHook;
    private final Injector injector;

    private Map<String, Configuration> configurations;
    private final CountDownLatch IS_CONFIGURED_LATCH = new CountDownLatch(1);

    @Inject
    public ConfigurationUpdateManager(ConfigurationLoader loader, TaskManager<Configuration> taskManager, Cluster cluster, ShutdownHook shutdownHook,
            Injector injector) {
        this.loader = loader;
        this.taskManager = taskManager;
        this.cluster = cluster;
        this.shutdownHook = shutdownHook;
        this.injector = injector;
    }

    /**
     * Initialize configuration set, based on cluster availability
     */
    public void initialize() {
        Preconditions.checkState(isNull(configurations), "Cannot initialize the Configuration Update Manager more than once!");

        if (cluster.isEnabled()) {

            // manage cluster node events
            cluster.registerListener(new ClusterEventListener());

            // manage cluster partition migration events
            cluster.registerListener(new ClusterMigrationListener());

            // registers task listener to manage changes in the configuration map
            IMap<String, Configuration> hzMap = cluster.distributedMap(CONFIGURATION_MAP_KEY);
            hzMap.addLocalEntryListener(new ConfigurationMapListener());
            configurations = hzMap;

        } else {
            configurations = new HashMap<>();
        }
    }

    /**
     * Periodically updates the set of known configurations
     */
    @Override
    public void run() {
        if (cluster.isEnabled() && !cluster.isMaster()) {
            logger.info("Skipping configuration update on this node (not master)");

            updateTasksAfterClusterEvent();
            return;
        }

        // load configurations from disk and filter out disabled ones
        try {
            Set<Configuration> latestConfigurationSet = loader.load().stream().filter(c -> !c.disabled()).collect(Collectors.toSet());

            // delete configurations
            ImmutableSet<Configuration> deleted = ImmutableSet.copyOf(Sets.difference(configurations(), latestConfigurationSet));
            deleted.forEach(new DeleteTaskConsumer().andThen(new DeleteConfigurationConsumer()));

            // insert or update new configurations
            latestConfigurationSet.forEach(new UpsertTaskConsumer().andThen(new UpsertConfigurationConsumer()));
            logger.info("Updated configuration set; {} configs upserted, {} configs deleted", latestConfigurationSet.size(), deleted.size());

            // if running in runOnce mode, stop immediately if there are no configurations to process
            if (latestConfigurationSet.isEmpty()) {
                taskManager.completeIfRunningOnceWithoutAnyTasks();
            }

        } catch (RuntimeException e) {
            logger.warn("Unexpected exception while processing configurations", e);

            // if this is the first run, rethrow the exception to cause the program to stop
            if (IS_CONFIGURED_LATCH.getCount() > 0) {
                taskManager.notifyCompleted();
                shutdownHook.shutdown();
                throw e;
            }

        } finally {
            // mark that we have processed configurations
            IS_CONFIGURED_LATCH.countDown();
        }
    }

    /**
     * @return Set of {@link Configuration}s associated with the current node, or all configurations if not running in a cluster
     */
    @Override
    public Set<Configuration> get() {
        Map<String, Configuration> localConfigurations;

        // load local configurations from Hazelcast
        if (cluster.isEnabled()) {
            IMap<String, Configuration> map = (IMap<String, Configuration>) configurations;
            localConfigurations = map.getAll(map.localKeySet());

            // or return all known configurations
        } else {
            localConfigurations = configurations;
        }

        return immutableOrEmptySet(new HashSet<>(localConfigurations.values()));
    }

    /**
     *  @return Set of all known {@link Configuration}s
     */
    Set<Configuration> configurations() {
        return immutableOrEmptySet(new HashSet<>(configurations.values()));
    }



    /**
     * Ensures only tasks that should be running are running
     */
    private void updateTasksAfterClusterEvent() {
        logger.info("Synchronizing tasks on local node...");

        // compute the difference between currently executing tasks and locally managed configurations
        Set<Configuration> localConfigurations = get();
        Set<Configuration> localTasks = taskManager.allTasks();
        ImmutableSet.copyOf(Sets.difference(localTasks, localConfigurations))
                // and delete all the tasks that should not run on the local node
                .forEach(new DeleteTaskConsumer((always) -> true));

        // upsert all local configurations, to ensure all that should be running are running
        localConfigurations.stream()
                // transient members may have been cleared so re-inject them
                .peek(cfg -> injector.injectMembers(cfg))
                .forEach(new UpsertTaskConsumer((always) -> true));
    }


    /**
     * Blocks until the Configuration Update Manager ran (at least once)
     */
    public void awaitUntilConfigured() throws InterruptedException {
        IS_CONFIGURED_LATCH.await();
    }



    /* Upsert and delete configurations and tasks behavior */


    /**
     * Upserts objects in the configuration map
     */
    class UpsertConfigurationConsumer implements Consumer<Configuration> {
        @Override
        public void accept(Configuration configuration) {
            configurations.put(configuration.toString(), configuration);
        }
    }

    /**
     * Deletes objects from the configuration map
     */
    class DeleteConfigurationConsumer implements Consumer<Configuration> {
        @Override
        public void accept(Configuration configuration) {
            configurations.remove(configuration.toString());
        }
    }

    /**
     * Upserts a task for each configuration
     * <p/>
     * <p/>By default it does not do so if not currently running in cluster mode, since in that case
     *     task management is handled by {@link ConfigurationMapListener}
     * <p/>
     * <p/>However, it allows overriding this behavior, since there are instances where tasks should be updated
     *     regardless if running in cluster mode
     */
    class UpsertTaskConsumer implements Consumer<Configuration> {
        private final Predicate<Cluster> runPredicate;

        UpsertTaskConsumer() {
            this(cluster -> !cluster.isEnabled());
        }

        UpsertTaskConsumer(Predicate<Cluster> runPredicate) {
            this.runPredicate = runPredicate;
        }

        @Override
        public void accept(Configuration configuration) {
            // if not running in cluster mode, tasks need to be managed directly
            if (runPredicate.test(cluster)) {
                taskManager.upsert(configuration);
            }
        }
    }

    /**
     * Deletes the corresponding task for each configuration
     * <p/>
     * <p/>By default it does not do so if not currently running in cluster mode, since in that case
     *     task management is handled by {@link ConfigurationMapListener}
     * <p/>
     * <p/>However, it allows overriding this behavior, since there are instances where tasks should be cleared
     *     regardless if running in cluster mode
     */
    class DeleteTaskConsumer implements Consumer<Configuration> {
        private final Predicate<Cluster> runPredicate;

        /**
         * Default behavior: only operates when not running in cluster mode
         */
        DeleteTaskConsumer() {
            this(cluster -> !cluster.isEnabled());
        }

        /**
         * Can replace the default behavior with a custom {@link Predicate}
         */
        DeleteTaskConsumer(Predicate<Cluster> runPredicate) {
            this.runPredicate = runPredicate;
        }

        @Override
        public void accept(Configuration configuration) {
            // if not running in cluster mode, tasks need to be managed directly
            if (runPredicate.test(cluster)) {
                taskManager.remove(configuration);
            }
        }
    }



    /* Cluster events */


    /* Configuration add/remove/update events */

    /**
     * Handles events on Hazelcast nodes
     */
    class ConfigurationMapListener implements EntryAddedListener<String, Configuration>,
            EntryRemovedListener<String, Configuration>,
            EntryUpdatedListener<String, Configuration>,
            EntryEvictedListener<String, Configuration> {
        @Override
        public void entryAdded(EntryEvent<String, Configuration> event) {
            logger.info("[CLUSTER] Added task for {}", event.getKey());
            taskManager.upsert(event.getValue());
            IS_CONFIGURED_LATCH.countDown();
        }

        @Override
        public void entryRemoved(EntryEvent<String, Configuration> event) {
            logger.info("[CLUSTER] Removed task for {}", event.getKey());
            // since the entry has been removed, we need the old value rather than the current value
            taskManager.remove(event.getOldValue());
        }

        @Override
        public void entryUpdated(EntryEvent<String, Configuration> event) {
            logger.info("[CLUSTER] Updated task for {}", event.getKey());
            taskManager.upsert(event.getValue());
        }

        @Override
        public void entryEvicted(EntryEvent<String, Configuration> event) {
            logger.info("[CLUSTER] Evicted task for {}", event.getKey());
            taskManager.remove(event.getValue());
        }
    }



    /* Cluster membership events */

    /**
     * Handles events pertaining to the whole cluster
     */
    class ClusterEventListener implements MembershipListener {
        @Override
        public void memberAdded(MembershipEvent event) {
            logger.info("[CLUSTER] Member(s) added: {}; rebalancing tasks...", event.getMembers());
            updateTasksAfterClusterEvent();
        }

        @Override
        public void memberRemoved(MembershipEvent event) {
            logger.info("[CLUSTER] Member(s) removed: {}; rebalancing tasks...", event.getMembers());
            updateTasksAfterClusterEvent();
        }

        @Override
        public void memberAttributeChanged(MemberAttributeEvent event) {
            // nothing to do
        }
    }



    /* Partition migrations */

    /**
     * Manages partition migrations
     */
    class ClusterMigrationListener implements MigrationListener {
        private final Subject<MigrationEvent> migrationEvent;

        public ClusterMigrationListener() {
            // init a publish subject, to allow emitting migration events
            Subject<MigrationEvent> subj = PublishSubject.create();
            migrationEvent = subj.toSerialized();

            // collect migration events every 10 seconds and remove any redundant tasks on every node
            migrationEvent.buffer(10, TimeUnit.SECONDS)
                    // filter our windows when no events have been observed
                    .filter(events -> !events.isEmpty())

                    // log partition migration event
                    .doOnNext(events -> logger.info("[CLUSTER] Migrated {} partition", events.size()))

                    // and update tasks
                    .doOnNext(events -> updateTasksAfterClusterEvent())

                    // process async
                    .subscribeOn(Schedulers.computation())
                    .subscribe();
        }

        @Override
        public void migrationStarted(MigrationEvent event) {
            // nothing to do
        }

        @Override
        public void migrationCompleted(MigrationEvent event) {
            // if data was moved
            if (event.getOldOwner() != event.getNewOwner()) {
                migrationEvent.onNext(event);
            }
        }

        @Override
        public void migrationFailed(MigrationEvent event) {
            logger.warn("[CLUSTER] Unexpected migration failure event {}", event);
        }
    }
}
