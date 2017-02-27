/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import com.salesforce.argus.model.builder.MetricResponseBuilderTest;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class MetricResponseTest {
    @Test
    public void testCacheKeyIsMetricName() throws Exception {
        // ARRANGE
        MetricResponse response = MetricResponseBuilderTest.defaultMetricResponseBuilder().build();

        // ACT/ASSERT
        assertThat(response.cacheKey(), equalTo(response.metric()));
    }
}