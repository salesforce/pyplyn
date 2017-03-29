/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.providers.client;

import com.salesforce.argus.ArgusClient;
import com.salesforce.pyplyn.client.ClientFactoryException;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import org.mockito.Mock;
import org.testng.annotations.*;

import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 6.0
 */
public class RemoteClientFactoryTest {
    private AppBootstrapFixtures fixtures;

    @Mock
    AbstractConnector connector;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testGetClientCachingWorks() throws Exception {
        // ARRANGE
        fixtures.freeze();

        // init a dummy connector
        doReturn("http://localhost/").when(connector).endpoint();
        doReturn(AppBootstrapFixtures.MOCK_CONNECTOR_NAME).when(connector).connectorId();
        doReturn(connector).when(fixtures.appConnector()).get(AppBootstrapFixtures.MOCK_CONNECTOR_NAME);

        // init client factory
        RemoteClientFactory<ArgusClient> factory = spy(new RemoteClientFactory<>(fixtures.appConnector(), ArgusClient.class));
        factory.getClient(AppBootstrapFixtures.MOCK_CONNECTOR_NAME);

        // ACT
        // this should be cached and not result in a second call to initialize
        factory.getClient(AppBootstrapFixtures.MOCK_CONNECTOR_NAME);

        // ASSERT
        verify(factory, times(1)).initializeClient(AppBootstrapFixtures.MOCK_CONNECTOR_NAME);
    }

    @Test(expectedExceptions = ClientFactoryException.class)
    public void testGetClientThrowsExceptionOnInstantiationIssue() throws Exception {
        // ARRANGE
        fixtures.freeze();

        // incorrectly init the app connector
        doReturn(connector).when(fixtures.appConnector()).get(AppBootstrapFixtures.MOCK_CONNECTOR_NAME);

        // init client factory
        RemoteClientFactory<ArgusClient> factory = spy(new RemoteClientFactory<>(fixtures.appConnector(), ArgusClient.class));

        // ACT/ASSERT
        factory.getClient(AppBootstrapFixtures.MOCK_CONNECTOR_NAME);
    }
}