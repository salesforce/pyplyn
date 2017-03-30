/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.salesforce.pyplyn.configuration.Configuration;
import org.testng.annotations.Test;

import static com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.ConfigurationsTestHelper.createFullConfiguration;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class DuctConfigurationTest {
    @Test
    public void testAllKnownETLObjects() throws Exception {
        // ARRANGE
        Configuration configuration1 = createFullConfiguration(100L, false);
        Configuration configuration2 = createFullConfiguration(200L, false);

        // ASSERT
        assertThat("Configurations should be equal based on their E/T/L parameters, but not other params", configuration1, equalTo(configuration2));
    }


    @Test
    public void testConfigurationWrapperDelegatesEquality() throws Exception {
        // ARRANGE
        ConfigurationWrapper configuration1 =
                new ConfigurationWrapper(createFullConfiguration(100L, false), null);
        ConfigurationWrapper configuration2 =
                new ConfigurationWrapper(createFullConfiguration(200L, false), null);

        // ASSERT
        assertThat("Configuration wrappers should delegate equality to their Configuration objects", configuration1, equalTo(configuration2));
    }
}
