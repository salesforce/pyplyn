/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.argus;

import static java.util.stream.Collectors.toCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.*;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.argus.model.ImmutableMetricResponse;
import com.salesforce.argus.model.MetricResponse;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapLatches;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationUpdateManager;
import com.salesforce.pyplyn.model.Transmutation;
import com.salesforce.pyplyn.status.MeterType;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class ArgusExtractProcessorTest {
    private AppBootstrapFixtures fixtures;
    private ArgumentCaptor<MeterType> meterTypeArgumentCaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
        meterTypeArgumentCaptor = ArgumentCaptor.forClass(MeterType.class);
    }

    @Test
    public void testProcessShouldFailWhenClientIsMissing() throws Exception {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfiguration()
                .callRealArgusExtractProcessor()
                .argusClientCanNotAuth()
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        // since we had no real client, expecting ArgusExtractProcessor to have logged a failure (ExtractFailure and AuthenticationFailure)
        verify(fixtures.systemStatus(), times(2)).meter(eq("Argus"), meterTypeArgumentCaptor.capture());
        assert(meterTypeArgumentCaptor.getAllValues().stream().anyMatch(meterType -> meterType.processStatus().name().equals("ExtractFailure")));
    }


    @Test
    public void testDefaultValueProvidedOnTimeout() throws Exception {
        // ARRANGE
        MetricResponse response = ImmutableMetricResponse.builder()
                .metric("argus-metric")
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfigurationWithDefaultValueAndRepeatInterval(1.2d, 100L)
                .callRealArgusExtractProcessor()
                .argusClientReturns(Collections.singletonList(response))
                .initializeFixtures();

        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);

        // ASSERT
        verify(fixtures.systemStatus(), times(1)).meter(eq("Argus"), meterTypeArgumentCaptor.capture());
        assert(meterTypeArgumentCaptor.getValue().processStatus().name().equals("ExtractSuccess"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Transmutation>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixtures.refocusLoadProcessor()).executeAsync(dataCaptor.capture(), any());

        List<Transmutation> data = dataCaptor.getValue();
        assertThat(data, hasSize(1));

        Transmutation result = data.get(0);
        assertThat(result.name(), equalTo("argus-metric"));
        assertThat(result.value(), equalTo(1.2d));
        assertThat(result.originalValue(), equalTo(1.2d));
        assertThat(result.metadata().messages(), hasItem(containsString("Default value")));
    }

    @Test
    public void testMetricResponsesAreCached() throws Exception {
        // ARRANGE
        String now = Long.valueOf(Instant.now().toEpochMilli()).toString();
        MetricResponse response = ImmutableMetricResponse.builder()
                .metric("argus-metric")
                .datapoints(new TreeMap<>(Collections.singletonMap(now, "1.2")))
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfigurationWithCache()
                .realMetricResponseCache()
                .callRealArgusExtractProcessor()
                .argusClientReturns(Collections.singletonList(response))
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(false);


        // ACT
        manager = fixtures.initConfigurationManager().configurationManager();
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);

        // ASSERT
        // two instances of ExtractSuccess
        verify(fixtures.systemStatus(), times(2)).meter(eq("Argus"), meterTypeArgumentCaptor.capture());
        meterTypeArgumentCaptor.getAllValues().forEach(meterType -> {
            assert(meterType.processStatus().name().equals("ExtractSuccess"));
        });
        verify(fixtures.metricResponseCache(), times(4)).isCached("argus-metric");
        verify(fixtures.metricResponseCache(), times(1)).cache(any(), anyLong());
    }

    @Test
    public void testMetricResponsesWithNoDatapointsAreNotCached() throws Exception {
        // ARRANGE
        MetricResponse response = ImmutableMetricResponse.builder()
                .metric("argus-metric")
                .datapoints(Collections.emptySortedMap())
                .build();

        // bootstrap
        try {
            fixtures.enableLatches()
                    .oneArgusToRefocusConfigurationWithCache()
                    .realMetricResponseCache()
                    .argusClientReturns(Collections.singletonList(response))
                    .callRealArgusExtractProcessor()
                    .initializeFixtures();

            // init app
            ConfigurationUpdateManager manager = fixtures.configurationManager();

            // ACT
            manager.run();
            AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().countDown();

            // shutdown
            AppBootstrapLatches.holdOffUntilExtractProcessorFinishes().await();
            fixtures.shutdownHook().shutdown();

            // wait for shutdown
            AppBootstrapLatches.appHasShutdown().await();


            // ASSERT

            // one of each of ExtractSuccess and ExtractNoDataReturned
            verify(fixtures.systemStatus(), times(2)).meter(eq("Argus"), meterTypeArgumentCaptor.capture());
            List<String> processStatuses = meterTypeArgumentCaptor.getAllValues()
                                                                  .stream()
                                                                  .map(meterType -> meterType.processStatus().name())
                                                                  .collect(toCollection(ArrayList::new));
            assert(processStatuses.contains("ExtractSuccess"));
            assert(processStatuses.contains("ExtractNoDataReturned"));
            assertThat(String.format("Expected 2 elements, found %d", processStatuses.size()), processStatuses.size() == 2);

            verify(fixtures.metricResponseCache(), times(2)).isCached("argus-metric");
            verify(fixtures.metricResponseCache(), times(0)).cache(any(), anyLong());

        } finally {
            AppBootstrapLatches.release();
        }
    }

    @Test
    public void testProcessShouldFailWithNullResponse() throws Exception {
        testWithMetricResponse(null);

        // ASSERT
        // since no default value is specified, should throw ExtractFailure and ExtractNoDataReturned
        verify(fixtures.systemStatus(), times(2)).meter(eq("Argus"), meterTypeArgumentCaptor.capture());
        assert(meterTypeArgumentCaptor.getAllValues().stream().anyMatch(meterType -> meterType.processStatus().name().equals("ExtractNoDataReturned")));
        assert(meterTypeArgumentCaptor.getAllValues().stream().anyMatch(meterType -> meterType.processStatus().name().equals("ExtractFailure")));
    }

    @Test
    public void testProcessShouldFailWithEmptyDatapoints() throws Exception {
        // create a sample
        MetricResponse response = ImmutableMetricResponse.builder()
                .metric("argus-metric")
                .datapoints(Collections.emptySortedMap())
                .build();

        testWithMetricResponse(Collections.singletonList(response));


        // ASSERT
        // expecting ExtractSuccess and ExtractNoDataReturned
        verify(fixtures.systemStatus(), times(2)).meter(eq("Argus"), meterTypeArgumentCaptor.capture());
        assert(meterTypeArgumentCaptor.getAllValues().stream().anyMatch(meterType -> meterType.processStatus().name().equals("ExtractNoDataReturned")));
    }


    @Test
    public void testProcessShouldFailWhenGetMetricsThrowsExceptions() throws Exception {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfigurationWithDefaultValueAndRepeatInterval(null, 100L)
                .callRealArgusExtractProcessor()
                .argusClientThrowsExceptionOnGetMetrics()
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        verify(fixtures.systemStatus(), times(1)).meter(eq("Argus"), meterTypeArgumentCaptor.capture());
        assert(meterTypeArgumentCaptor.getAllValues().stream().anyMatch(meterType -> meterType.processStatus().name().equals("ExtractFailure")));
    }


    /**
     * Executes a test that assumes a failure when a bad sample is returned from the Endpoint
     */
    private void testWithMetricResponse(List<MetricResponse> response) throws UnauthorizedException, InterruptedException {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfigurationWithDefaultValueAndRepeatInterval(null, 100L)
                .callRealArgusExtractProcessor()
                .argusClientReturns(response)
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);
    }
}