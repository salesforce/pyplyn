/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.standard;

import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.StatusCode;
import com.salesforce.pyplyn.model.ThresholdType;
import com.salesforce.pyplyn.model.Transmutation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static com.salesforce.pyplyn.model.StatusCode.*;
import static com.salesforce.pyplyn.model.ThresholdType.LESS_THAN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
    public void testLessThanValueIsOk() throws Exception {
        testThresholdAgainst(LESS_THAN, 1d, 1d, 1d, OK);
    }

    @Test
    public void testLessThanValueIsInfo() throws Exception {
        testThresholdAgainst(LESS_THAN, 1d, 1d, 10d, INFO);
    }

    @Test
    public void testLessThanValueIsWarn() throws Exception {
        testThresholdAgainst(LESS_THAN, 1d, 10d, 10d, WARN);
    }

    @Test
    public void testLessThanValueIsCrit() throws Exception {
        testThresholdAgainst(LESS_THAN, 10d, 10d, 10d, ERR);
    }

    @Test
    public void testGreaterThanValueIsOk() throws Exception {
        testThresholdAgainst(ThresholdType.GREATER_THAN, 20d, 20d, 20d, OK);
    }

    @Test
    public void testGreaterThanValueIsInfo() throws Exception {
        testThresholdAgainst(ThresholdType.GREATER_THAN, 20d, 20d, 10d, INFO);
    }

    @Test
    public void testGreaterThanValueIsWarn() throws Exception {
        testThresholdAgainst(ThresholdType.GREATER_THAN, 20d, 10d, 10d, WARN);
    }

    @Test
    public void testGreaterThanValueIsCrit() throws Exception {
        testThresholdAgainst(ThresholdType.GREATER_THAN, 10d, 10d, 10d, ERR);
    }

    @Test
    public void testEquality() throws Exception {
        // ARRANGE
        Threshold threshold1 = ImmutableThreshold.of("metric", 10d, 10d, 10d, LESS_THAN);
        Threshold threshold2 = ImmutableThreshold.of(null, null, null, null, LESS_THAN);
        Threshold threshold3 = ImmutableThreshold.of("metric", 10d, 10d, 10d, LESS_THAN);

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