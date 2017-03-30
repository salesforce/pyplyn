/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.ExtractImpl;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConfigurationTest {
    @Test
    public void testEmptyConfigurationsAreEqual() throws Exception {
        // ARRANGE
        Configuration configuration1 = new Configuration(0, new Extract[]{}, new Transform[]{}, new Load[]{}, false);
        Configuration configuration2 = new Configuration(0, new Extract[]{}, new Transform[]{}, new Load[]{}, false);

        // ACT/ASSERT
        assertConfigurationsEqual(configuration1, configuration2);
    }

    @Test
    public void testConfigurationsWithSameEmptyETLAreEqual() throws Exception {
        // ARRANGE
        Configuration configuration1 = new Configuration(0, new Extract[]{}, new Transform[]{}, new Load[]{}, false);
        Configuration configuration2 = new Configuration(1, new Extract[]{}, new Transform[]{}, new Load[]{}, true);

        // ACT/ASSERT
        assertConfigurationsEqual(configuration1, configuration2);
    }

    @Test
    public void testConfigurationsWithSameETLAreEqual() throws Exception {
        // ARRANGE
        Extract e1 = new ExtractImpl("same");
        Extract e2 = new ExtractImpl("same");

        Configuration configuration1 = new Configuration(0, new Extract[]{e1}, new Transform[]{}, new Load[]{}, false);
        Configuration configuration2 = new Configuration(0, new Extract[]{e2}, new Transform[]{}, new Load[]{}, false);

        // ACT/ASSERT
        assertConfigurationsEqual(configuration1, configuration2);
    }


    @Test
    public void testConfigurationsWithDifferentETLAreEqual() throws Exception {
        // ARRANGE
        Extract e = new ExtractImpl("same");

        Configuration configuration1 = new Configuration(0, new Extract[]{e}, new Transform[]{}, new Load[]{}, false);
        Configuration configuration2 = new Configuration(0, new Extract[]{}, new Transform[]{}, new Load[]{}, false);

        // ACT/ASSERT
        assertConfigurationsAreNotEqual(configuration1, configuration2);
    }


    @Test
    public void testConfigurationGetters() throws Exception {
        // ARRANGE
        Extract[] extracts = {};
        Transform[] transforms = {};
        Load[] loads = {};

        Configuration configuration = new Configuration(100L, extracts, transforms, loads, true);

        // ACT
        boolean enabled = configuration.isEnabled();
        long repeatIntervalMillis = configuration.repeatIntervalMillis();
        Extract[] extract = configuration.extract();
        Transform[] transform = configuration.transform();
        Load[] load = configuration.load();

        // ASSERT
        assertThat("Configuration should be disabled", enabled, is(false));
        assertThat("Expecting same repeat interval", repeatIntervalMillis, is(100L));
        assertThat("Extract should have been copied on get", extract, not(sameInstance(extracts)));
        assertThat("Transform should have been copied on get", transform, not(sameInstance(transforms)));
        assertThat("Load should have been copied on get", load, not(sameInstance(loads)));
    }


    /**
     * Tests if two configurations are equal
     */
    private void assertConfigurationsEqual(Configuration configuration1, Configuration configuration2) {
        // ACT
        boolean equal = configuration1.equals(configuration2);
        boolean sameHashcode = configuration1.hashCode() == configuration2.hashCode();

        // ASSERT
        assertThat("Expecting equal configurations", equal, is(true));
        assertThat("Expecting different instances", configuration1, not(sameInstance(configuration2)));
        assertThat("Expecting hashcodes to be the same", sameHashcode, is(true)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }


    /**
     * Tests if two configurations are not equal
     */
    private void assertConfigurationsAreNotEqual(Configuration configuration1, Configuration configuration2) {
        // ACT
        boolean equal = configuration1.equals(configuration2);
        boolean sameHashcode = configuration1.hashCode() == configuration2.hashCode();

        // ASSERT
        assertThat("Expecting configurations to be different", equal, is(false));
        assertThat("Expecting different instances", configuration1, not(sameInstance(configuration2)));
        assertThat("Expecting hashcodes to be different", sameHashcode, is(false));
    }
}