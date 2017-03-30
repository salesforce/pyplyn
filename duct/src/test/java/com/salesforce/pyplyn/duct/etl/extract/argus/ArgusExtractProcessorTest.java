/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.argus;

import com.salesforce.argus.model.MetricResponse;
import com.salesforce.argus.model.builder.MetricResponseBuilder;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.MetricDuct;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.status.MeterType;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class ArgusExtractProcessorTest {
    private AppBootstrapFixtures fixtures;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testProcessShouldFailWhenClientIsMissing() throws Exception {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfiguration()
                .argusClientCanNotAuth()
                .callRealArgusExtractProcessor()
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        // since we had no real client, expecting ArgusExtractProcessor to have logged a failure
        verify(fixtures.systemStatus(), times(1)).meter("Argus", MeterType.ExtractFailure);
    }


    @Test
    public void testDefaultValueProvidedOnTimeout() throws Exception {
        // ARRANGE
        MetricResponse response = new MetricResponseBuilder()
                .withMetric("argus-metric")
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfigurationWithDefaultValue(1.2d)
                .callRealArgusExtractProcessor()
                .argusClientReturns(Collections.singletonList(response))
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        verify(fixtures.systemStatus(), times(1)).meter("Argus", MeterType.ExtractSuccess);
        verify(fixtures.systemStatus(), times(0)).meter("Argus", MeterType.ExtractFailure);
        verify(fixtures.systemStatus(), times(0)).meter("Argus", MeterType.ExtractNoDataReturned);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TransformationResult>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixtures.refocusLoadProcessor()).execute(dataCaptor.capture(), any());

        List<TransformationResult> data = dataCaptor.getValue();
        assertThat(data, hasSize(1));

        TransformationResult result = data.get(0);
        assertThat(result.name(), equalTo("argus-metric"));
        assertThat(result.value(), equalTo(1.2d));
        assertThat(result.originalValue(), equalTo(1.2d));
        assertThat(result.metadata().messages(), hasItem(containsString("Default value")));
    }

    @Test
    public void testMetricResponsesAreCached() throws Exception {
        // ARRANGE
        String now = Long.valueOf(Instant.now().toEpochMilli()).toString();
        MetricResponse response = new MetricResponseBuilder()
                .withMetric("argus-metric")
                .withDatapoints(new TreeMap<>(Collections.singletonMap(now, "1.2")))
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfigurationWithCache()
                .realMetricResponseCache()
                .callRealArgusExtractProcessor()
                .argusClientReturns(Collections.singletonList(response))
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();
        app.run();

        // ACT
        app.run();

        // ASSERT
        verify(fixtures.systemStatus(), times(2)).meter("Argus", MeterType.ExtractSuccess);
        verify(fixtures.metricResponseCache(), times(4)).isCached("argus-metric");
        verify(fixtures.metricResponseCache(), times(1)).cache(any(), anyLong());
    }

    @Test
    public void testMetricResponsesWithNoDatapointsAreNotCached() throws Exception {
        // ARRANGE
        MetricResponse response = new MetricResponseBuilder()
                .withMetric("argus-metric")
                .withDatapoints(Collections.emptySortedMap())
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfigurationWithCache()
                .realMetricResponseCache()
                .callRealArgusExtractProcessor()
                .argusClientReturns(Collections.singletonList(response))
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        verify(fixtures.systemStatus(), times(1)).meter("Argus", MeterType.ExtractSuccess);
        verify(fixtures.metricResponseCache(), times(2)).isCached("argus-metric");
        verify(fixtures.metricResponseCache(), times(0)).cache(any(), anyLong());
    }

    @Test
    public void testProcessShouldFailWithNullResponse() throws Exception {
        testWithMetricResponse(null);

        // ASSERT
        verify(fixtures.systemStatus(), times(1)).meter("Argus", MeterType.ExtractFailure);
    }

    @Test
    public void testProcessShouldFailWithEmptyDatapoints() throws Exception {
        // create a sample
        MetricResponse response = new MetricResponseBuilder()
                .withMetric("argus-metric")
                .withDatapoints(Collections.emptySortedMap())
                .build();

        testWithMetricResponse(Collections.singletonList(response));


        // ASSERT
        verify(fixtures.systemStatus(), times(1)).meter("Argus", MeterType.ExtractNoDataReturned);
    }


    @Test
    public void testProcessShouldFailWhenGetMetricsThrowsExceptions() throws Exception {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfigurationWithDefaultValue(null)
                .callRealArgusExtractProcessor()
                .argusClientThrowsExceptionOnGetMetrics()
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        verify(fixtures.systemStatus(), times(1)).meter("Argus", MeterType.ExtractFailure);
    }


    /**
     * Executes a test that assumes a failure when a bad sample is returned from the Endpoint
     */
    private void testWithMetricResponse(List<MetricResponse> response) throws UnauthorizedException {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfigurationWithDefaultValue(null)
                .callRealArgusExtractProcessor()
                .argusClientReturns(response)
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();
    }
}