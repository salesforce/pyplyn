/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.MetricResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test class
 * <p/>Ensures that the {@link MetricResponseBuilder} functions as expected (allows composition of fields and generates
 *  an immutable {@link MetricResponse} object when {@link MetricResponseBuilder#build()} is called
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class MetricResponseBuilderTest {
    private MetricResponseBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        builder = defaultMetricResponseBuilder();
    }

    @Test
    public void testExpectedFields() throws Exception {
        // ARRANGE
        MetricResponse metricResponse = builder.build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(metricResponse);
    }

    @Test
    public void testExpectedFieldsUsingTheCopyConstructor() throws Exception {
        // ARRANGE
        MetricResponse metricResponse = new MetricResponseBuilder(builder.build()).build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(metricResponse);
    }

    /**
     * Runs all the assertions
     */
    private void assertAllExpectedFieldValues(MetricResponse metricResponse) {
        assertThat(metricResponse.scope(), equalTo("scope"));
        assertThat(metricResponse.metric(), equalTo("metric"));
        assertThat(metricResponse.tags(), equalTo(Collections.singletonMap("tag", "value")));
        assertThat(metricResponse.namespace(), equalTo("namespace"));
        assertThat(metricResponse.displayName(), equalTo("displayName"));
        assertThat(metricResponse.units(), equalTo("units"));
        assertThat(metricResponse.datapoints(), equalTo(datapointMap()));
    }

    /**
     * Initializes a default object builder
     */
    public static MetricResponseBuilder defaultMetricResponseBuilder() {
        return new MetricResponseBuilder()
                .withScope("scope")
                .withMetric("metric")
                .withTags(Collections.singletonMap("tag", "value"))
                .withNamespace("namespace")
                .withDisplayName("displayName")
                .withUnits("units")
                .withDatapoints(datapointMap());
    }

    /**
     * Constructs a {@link SortedMap} containing one datapoint
     */
    public static SortedMap<String, String> datapointMap() {
        return new TreeMap<>(Collections.singletonMap("datapoint", "value"));
    }
}