/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Injector;
import com.salesforce.pyplyn.configuration.EndpointConnector;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class SimpleConnectorProviderTest {
    AppBootstrapFixtures fixtures;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testProviderLoadsOneConnector() throws Exception {
        // ARRANGE
        fixtures.appConfigMocks()
                .connectorsPath(ConnectorTest.ONE_CONNECTOR);
        Injector injector = fixtures.initializeFixtures().injector();
        SimpleConnectorProvider provider = injector.getProvider(SimpleConnectorProvider.class).get();

        // ACT
        List<EndpointConnector> connectors = provider.get();

        // ASSERT
        assertThat(connectors, notNullValue());
        assertThat(connectors, hasSize(1));
    }

    @Test
    public void testProviderReturnsEmptyListWithoutPath() throws Exception {
        // ARRANGE
        Injector injector = fixtures.initializeFixtures().injector();
        SimpleConnectorProvider provider = injector.getProvider(SimpleConnectorProvider.class).get();

        // ACT
        List<EndpointConnector> connectors = provider.get();

        // ASSERT
        assertThat(connectors, notNullValue());
        assertThat(connectors, empty());
    }
}