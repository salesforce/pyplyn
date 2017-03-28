/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.threshold;

import com.salesforce.pyplyn.model.TransformationResult;
import org.testng.annotations.*;

import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.salesforce.pyplyn.duct.etl.transform.threshold.Threshold.Type.LESS_THAN;
import static org.mockito.Mockito.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 6.0
 */
public class ThresholdTest {
    private List<List<TransformationResult>> transformationResults;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<TransformationResult> series = Collections.singletonList(
                new TransformationResult(now, "metric", 10d, 10d)
        );

        transformationResults = Collections.singletonList(series);
    }

    @Test
    public void testLessThanValueIsOk() throws Exception {
        testThresholdAgainst(LESS_THAN, 1d, 1d, 1d, Threshold.Value.OK);
    }

    @Test
    public void testLessThanValueIsInfo() throws Exception {
        testThresholdAgainst(LESS_THAN, 1d, 1d, 10d, Threshold.Value.INFO);
    }

    @Test
    public void testLessThanValueIsWarn() throws Exception {
        testThresholdAgainst(LESS_THAN, 1d, 10d, 10d, Threshold.Value.WARN);
    }

    @Test
    public void testLessThanValueIsCrit() throws Exception {
        testThresholdAgainst(LESS_THAN, 10d, 10d, 10d, Threshold.Value.CRIT);
    }

    @Test
    public void testGreaterThanValueIsOk() throws Exception {
        testThresholdAgainst(Threshold.Type.GREATER_THAN, 20d, 20d, 20d, Threshold.Value.OK);
    }

    @Test
    public void testGreaterThanValueIsInfo() throws Exception {
        testThresholdAgainst(Threshold.Type.GREATER_THAN, 20d, 20d, 10d, Threshold.Value.INFO);
    }

    @Test
    public void testGreaterThanValueIsWarn() throws Exception {
        testThresholdAgainst(Threshold.Type.GREATER_THAN, 20d, 10d, 10d, Threshold.Value.WARN);
    }

    @Test
    public void testGreaterThanValueIsCrit() throws Exception {
        testThresholdAgainst(Threshold.Type.GREATER_THAN, 10d, 10d, 10d, Threshold.Value.CRIT);
    }

    @Test
    public void testEquality() throws Exception {
        // ARRANGE
        Threshold threshold1 = new Threshold("metric", 10d, 10d, 10d, LESS_THAN);
        Threshold threshold2 = new Threshold(null, null, null, null, LESS_THAN);
        Threshold threshold3 = new Threshold("metric", 10d, 10d, 10d, LESS_THAN);

        // ACT/ASSERT
        assertThat(threshold1, not(equalTo(threshold2)));
        assertThat(threshold1.hashCode(), equalTo(threshold3.hashCode()));
    }

    /**
     * Main test logic
     */
    private void testThresholdAgainst(Threshold.Type type, double crit, double warn, double info, Threshold.Value status) throws Exception {
        // ARRANGE
        Threshold threshold = new Threshold(null, crit, warn, info, type);

        // ACT
        List<List<TransformationResult>> actual = threshold.apply(transformationResults);

        // ASSERT
        assertThat(actual, not(empty()));
        assertThat(actual.get(0), not(empty()));

        TransformationResult result = actual.get(0).get(0);
        assertThat(result.value(), equalTo(status.value()));
    }

}