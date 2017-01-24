/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hazelcast.core.IMap;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.cluster.Cluster;
import com.salesforce.pyplyn.status.SystemStatus;

import java.util.Map;

/**
 * Loads all configurations, disregarding any logic to split on multiple hosts
 *   annotated as Singleton as there should only be one instance of this class in operation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class DistributedConfigurationProvider extends SinglePartitionConfigurationProvider {
    private static final String CONFIGURATIONS_MAP = "configurations";
    private final Cluster cluster;

    @Inject
    public DistributedConfigurationProvider(ConfigurationProvider provider, Cluster cluster, SystemStatus systemStatus) {
        super(provider, systemStatus);
        this.cluster = cluster;

        // reinitialize the map to be backed by Hazelcast
        configurations = cluster.distributedMap(CONFIGURATIONS_MAP);
    }

    /**
     * Only update configurations if executed on the master node,
     *   otherwise do nothing
     */
    @Override
    public void run() {
        if (cluster.isMaster()) {
            updateConfigurations();
        }
    }

    /**
     * Returns a partitioned subset of {@link Configuration}s, corresponding to the local node
     */
    protected Map<Configuration, ConfigurationWrapper> retrieveLocalNodeConfigurations() {
        IMap<Configuration, ConfigurationWrapper> hazelcastConfigurations = (IMap<Configuration, ConfigurationWrapper>) this.configurations;
        return hazelcastConfigurations.getAll(hazelcastConfigurations.localKeySet());
    }
}
