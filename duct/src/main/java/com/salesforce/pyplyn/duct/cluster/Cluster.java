/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.cluster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;

import java.io.FileNotFoundException;
import java.util.Set;

import static com.salesforce.pyplyn.util.SerializationHelper.loadResourceInsecure;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Initializes the cluster logic
 *   wraps all {@link Hazelcast} functionality, to simplify the API
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
@Singleton
public class Cluster {
    private final HazelcastInstance hazelcast;

    /**
     * Cluster constructor
     *
     * @throws FileNotFoundException if an invalid *hazelcast.xml* location was specified in the {@link AppConfig}
     */
    @Inject
    public Cluster(AppConfig appConfig, ShutdownHook shutdownHook) throws FileNotFoundException {
        // start Hazelcast, if enabled
        if (nonNull(appConfig.hazelcast()) && appConfig.hazelcast().isEnabled()) {
            hazelcast = init(appConfig.hazelcast().config());

            // if a Hazelcast cluster was initialized, register it for self shutdown
            shutdownHook.registerOperation(hazelcast::shutdown);

        } else {
            hazelcast = null;
        }
    }

    /**
     * Initializes the Hazelcast instance based on the passed path to *hazelcast.xml*
     * <p/>
     * <p/>NOTE: This is not a great pattern, however it is required to allow testing of this class,
     *  without actually starting a Hazelcast cluster; be EXTRA CAREFUL when you override this method,
     *  as it called in this class' constructor!
     */
    HazelcastInstance init(String configFile) throws FileNotFoundException {
        Config config = new XmlConfigBuilder(loadResourceInsecure(configFile)).build();
        return Hazelcast.newHazelcastInstance(config);
    }

    /**
     * Delegates the Hazelcast map operation
     */
    public <K, V> IMap<K, V> distributedMap(String name) {
        // nothing to return if Hazelcast is not running
        if (isNull(hazelcast)) {
            return null;
        }

        return hazelcast.getMap(name);
    }

    /**
     * Returns true if the code is executed on the master (oldest member of the cluster)
     *   if Hazelcast is not running, returns true, as the only existing node is a master node
     */
    public boolean isMaster() {
        if (isNull(hazelcast)) {
            return true;
        }

        Set<Member> members = hazelcast.getCluster().getMembers();
        Member member = members.iterator().next();
        return member.localMember();
    }
}
