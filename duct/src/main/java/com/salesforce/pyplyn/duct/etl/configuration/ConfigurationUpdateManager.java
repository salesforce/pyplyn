package com.salesforce.pyplyn.duct.etl.configuration;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;
import com.hazelcast.map.listener.*;
import com.hazelcast.util.Preconditions;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.cluster.Cluster;
import com.salesforce.pyplyn.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
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

    private Map<Configuration, Configuration> configurations;
    private final CountDownLatch IS_CONFIGURED_LATCH = new CountDownLatch(1);

    @Inject
    public ConfigurationUpdateManager(ConfigurationLoader loader, TaskManager<Configuration> taskManager, Cluster cluster, ShutdownHook shutdownHook) {
        this.loader = loader;
        this.taskManager = taskManager;
        this.cluster = cluster;
        this.shutdownHook = shutdownHook;
    }

    /**
     * Periodically updates the set of known configurations
     */
    @Override
    public void run() {
        if (cluster.isEnabled() && !cluster.isMaster()) {
            logger.info("Skipping configuration update on this node (not master)");
            return;
        }

        // load configurations from disk and filter out disabled ones
        try {
            Set<Configuration> latestConfigurationSet = loader.load().stream().filter(Configuration::isEnabled).collect(Collectors.toSet());
            // delete configurations
            ImmutableSet<Configuration> deleted = ImmutableSet.copyOf(Sets.difference(configurations(), latestConfigurationSet));
            deleted.forEach(configuration -> {
                taskManager.remove(configuration);
                configurations.remove(configuration);
            });

            // insert or update new configurations
            latestConfigurationSet.forEach(configuration -> {
                taskManager.upsert(configuration);
                configurations.put(configuration, configuration);
            });

            logger.info("Updated configuration set; {} configs upserted, {} configs deleted", latestConfigurationSet.size(), deleted.size());

        } catch (RuntimeException e) {
            logger.warn("Could not update configurations", e);

            // if this is the first run, stop right away
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
        if (cluster.isEnabled()) {
            IMap<Configuration, Configuration> map = (IMap<Configuration, Configuration>) configurations;
            return CollectionUtils.immutableOrEmptySet(map.localKeySet());
        } else {
            return CollectionUtils.immutableOrEmptySet(configurations.keySet());
        }
    }

    /**
     *  @return Set of all known {@link Configuration}s
     */
    Set<Configuration> configurations() {
        return configurations.keySet();
    }


    /**
     * Initialize configuration set, based on cluster availability
     */
    public void initialize() {
        Preconditions.checkState(isNull(configurations), "Cannot initialize the Configuration Update Manager more than once!");

        if (cluster.isEnabled()) {
            IMap<Configuration, Configuration> hzMap = cluster.distributedMap(CONFIGURATION_MAP_KEY);
            hzMap.addLocalEntryListener(new TaskEntryListener());
            configurations = hzMap;

        } else {
            configurations = new HashMap<>();
        }
    }

    /**
     * Blocks until the Configuration Update Manager ran (at least once)
     */
    public void awaitUntilConfigured() throws InterruptedException {
        IS_CONFIGURED_LATCH.await();
    }


    /**
     * Handles map events on Hazelcast slaves
     */
    class TaskEntryListener implements EntryAddedListener<Configuration, Configuration>,
            EntryRemovedListener<Configuration, Configuration>,
            EntryUpdatedListener<Configuration, Configuration>,
            EntryEvictedListener<Configuration, Configuration>,
            MapEvictedListener,
            MapClearedListener {

        @Override
        public void entryAdded( EntryEvent<Configuration, Configuration> event ) {
            logger.info("Added task for {}", event.getKey());
            taskManager.upsert(event.getKey());
            IS_CONFIGURED_LATCH.countDown();
        }

        @Override
        public void entryRemoved( EntryEvent<Configuration, Configuration> event ) {
            logger.info("Removed task for {}", event.getKey());
            taskManager.remove(event.getKey());
        }

        @Override
        public void entryUpdated( EntryEvent<Configuration, Configuration> event ) {
            logger.info("Updated task for {}", event.getKey());
            taskManager.upsert(event.getKey());
        }

        @Override
        public void entryEvicted( EntryEvent<Configuration, Configuration> event ) {
            logger.info("Evicted task for {}", event.getKey());
            taskManager.remove(event.getKey());
        }

        @Override
        public void mapEvicted( MapEvent event ) {
            logger.warn("Configuration map was evicted!");
        }

        @Override
        public void mapCleared( MapEvent event ) {
            logger.warn("Configuration map was cleared!");
        }
    }
}
