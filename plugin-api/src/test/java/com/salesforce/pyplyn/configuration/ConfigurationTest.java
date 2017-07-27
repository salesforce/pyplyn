/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

import com.salesforce.pyplyn.model.*;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
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
        Configuration configuration1 = ImmutableConfiguration.of(0, emptyList(), emptyList(), emptyList(), false);
        Configuration configuration2 = ImmutableConfiguration.of(0, emptyList(), emptyList(), emptyList(), false);

        // ACT/ASSERT
        assertConfigurationsEqual(configuration1, configuration2);
    }

    @Test
    public void testConfigurationsWithSameEmptyETLAreEqual() throws Exception {
        // ARRANGE
        Configuration configuration1 = ImmutableConfiguration.of(0, emptyList(), emptyList(), emptyList(), false);
        Configuration configuration2 = ImmutableConfiguration.of(1, emptyList(), emptyList(), emptyList(), true);

        // ACT/ASSERT
        assertConfigurationsEqual(configuration1, configuration2);
    }

    @Test
    public void testConfigurationsWithSameETLAreEqual() throws Exception {
        // ARRANGE
        Extract e1 = new ExtractImpl("same");
        Extract e2 = new ExtractImpl("same");

        Configuration configuration1 = ImmutableConfiguration.of(0, singletonList(e1), emptyList(), emptyList(), false);
        Configuration configuration2 = ImmutableConfiguration.of(0, singletonList(e2), emptyList(), emptyList(), false);

        // ACT/ASSERT
        assertConfigurationsEqual(configuration1, configuration2);
    }


    @Test
    public void testConfigurationsWithDifferentETLAreEqual() throws Exception {
        // ARRANGE
        Extract e = new ExtractImpl("same");

        Configuration configuration1 = ImmutableConfiguration.of(0, singletonList(e), emptyList(), emptyList(), false);
        Configuration configuration2 = ImmutableConfiguration.of(0, emptyList(), emptyList(), emptyList(), false);

        // ACT/ASSERT
        assertConfigurationsAreNotEqual(configuration1, configuration2);
    }


    @Test
    public void testConfigurationGetters() throws Exception {
        // ARRANGE
        List<Extract> extracts = singletonList(new Extract() {});
        List<Transform> transforms = singletonList((Transform) input -> null);
        List<Load> loads = singletonList(new Load() {});

        Configuration configuration = ImmutableConfiguration.of(100L, extracts, transforms, loads, true);

        // ACT
        boolean enabled = !configuration.disabled();
        long repeatIntervalMillis = configuration.repeatIntervalMillis();
        List<Extract> extract = configuration.extract();
        List<Transform> transform = configuration.transform();
        List<Load> load = configuration.load();

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