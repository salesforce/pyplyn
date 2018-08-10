/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.testng.annotations.Test;
/**
 * Test class
 *
 * @author Chris Coraggio &lt;chris.coraggio@salesforce.com&gt;
 * @since 10.3.5
 */

public class ThresholdTypeTest {

    // ARRANGE
    /**
     * We do not use mocking in this test class due to how inexpensive it is to instantiate ThresholdType
     */

    @Test
    public void testThresholds() throws Exception {

        /**
         * Enumerate the comparison cases for ThresholdType
         *
         * The cases follow this pattern (except for null checking):
         *
         * value > threshold
         * value = threshold
         * value < threshold
         */

        // ACT/ASSERT

        validateThreshold(ThresholdType.EQUAL_TO, 2, .000, false);
        validateThreshold(ThresholdType.EQUAL_TO, .001, .000, true);
        validateThreshold(ThresholdType.EQUAL_TO, 100, 101.0, false);

        validateThreshold(ThresholdType.GREATER_THAN, 1, 0.000, true);
        validateThreshold(ThresholdType.GREATER_THAN, 0.000, 0.000, false);
        validateThreshold(ThresholdType.GREATER_THAN, -1, 0.000, false);

        validateThreshold(ThresholdType.LESS_THAN, 43.2, 43.1, false);
        validateThreshold(ThresholdType.LESS_THAN, 43.2, 43.2, false);
        validateThreshold(ThresholdType.LESS_THAN, 42.4, 43.2, true);

        validateThreshold(ThresholdType.GREATER_THAN_OR_EQ, 0, -1.0, true);
        validateThreshold(ThresholdType.GREATER_THAN_OR_EQ, 3.5, 3.5, true);
        validateThreshold(ThresholdType.GREATER_THAN_OR_EQ, -7, -1.0, false);

        validateThreshold(ThresholdType.LESS_THAN_OR_EQ, 3.5, 3.0, false);
        validateThreshold(ThresholdType.LESS_THAN_OR_EQ, 3.5, 3.5, true);
        validateThreshold(ThresholdType.LESS_THAN_OR_EQ, 0, 1.0, true);

        validateThreshold(ThresholdType.GREATER_THAN, 867530.9, null, false);
        validateThreshold(ThresholdType.EQUAL_TO, 0, null, false);
        validateThreshold(ThresholdType.LESS_THAN, -1000, null, false);
    }

    /**
     *
     * Asserts that the comparison between value and threshold is expected
     *
     * @param type - the comparator
     * @param value - value to compare
     * @param threshold - the value to compare against
     * @param expected - true/false expectation of comparison
     */

    private void validateThreshold(ThresholdType type, Number value, Double threshold, boolean expected) {
        boolean result = type.matches(value, threshold);
        assertThat(String.format("%f should be %s %f", value.doubleValue(), type.name(), threshold), result, is(expected));
    }
}