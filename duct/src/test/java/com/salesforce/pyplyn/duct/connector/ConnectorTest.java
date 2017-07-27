/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.salesforce.pyplyn.cache.CacheFactory;
import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.ConnectorInterface;
import com.salesforce.pyplyn.configuration.ImmutableConnector;
import com.salesforce.pyplyn.duct.app.BootstrapException;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.salesforce.pyplyn.duct.connector.AppConnectors.DUPLICATE_CONNECTOR_ERROR;
import static com.salesforce.pyplyn.util.SerializationHelper.loadResourceInsecure;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class ConnectorTest {
    public static final String ONE_CONNECTOR = "/connectors/one-connector.json";
    public static final String DUPLICATE_CONNECTORS = "/connectors/duplicate-connectors.json";

    AppBootstrapFixtures fixtures;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testConnectorFields() throws Exception {
        // ARRANGE
        Injector injector = fixtures.initializeFixtures().injector();
        ObjectMapper mapper = injector.getProvider(ObjectMapper.class).get();

        // ACT
        Connector[] connectors = mapper.readValue(loadResourceInsecure(ONE_CONNECTOR), Connector[].class);

        // ASSERT
        assertConnectorsWereDeserialized(connectors);
        assertThat(connectors[0].id(), equalTo("id"));
        assertThat(connectors[0].endpoint(), equalTo("endpoint"));
        assertThat(connectors[0].username(), equalTo("username"));
        assertThat(connectors[0].proxyHost(), equalTo("proxyHost"));
        assertThat(connectors[0].proxyPort(), equalTo(1));
    }

    @Test
    public void testDuplicateConnectors() throws Exception {
        // ARRANGE
        Injector injector = fixtures.initializeFixtures().injector();
        ObjectMapper mapper = injector.getProvider(ObjectMapper.class).get();

        // ACT
        // deserialize connectors into expected type, then add to a set
        Connector[] connectors = mapper.readValue(loadResourceInsecure(DUPLICATE_CONNECTORS), Connector[].class);
        Set<List<ConnectorInterface>> connectorSet = createConnectorSet(connectors);

        try {
            new AppConnectors(connectorSet, mock(CacheFactory.class));
            fail("Expected this test to fail as we don't allow duplicate connector ids");

        } catch (BootstrapException e) {
            // ASSERT
            assertConnectorsWereDeserialized(connectors);
            assertThat(e.getMessage(), containsString(String.format(DUPLICATE_CONNECTOR_ERROR,
                    ImmutableConnector.class.getSimpleName(), connectors[0].id())));
        }
    }

    @Test
    public void testAppConnectorCanBeInitialized() throws Exception {
        // ARRANGE
        Injector injector = fixtures.initializeFixtures().injector();
        ObjectMapper mapper = injector.getProvider(ObjectMapper.class).get();

        // ACT
        // deserialize connectors into expected type, then add to a set
        ConnectorInterface[] connectors = mapper.readValue(loadResourceInsecure(ONE_CONNECTOR), Connector[].class);
        Set<List<ConnectorInterface>> connectorSet = createConnectorSet(connectors);

        // initialize the AppConnectors object
        AppConnectors appConnectors = new AppConnectors(connectorSet, mock(CacheFactory.class));

        assertConnectorsWereDeserialized(connectors);
        assertThat(appConnectors.findConnector("invalid-unknown-id"), nullValue());
        assertThat(appConnectors.findConnector(connectors[0].id()), notNullValue());
    }


    /**
     * Asserts that we can expect at least one valid connector in the array
     */
    private static void assertConnectorsWereDeserialized(ConnectorInterface[] connectors) {
        assertThat(connectors, notNullValue());
        assertThat(connectors.length, greaterThan(0));
    }

    /**
     * Creates a connector set that can be used to initialize an {@link AppConnectors} object
     */
    public static Set<List<ConnectorInterface>> createConnectorSet(ConnectorInterface[] connectors) {
        Set<List<ConnectorInterface>> connectorSet = new HashSet<>();
        connectorSet.add(Arrays.asList(connectors));
        return connectorSet;
    }
}