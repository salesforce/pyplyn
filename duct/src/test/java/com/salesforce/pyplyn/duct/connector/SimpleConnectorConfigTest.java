/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.connector;

import com.google.inject.Injector;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.pyplyn.duct.app.BootstrapException;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.util.SerializationHelper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.salesforce.pyplyn.duct.connector.AppConnector.DUPLICATE_CONNECTOR_ERROR;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class SimpleConnectorConfigTest {
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
        Injector injector = fixtures.freeze().injector();
        SerializationHelper serializer = injector.getProvider(SerializationHelper.class).get();

        // ACT
        SimpleConnectorConfig[] connectors = serializer.deserializeJsonFile(ONE_CONNECTOR, SimpleConnectorConfig[].class);

        // ASSERT
        assertConnectorsWereDeserialized(connectors);
        assertThat(connectors[0].connectorId(), equalTo("id"));
        assertThat(connectors[0].endpoint(), equalTo("endpoint"));
        assertThat(connectors[0].username(), equalTo("username"));
        assertThat(connectors[0].proxyHost(), equalTo("proxyHost"));
        assertThat(connectors[0].proxyPort(), equalTo(1));
    }

    @Test
    public void testPasswordIsNotCached() throws Exception {
        // ARRANGE
        Injector injector = fixtures.freeze().injector();
        SerializationHelper serializer = injector.getProvider(SerializationHelper.class).get();

        // ACT
        SimpleConnectorConfig[] connectors = serializer.deserializeJsonFile(ONE_CONNECTOR, SimpleConnectorConfig[].class);

        // ASSERT
        assertConnectorsWereDeserialized(connectors);

        // ARRANGE
        SimpleConnectorConfig connector = spy(connectors[0]);
        connector.setConnectorFilePath(ONE_CONNECTOR);

        // ASSERT
        assertThat(connector.password(), equalTo("password".getBytes()));
        verify(connector, times(1)).connectorFilePath();
    }

    @Test
    public void testDuplicateConnectors() throws Exception {
        // ARRANGE
        Injector injector = fixtures.freeze().injector();
        SerializationHelper serializer = injector.getProvider(SerializationHelper.class).get();

        // ACT
        // deserialize connectors into expected type, then add to a set
        SimpleConnectorConfig[] connectors = serializer.deserializeJsonFile(DUPLICATE_CONNECTORS, SimpleConnectorConfig[].class);
        Set<List<AbstractConnector>> connectorSet = createConnectorSet(connectors);

        try {
            new AppConnector(connectorSet);
            fail("Expected this test to fail as we don't allow duplicate connector ids");

        } catch (BootstrapException e) {
            // ASSERT
            assertConnectorsWereDeserialized(connectors);
            assertThat(e.getMessage(), containsString(String.format(DUPLICATE_CONNECTOR_ERROR,
                    SimpleConnectorConfig.class.getSimpleName(), connectors[0].connectorId())));
        }
    }

    @Test
    public void testAppConnectorCanBeInitialized() throws Exception {
        // ARRANGE
        Injector injector = fixtures.freeze().injector();
        SerializationHelper serializer = injector.getProvider(SerializationHelper.class).get();

        // ACT
        // deserialize connectors into expected type, then add to a set
        SimpleConnectorConfig[] connectors = serializer.deserializeJsonFile(ONE_CONNECTOR, SimpleConnectorConfig[].class);
        Set<List<AbstractConnector>> connectorSet = createConnectorSet(connectors);

        // initialize the AppConnector object
        AppConnector appConnector = new AppConnector(connectorSet);

        assertConnectorsWereDeserialized(connectors);
        assertThat(appConnector.get("invalid-unknown-id"), nullValue());
        assertThat(appConnector.get(connectors[0].connectorId()), notNullValue());
    }


    /**
     * Asserts that we can expect at least one valid connector in the array
     */
    private static void assertConnectorsWereDeserialized(SimpleConnectorConfig[] connectors) {
        assertThat(connectors, notNullValue());
        assertThat(connectors.length, greaterThan(0));
    }

    /**
     * Creates a connector set that can be used to initialize an {@link AppConnector} object
     */
    public static Set<List<AbstractConnector>> createConnectorSet(AbstractConnector[] connectors) {
        Set<List<AbstractConnector>> connectorSet = new HashSet<>();
        connectorSet.add(Arrays.asList(connectors));
        return connectorSet;
    }
}