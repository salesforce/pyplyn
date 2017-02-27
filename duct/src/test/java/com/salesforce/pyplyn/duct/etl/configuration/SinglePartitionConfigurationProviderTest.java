/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

import static com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.ConfigurationsTestHelper.createCustomConfiguration;
import static com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.ConfigurationsTestHelper.createFullConfiguration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class SinglePartitionConfigurationProviderTest {
    private AppBootstrapFixtures fixtures;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testUpdateConfigurations() throws Exception {
        // ARRANGE
        Configuration configuration1 = createFullConfiguration(100L, false);
        Configuration configuration2 = createCustomConfiguration("argus", "refocus",
                "expression", "name",
                "subject", "aspect",
                100L, false);

        fixtures.realSinglePartitionConfigProvider()
                .configurationProviderReturns(configuration1)
                .freeze();

        SinglePartitionConfigurationProvider provider = (SinglePartitionConfigurationProvider)fixtures.configurationSetProvider();

        // ACT
        provider.run();
        Set<ConfigurationWrapper> firstSet = provider.get();

        // simulate an update of the configuration set
        fixtures.configurationProviderReturns(configuration2);
        provider.updateConfigurations();
        Set<ConfigurationWrapper> secondSet = provider.get();

        // ASSERT
        assertThat(firstSet, hasSize(1));
        assertThat(secondSet, hasSize(1));
        assertThat(firstSet, not(hasItems(secondSet.toArray(new ConfigurationWrapper[]{}))));
        verify(provider, times(0)).markFailure();
    }


    @Test
    public void testUpdateConfigurationsFailure() throws Exception {
        // ARRANGE
        Configuration configuration1 = createFullConfiguration(100L, false);

        fixtures.realSinglePartitionConfigProvider()
                .configurationProviderReturns(configuration1)
                .freeze();

        SinglePartitionConfigurationProvider provider = (SinglePartitionConfigurationProvider)fixtures.configurationSetProvider();

        // ACT
        provider.run();
        Set<ConfigurationWrapper> firstSet = provider.get();

        // simulate an update of the configuration set
        fixtures.configurationProviderThrowsException();
        provider.updateConfigurations();
        Set<ConfigurationWrapper> secondSet = provider.get();

        // ASSERT
        verify(provider, times(1)).markFailure();
        assertThat(firstSet, hasSize(1));
        assertThat("Expecting the old configuration list to be preserved on any failures", secondSet, hasSize(1));
    }
}