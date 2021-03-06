/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import static com.salesforce.pyplyn.duct.appconfig.AppConfigProviderTest.fixSerializationHelper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.BootstrapException;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class ConfigurationIntakeTest {
    public static final String INVALID_CONFIGURATIONS = "/configurations/invalid/";

    private AppBootstrapFixtures fixtures;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testInvalidConfigurations() throws Exception {
        // ARRANGE
        String invalidConfigurationsDir = this.getClass().getResource(INVALID_CONFIGURATIONS).getFile();
        ObjectMapper mapper = fixSerializationHelper(fixtures);
        ConfigurationIntake configurationIntake = spy(new ConfigurationIntake(mapper));


        // ACT
        Set<Configuration> parsedConfigurations = configurationIntake.parseAll(invalidConfigurationsDir);


        // ASSERT
        try {
            verify(configurationIntake).getAllConfigurationsFromDisk(invalidConfigurationsDir);
            assertThat(parsedConfigurations, empty());

            configurationIntake.throwRuntimeExceptionOnErrors();
            fail("Expecting an exception to be thrown");

        } catch (BootstrapException e) {
            // ASSERT
            assertThat(e.getMessage(), containsString(ConfigurationIntake.CONFIGURATIONS_READ_ERROR));
        }
    }

    @Test
    public void testNullConfigurationsDir() throws Exception {
        // ARRANGE
        ObjectMapper mapper = fixSerializationHelper(fixtures);
        ConfigurationIntake configurationIntake = new ConfigurationIntake(mapper);

        // ACT
        List<String> configurations = configurationIntake.getAllConfigurationsFromDisk(null);

        // ASSERT
        assertThat(configurations, empty());
    }

    @Test(expectedExceptions = IOException.class)
    public void testNonExistentConfigurationsDir() throws Exception {
        // ARRANGE
        ObjectMapper mapper = fixSerializationHelper(fixtures);
        ConfigurationIntake configurationIntake = new ConfigurationIntake(mapper);

        // ACT/ASSERT
        configurationIntake.getAllConfigurationsFromDisk("/invalid/dir/should/throw/IOException");
    }

    @Test
    public void testInvalidConfigurationsViaProvider() throws Exception {
        // ARRANGE
        String invalidConfigurationsDir = this.getClass().getResource(INVALID_CONFIGURATIONS).getFile();
        fixtures.appConfigMocks().configurationsPath(invalidConfigurationsDir);

        ObjectMapper mapper = fixSerializationHelper(fixtures);
        ConfigurationIntake configurationIntake = new ConfigurationIntake(mapper);

        // ACT
        ConfigurationLoader configurationLoader = new ConfigurationLoader(fixtures.appConfigMocks().get(), configurationIntake);

        try {
            configurationLoader.load();

            // ASSERT
            fail("Expecting an exception to be thrown");

        } catch (BootstrapException e) {
            // ASSERT
            assertThat(e.getMessage(), containsString(ConfigurationIntake.CONFIGURATIONS_READ_ERROR));
        }
    }
}