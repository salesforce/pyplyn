/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Instant;
import java.util.TreeMap;

import org.testng.annotations.Test;

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
        MetricResponse response = ImmutableMetricResponse.builder().metric("metric").build();


        // ACT/ASSERT
        assertThat(response.cacheKey(), equalTo(response.metric()));
    }

    @Test
    public void testToStringContainsLastDatapoint() throws Exception {
        // ARRANGE
        String now = Long.valueOf(Instant.now().toEpochMilli()).toString();
        TreeMap<String, String> map = new TreeMap<>();
        map.put(now, "1.0");
        map.put(now+1, "2.0");
        map.put(now+2, "3.0");

        MetricResponse response = ImmutableMetricResponse.builder()
                .metric("metric")
                .datapoints(map)
                .build();


        // ACT
        String stringResponse = response.toString();


        // ASSERT
        assertThat(stringResponse, containsString("metric"));
        assertThat(stringResponse, containsString("3.0"));
        assertThat(stringResponse, not(containsString("2.0")));
        assertThat(stringResponse, not(containsString("1.0")));
    }
}