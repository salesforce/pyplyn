package com.salesforce.argus.model;

import com.salesforce.argus.model.builder.MetricResponseBuilderTest;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.*;

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