/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.google.inject.Injector;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.connector.SimpleConnectorConfig;
import com.salesforce.pyplyn.util.SerializationHelper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.salesforce.pyplyn.duct.connector.SimpleConnectorConfigTest.ONE_CONNECTOR;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Integration test for testing that configurations and connectors can be deserialized
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class JsonConfigDeserializeTest {
    public static final String ONE_CONFIGURATION = "/configuration.example.json";
    private AppBootstrapFixtures fixtures;
    private SerializationHelper serializer;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testDeserializeConfiguration() throws Exception {
        // ARRANGE
        Injector injector = fixtures.freeze().injector();
        serializer = injector.getProvider(SerializationHelper.class).get();

        // ACT
        Configuration[] configuration = serializer.deserializeJsonFile(ONE_CONFIGURATION, Configuration[].class);

        // ASSERT
        assertThat(configuration, notNullValue());
    }

    @Test
    public void testDeserializeConnector() throws Exception {
        // ARRANGE
        Injector injector = fixtures.freeze().injector();
        serializer = injector.getProvider(SerializationHelper.class).get();

        // ACT
        SimpleConnectorConfig[] appConnectors = serializer.deserializeJsonFile(ONE_CONNECTOR, SimpleConnectorConfig[].class);

        // ASSERT
        assertThat(appConnectors, notNullValue());
    }
}
