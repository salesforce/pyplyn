/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.cluster;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import java.util.Collections;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 4.0
 */
public class ClusterTest {
    Cluster cluster;

    @Mock
    AppConfig appConfig;

    @Mock
    AppConfig.Hazelcast hazelcastConfig;

    ShutdownHook shutdownHook;

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Mock
    private com.hazelcast.core.Cluster hazelcastCluster;

    @Mock
    private Member member;


    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        shutdownHook = spy(new ShutdownHook());

        doReturn(hazelcastConfig).when(appConfig).hazelcast();
        cluster = spy(new Cluster(appConfig, shutdownHook));

        doReturn(hazelcastInstance).when(cluster).initHazelcast();
        doReturn(hazelcastCluster).when(hazelcastInstance).getCluster();
        doReturn(Collections.singleton(member)).when(hazelcastCluster).getMembers();
    }

    @Test
    public void testIsAlwaysMaster() throws Exception {
        // ACT/ASSERT
        assertThat("Expecting isMaster=true, when Hazelcast is not running", cluster.isMaster(), equalTo(true));
        verify(shutdownHook, times(0)).registerOperation(any());
    }

    @Test
    public void testIsNotMaster() throws Exception {
        // ARRANGE
        doReturn(true).when(hazelcastConfig).isEnabled();
        doReturn(false).when(member).localMember();

        cluster.initialize();

        // ACT/ASSERT
        assertThat("Expecting isEnabled=true, when Hazelcast is running", cluster.isEnabled(), equalTo(true));
        assertThat("Expecting isMaster=false, when Hazelcast is running as slave", cluster.isMaster(), equalTo(false));
        verify(shutdownHook).registerOperation(any());
    }

    @Test
    public void testDistributedMap() throws Exception {
        // ACT/ASSERT
        try {
            cluster.distributedMap("map");
            fail("Expecting this to fail");

        } catch (NullPointerException e) {
            verify(cluster, times(1)).guardAgainstInitializationFailures();
        }
    }

    @Test
    public void testStartCluster() throws Exception {
        // ARRANGE
        doReturn(true).when(hazelcastConfig).isEnabled();

        // ACT
        cluster.initialize();

        // ASSERT
        verify(cluster).initHazelcast();
        verify(shutdownHook).registerOperation(any());
    }

    @Test
    public void testStopCluster() throws Exception {
        // ARRANGE
        doReturn(true).when(hazelcastConfig).isEnabled();
        doReturn(false).when(member).localMember();

        // ACT
        cluster.initialize();
        shutdownHook.shutdown();

        // ASSERT
        verify(cluster).initHazelcast();
        verify(hazelcastInstance).shutdown();
    }
}