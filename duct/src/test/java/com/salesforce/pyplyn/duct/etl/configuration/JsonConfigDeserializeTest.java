/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.salesforce.pyplyn.duct.connector.ConnectorTest.ONE_CONNECTOR;
import static com.salesforce.pyplyn.util.SerializationHelper.loadResourceInsecure;
import static org.hamcrest.MatcherAssert.assertThat;
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
    private ObjectMapper mapper;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();

        Injector injector = fixtures.initializeFixtures().injector();
        mapper = injector.getProvider(ObjectMapper.class).get();
    }

    @Test
    public void testDeserializeConfiguration() throws Exception {
        // ACT
        Configuration[] configuration = mapper.readValue(loadResourceInsecure(ONE_CONFIGURATION), Configuration[].class);

        // ASSERT
        assertThat(configuration, notNullValue());
    }

    @Test
    public void testDeserializeConnector() throws Exception {
        // ACT
        Connector[] appConnectors = mapper.readValue(loadResourceInsecure(ONE_CONNECTOR), Connector[].class);

        // ASSERT
        assertThat(appConnectors, notNullValue());
    }
}
