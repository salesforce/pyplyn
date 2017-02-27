/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.configuration.UpdatableConfigurationSetProvider;
import com.salesforce.pyplyn.status.MeterType;
import com.salesforce.pyplyn.status.SystemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Loads all configurations, disregarding any logic to split on multiple hosts
 *   annotated as Singleton as there should only be one instance of this class in operation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class SinglePartitionConfigurationProvider implements UpdatableConfigurationSetProvider<ConfigurationWrapper> {
    protected static final Logger logger = LoggerFactory.getLogger(SinglePartitionConfigurationProvider.class);
    private static final String PROCESS_NAME = "ConfigurationProvider";
    private final Lock updateLock = new ReentrantLock();
    protected final ConfigurationProvider provider;
    private final SystemStatus systemStatus;
    protected Map<Configuration, ConfigurationWrapper> configurations = new HashMap<>();


    /**
     * Default constructor
     */
    @Inject
    public SinglePartitionConfigurationProvider(ConfigurationProvider provider, SystemStatus systemStatus) {
        this.provider = provider;
        this.systemStatus = systemStatus;
    }

    /**
     * Updates the known configuration list
     * <p/>
     * <p/>This implementation acquires a lock to guard against multiple threads attempting to update at the same time.
     */
    @Override
    public void updateConfigurations() {
        try {
            // attempt to acquire a lock and update the list of configurations
            if (updateLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                // make a copy of all current elements
                Map<Configuration, ConfigurationWrapper> oldEntries = ImmutableMap.copyOf(configurations);

                try {
                    // merge new configurations into existing map
                    mergeConfigurationSets(provider.get(), configurations);

                } catch (RuntimeException e) {
                    logger.error("Unexpected error updating configurations!", e);
                    markFailure();

                    // if we could not update configurations, we will restore the old set
                    restoreBackedUpEntries(oldEntries);

                } finally {
                    updateLock.unlock();
                }
            }

        } catch (InterruptedException e) {
            // nothing to do, either the program is stopping or another thread was updating the configurations
        }
    }


    /**
     * Returns all known {@link ConfigurationWrapper}s
     */
    @Override
    public Set<ConfigurationWrapper> get() {
        return retrieveLocalNodeConfigurations().values().stream().collect(Collectors.toSet());
    }

    /**
     * Every time it runs, this class will update all known configurations
     */
    @Override
    public void run() {
        updateConfigurations();
    }


    /**
     * Clears map of any left values and restores old entries from a backup
     */
    private void restoreBackedUpEntries(Map<Configuration, ConfigurationWrapper> oldEntries) {
        // restore old map
        configurations.clear();
        oldEntries.entrySet().forEach(entry -> configurations.put(entry.getKey(), entry.getValue()));
    }

    /**
     * Merges the updated configurations set into an existing configurations map
     * <p/>- adds configurations that were not previously defined
     * <p/>- replace all other configurations, as they might have different specifications
     * <p/>- removes configurations that are not defined anymore
     */
    protected static void mergeConfigurationSets(Set<Configuration> updatedConfigurations, Map<Configuration, ConfigurationWrapper> configurations) {
        for (Configuration newConfig : updatedConfigurations) {
            // returns an existing wrapper (for an update)
            ConfigurationWrapper wrapper = configurations.get(newConfig);

            // initialize a new one if it's a new object
            if (isNull(wrapper)) {
                wrapper = new ConfigurationWrapper(newConfig, null);

                // if a mapping already existed, we need to update the configuration
            } else {
                wrapper.update(newConfig);
            }

            // update existing OR define new configuration
            configurations.put(newConfig, wrapper);
        }

        // delete old configurations
        Set<Configuration> deletedConfigurations = ImmutableSet.copyOf(Sets.difference(configurations.keySet(), updatedConfigurations));
        for (Configuration deletedConfiguration : deletedConfigurations) {
            configurations.remove(deletedConfiguration);
        }

        logger.info("Updated configuration set; {} new/updated configs, {} deleted configs", updatedConfigurations.size(), deletedConfigurations.size());
    }

    /**
     * Provides a system status update, notifying of an Configuration update failure
     */
    void markFailure() {
        systemStatus.meter(PROCESS_NAME, MeterType.ConfigurationUpdateFailure).mark();
    }

    /**
     * Returns all {@link Configuration}s corresponding to the current node
     * <p/>
     * For this class, this means all objects; however, subclasses can override this to return a subset
     */
    protected Map<Configuration, ConfigurationWrapper> retrieveLocalNodeConfigurations() {
        return configurations;
    }
}
