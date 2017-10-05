package com.salesforce.pyplyn.duct.cluster;

import java.io.FileNotFoundException;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;

/**
 * Configures the Hazelcast cluster
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class ClusterModule extends AbstractModule {
    @Override
    protected void configure() {
        // nothing to do
    }

    @Provides
    @Singleton
    Cluster getCluster(AppConfig appConfig, ShutdownHook shutdownHook) throws FileNotFoundException {
        Cluster cluster = new Cluster(appConfig, shutdownHook);
        cluster.initialize();
        return cluster;
    }
}
