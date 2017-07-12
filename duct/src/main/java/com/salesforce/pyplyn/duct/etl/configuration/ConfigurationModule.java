/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.cluster.Cluster;

import java.util.Set;

/**
 * Binds the {@link ConfigurationLoader}, returning the set of {@link Configuration}s that should be processed
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConfigurationModule extends AbstractModule {
    @Override
    protected void configure() {
        // allows Guice to inject a Set<Configuration>, when required
        //   this includes all Configurations read from disk on the current node
        bind(new TypeLiteral<TaskManager<Configuration>>() {}).asEagerSingleton();
        bind(new TypeLiteral<Set<Configuration>>() {}).toProvider(ConfigurationUpdateManager.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    ConfigurationUpdateManager configurationManager(ConfigurationLoader loader, TaskManager<Configuration> taskRegistry, Cluster cluster, ShutdownHook shutdownHook) {
        ConfigurationUpdateManager manager = new ConfigurationUpdateManager(loader, taskRegistry, cluster, shutdownHook);
        manager.initialize();
        return manager;
    }
}
