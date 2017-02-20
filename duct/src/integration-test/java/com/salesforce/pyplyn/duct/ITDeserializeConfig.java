/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct;

import com.google.inject.Injector;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.AppBootstrap;
import com.salesforce.pyplyn.duct.connector.SimpleConnectorConfig;
import com.salesforce.pyplyn.util.SerializationHelper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
/**
 * Integration test for testing that configs can be deserialized
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ITDeserializeConfig {
    private String appConfigFile = "/it-app-config.json";
    private String connectors = "/connectors.example.json";
    private String configurations = "/configuration.example.json";
    Injector injector;
    SerializationHelper serializer;

    @BeforeMethod
    public void setUp() throws Exception {
        AppBootstrap appBootstrap = new AppBootstrap(appConfigFile).bootstrap();

        // ARRANGE
        injector = appBootstrap.injector();
        serializer = injector.getProvider(SerializationHelper.class).get();
    }

    @Test
    public void deserializeConfiguration() throws Exception {
        // ACT
        Configuration[] configuration = serializer.deserializeJsonFile(configurations, Configuration[].class);

        // ASSERT
        assertThat(configuration, notNullValue());
    }

    @Test
    public void deserializeConnectors() throws Exception {
        // ACT
        SimpleConnectorConfig[] appConnectors = serializer.deserializeJsonFile(connectors, SimpleConnectorConfig[].class);

        // ASSERT
        assertThat(appConnectors, notNullValue());
    }
}
