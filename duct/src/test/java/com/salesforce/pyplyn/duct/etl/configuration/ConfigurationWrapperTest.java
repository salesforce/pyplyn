/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.salesforce.pyplyn.configuration.UpdatableConfigurationSetProvider;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class ConfigurationWrapperTest {
    private AppBootstrapFixtures fixtures;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testLastRunIsUpdatedAfterETLCycle() throws Exception {
        // ARRANGE
        fixtures.appConfigMocks().runOnce();
        fixtures.oneArgusToRefocusConfiguration()
                .freeze();

        // ACT
        fixtures.app().run();

        // ASSERT
        UpdatableConfigurationSetProvider<ConfigurationWrapper> configurationWrapperProvider = fixtures.configurationSetProvider();
        assertThat(configurationWrapperProvider.get(), hasSize(1));
        configurationWrapperProvider.get().forEach(configuration -> verify(configuration).ran());
    }
}