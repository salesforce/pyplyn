/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.app.BootstrapException;
import com.salesforce.pyplyn.util.SerializationHelper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.salesforce.pyplyn.duct.appconfig.AppConfigProviderTest.fixSerializationHelper;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.fail;

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
        SerializationHelper serializationHelper = fixSerializationHelper(fixtures);
        ConfigurationIntake configurationIntake = new ConfigurationIntake(serializationHelper);

        // ACT
        List<String> configurations = configurationIntake.listOfConfigurations(invalidConfigurationsDir);
        Set<Configuration> parsedConfigurations = configurationIntake.parseAll(configurations);

        // ASSERT
        try {
            assertThat(configurations, hasSize(1));
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
        SerializationHelper serializationHelper = fixSerializationHelper(fixtures);
        ConfigurationIntake configurationIntake = new ConfigurationIntake(serializationHelper);

        // ACT
        List<String> configurations = configurationIntake.listOfConfigurations(null);

        // ASSERT
        assertThat(configurations, empty());
    }

    @Test(expectedExceptions = IOException.class)
    public void testNonExistentConfigurationsDir() throws Exception {
        // ARRANGE
        SerializationHelper serializationHelper = fixSerializationHelper(fixtures);
        ConfigurationIntake configurationIntake = new ConfigurationIntake(serializationHelper);

        // ACT/ASSERT
        configurationIntake.listOfConfigurations("/invalid/dir/should/throw/IOException");
    }
}