/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.cluster;

import static com.salesforce.pyplyn.util.SerializationHelper.loadResourceInsecure;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.FileNotFoundException;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.*;
import com.hazelcast.util.Preconditions;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;

/**
 * Initializes the cluster logic
 *   wraps {@link Hazelcast} functionality, to simplify the API
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
@Singleton
public class Cluster {
    private final AppConfig.Hazelcast hazelcastConfig;
    private final ShutdownHook shutdownHook;
    private HazelcastInstance hazelcast;
    private boolean clusterEnabled = false;

    /**
     * Default constructor
     */
    @Inject
    public Cluster(AppConfig appConfig, ShutdownHook shutdownHook) {
        this.hazelcastConfig = appConfig.hazelcast();
        this.shutdownHook = shutdownHook;
    }

    /**
     * Initializes the Hazelcast instance based on the passed path to *hazelcast.xml*
     * @throws FileNotFoundException if an invalid *hazelcast.xml* location was specified in the {@link AppConfig}
     */
    void initialize() throws FileNotFoundException {
        Preconditions.checkState(isNull(hazelcast), "Cannot initialize more than once!");

        // start Hazelcast, if enabled
        if (nonNull(hazelcastConfig) && hazelcastConfig.isEnabled()) {
            hazelcast = initHazelcast();

            // if a Hazelcast cluster was initialized, register it for self shutdown
            shutdownHook.registerOperation(hazelcast::shutdown);
            clusterEnabled = true;
        }
    }

    /**
     * Registers a listener, allowing fine-grained management of cluster events
     */
    public void registerListener(MembershipListener membershipListener) {
        guardAgainstInitializationFailures();
        hazelcast.getCluster().addMembershipListener(membershipListener);
    }

    /**
     * Registers a listener, allowing fine-grained management of cluster events
     */
    public void registerListener(MigrationListener clusterMigrationListener) {
        guardAgainstInitializationFailures();
        hazelcast.getPartitionService().addMigrationListener(clusterMigrationListener);
    }

    /**
     * Delegates the Hazelcast map operation
     */
    public <K, V> IMap<K, V> distributedMap(String name) {
        guardAgainstInitializationFailures();
        return hazelcast.getMap(name);
    }

    /**
     * Returns true if the code is executed on the master (oldest member of the cluster)
     *   if Hazelcast is not running, returns true, as the only existing node is a master node
     */
    public boolean isMaster() {
        if (!clusterEnabled) {
            return true;
        }
        guardAgainstInitializationFailures();

        Set<Member> members = hazelcast.getCluster().getMembers();
        Member member = members.iterator().next();
        return member.localMember();
    }

    /**
     * @return true if the Hazelcast cluster is enabled
     */
    public boolean isEnabled() {
        return clusterEnabled;
    }

    /**
     * Prevent method calls, if the Hazelcast cluster was not initialized
     */
    void guardAgainstInitializationFailures() {
        Preconditions.checkNotNull(hazelcast, "Hazelcast cluster is not initialized, cannot call this method!");
    }

    /**
     * Initializes a Hazelcast cluster
     *   used for testing
     */
    HazelcastInstance initHazelcast() throws FileNotFoundException {
        Config clusterConfig = new XmlConfigBuilder(loadResourceInsecure(hazelcastConfig.config())).build();
        return Hazelcast.newHazelcastInstance(clusterConfig);
    }
}
