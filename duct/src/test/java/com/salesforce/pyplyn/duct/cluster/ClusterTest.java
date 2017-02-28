/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    AppConfig.Hazelcast hazelcast;

    @Mock
    ShutdownHook shutdownHook;


    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        doReturn(hazelcast).when(appConfig).hazelcast();
        cluster = new Cluster(appConfig, shutdownHook);
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
        doReturn(true).when(hazelcast).isEnabled();
        doReturn("config").when(hazelcast).config();

        MockedCluster cluster = new MockedCluster(appConfig, shutdownHook);
        cluster.isMasterNode(false);

        // ACT/ASSERT
        assertThat("Expecting isMaster=false, when Hazelcast is running", cluster.isMaster(), equalTo(false));
        verify(shutdownHook).registerOperation(any());
    }

    @Test
    public void testDistributedMap() throws Exception {
        // ACT/ASSERT
        assertThat("Expecting null return, when Hazelcast is not running", cluster.distributedMap("map"), nullValue());
        verify(shutdownHook, times(0)).registerOperation(any());
    }

    @Test
    public void testStartCluster() throws Exception {
        // ARRANGE
        doReturn(true).when(hazelcast).isEnabled();
        doReturn("config").when(hazelcast).config();

        // ACT
        cluster = new MockedCluster(appConfig, shutdownHook);

        // ASSERT
        assertThat(((MockedCluster) cluster).hazelcast, notNullValue());
        verify(shutdownHook).registerOperation(any());
    }


    @Test
    public void testStopCluster() throws Exception {
        // ARRANGE
        ShutdownHook shutdownHook = new ShutdownHook();
        doReturn(true).when(hazelcast).isEnabled();
        cluster = new MockedCluster(appConfig, shutdownHook);

        // ACT
        shutdownHook.shutdown();

        // ASSERT
        assertThat(((MockedCluster) cluster).hazelcast, notNullValue());
        verify(((MockedCluster) cluster).hazelcast).shutdown();
    }


    /**
     * Mocked cluster class; we need to override init to avoid actually initializing the Hazelcast cluster
     */
    private static class MockedCluster extends Cluster {
        private HazelcastInstance hazelcast;
        private com.hazelcast.core.Cluster hazelCluster;
        private Member member;

        public MockedCluster(AppConfig appConfig, ShutdownHook shutdownHook) throws FileNotFoundException {
            super(appConfig, shutdownHook);
        }

        /**
         * NOTE: this is called from the parent constructor; be careful to avoid adding circular dependencies !
         */
        @Override
        HazelcastInstance init(String configFile) throws FileNotFoundException {
            hazelcast = mock(HazelcastInstance.class);
            return hazelcast;
        }

        void isMasterNode(boolean masterNode) {
            hazelCluster = mock(com.hazelcast.core.Cluster.class);
            doReturn(hazelCluster).when(hazelcast).getCluster();

            member = mock(Member.class);
            doReturn(Collections.singleton(member)).when(hazelCluster).getMembers();
            doReturn(masterNode).when(member).localMember();
        }
    }
}