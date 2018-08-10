/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.standard;

import static com.salesforce.pyplyn.model.StatusCode.*;
import static com.salesforce.pyplyn.model.ThresholdType.EQUAL_TO;
import static com.salesforce.pyplyn.model.ThresholdType.LESS_THAN_OR_EQ;
import static com.salesforce.pyplyn.model.ThresholdType.GREATER_THAN_OR_EQ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.StatusCode;
import com.salesforce.pyplyn.model.ThresholdType;
import com.salesforce.pyplyn.model.Transmutation;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 6.0
 */
public class ThresholdTest {
    private List<List<Transmutation>> transformationResults;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Transmutation.Metadata metadata = ImmutableTransmutation.Metadata.builder().build();
        List<Transmutation> series = Collections.singletonList(
                ImmutableTransmutation.of(now.minusMinutes(2),  "metric", 10d, 10d, metadata)
        );

        transformationResults = Collections.singletonList(series);
    }

    @Test
    public void testLessThanOrEqualToValueIsOk() throws Exception {
        testThresholdAgainst(LESS_THAN_OR_EQ, 1d, 1d, 1d, OK);
    }

    @Test
    public void testLessThanOrEqualToValueIsInfo() throws Exception {
        testThresholdAgainst(LESS_THAN_OR_EQ, 1d, 1d, 10d, INFO);
    }

    @Test
    public void testLessThanOrEqualToValueIsWarn() throws Exception {
        testThresholdAgainst(LESS_THAN_OR_EQ, 1d, 10d, 10d, WARN);
    }

    @Test
    public void testLessThanOrEqualToValueIsCrit() throws Exception {
        testThresholdAgainst(LESS_THAN_OR_EQ, 10d, 10d, 10d, CRIT);
    }

    @Test
    public void testGreaterThanOrEqualToValueIsOk() throws Exception {
        testThresholdAgainst(GREATER_THAN_OR_EQ, 20d, 20d, 20d, OK);
    }

    @Test
    public void testGreaterThanOrEqualToValueIsInfo() throws Exception {
        testThresholdAgainst(GREATER_THAN_OR_EQ, 20d, 20d, 10d, INFO);
    }

    @Test
    public void testGreaterThanOrEqualToValueIsWarn() throws Exception {
        testThresholdAgainst(GREATER_THAN_OR_EQ, 20d, 10d, 10d, WARN);
    }

    @Test
    public void testGreaterThanOrEqualToValueIsCrit() throws Exception {
        testThresholdAgainst(GREATER_THAN_OR_EQ, 10d, 10d, 10d, CRIT);
    }

    @Test
    public void testEquality() throws Exception {
        // ARRANGE
        Threshold threshold1 = ImmutableThreshold.of("metric", 10d, 10d, 10d, EQUAL_TO);
        Threshold threshold2 = ImmutableThreshold.of(null, null, null, null, EQUAL_TO);
        Threshold threshold3 = ImmutableThreshold.of("metric", 10d, 10d, 10d, EQUAL_TO);

        // ACT/ASSERT
        assertThat(threshold1, not(equalTo(threshold2)));
        assertThat(threshold1.hashCode(), equalTo(threshold3.hashCode()));
    }

    /**
     * Main test logic
     */
    private void testThresholdAgainst(ThresholdType type, double crit, double warn, double info, StatusCode status) throws Exception {
        // ARRANGE
        Threshold threshold = ImmutableThreshold.of(null, crit, warn, info, type);

        // ACT
        List<List<Transmutation>> actual = threshold.apply(transformationResults);

        // ASSERT
        assertThat(actual, not(empty()));
        assertThat(actual.get(0), not(empty()));

        Transmutation result = actual.get(0).get(0);
        assertThat(result.value(), equalTo(status.value()));
    }

}