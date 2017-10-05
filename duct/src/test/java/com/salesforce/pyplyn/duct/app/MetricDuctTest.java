/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import static com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.ConfigurationsTestHelper.createThresholdTransforms;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.argus.model.ImmutableMetricResponse;
import com.salesforce.argus.model.MetricResponse;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationUpdateManager;
import com.salesforce.pyplyn.model.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 6.0
 */
public class MetricDuctTest {
    private AppBootstrapFixtures fixtures;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test(timeOut = 5000L)
    public void testRun() throws Exception {
        // ARRANGE
        // init transformation results
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Transmutation.Metadata metadata = ImmutableTransmutation.Metadata.builder().build();

        List<Transmutation> series1 = Arrays.asList(
                ImmutableTransmutation.of(now.minusMinutes(2),  "metric-ok", 1d, 1d, metadata),
                ImmutableTransmutation.of(now.minusMinutes(1),  "metric-ok", 2d, 2d, metadata),
                ImmutableTransmutation.of(now,  "metric-ok", 3d, 3d, metadata)
        );
        List<Transmutation> series2 = Arrays.asList(
                ImmutableTransmutation.of(now.minusMinutes(1),  "metric-warn", 100d, 100d, metadata),
                ImmutableTransmutation.of(now,  "metric-warn", 100d, 100d, metadata)
        );

        // init transforms
        List<Transform> transforms = createThresholdTransforms(null);

        // only run once
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.returnTransformationResultFromAllExtractProcessors(Arrays.asList(series1, series2))
                .argusToRefocusConfigurationWithTransformsAndRepeatInterval(transforms, 100L)
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Transmutation>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixtures.refocusLoadProcessor()).executeAsync(dataCaptor.capture(), any());

        List<Transmutation> data = dataCaptor.getValue();
        assertThat(data, hasSize(1));

        Transmutation result = data.get(0);
        assertThat(result.name(), equalTo("metric-warn"));
        assertThat(result.value(), equalTo(2d));
        assertThat(result.originalValue(), equalTo(100d));
        assertThat(result.metadata().messageCode(), equalTo("100"));
        assertThat(result.metadata().messages(), hasItems("metric-ok=3.00", "metric-warn=100.00"));
        assertThat(result.metadata().messages(), hasItem(containsString("WARN threshold hit by metric-warn")));
        assertThat(result.metadata().messages(), hasItem(containsString("value=100.00 GREATER_THAN 100.00")));
        assertThat(result.metadata().messages(), hasItem(containsString(now.toString())));
    }

    @Test(timeOut = 5000L)
    public void testPollingTransform() throws Exception {
        // ARRANGE
        String now = Long.valueOf(Instant.now().toEpochMilli()).toString();
        MetricResponse response = ImmutableMetricResponse.builder()
                .metric("argus-metric")
                .datapoints(new TreeMap<>(Collections.singletonMap(now, "1.2")))
                .build();

        // determine the number of retries
        final int numberOfRetries = 3;

        // only run once
        fixtures.appConfigMocks()
                .runOnce();

        // init the polling transform and have it respond after the third try
        TestPollingTransform testedPollingTransform = spy(new TestPollingTransform(numberOfRetries, 50, 10_000L, null, null));

        fixtures.argusClientReturns(singletonList(response))
                .callRealArgusExtractProcessor()
                .argusToRefocusConfigurationWithTransformsAndRepeatInterval(singletonList(testedPollingTransform), 1_000L)
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);

        // ASSERT
        verify(testedPollingTransform, times(1)).sendRequest(any());
        verify(testedPollingTransform, times(1 + numberOfRetries)).retrieveResult(any());
    }

    @Test(timeOut = 5000L)
    public void testPollingTransformTimeout() throws Exception {
        // ARRANGE
        String now = Long.valueOf(Instant.now().toEpochMilli()).toString();
        MetricResponse response = ImmutableMetricResponse.builder()
                .metric("argus-metric")
                .datapoints(new TreeMap<>(Collections.singletonMap(now, "1.2")))
                .build();

        // determine the number of retries
        final int numberOfRetries = 5;
        final long timeoutMs = 50L;

        // only run once
        fixtures.appConfigMocks()
                .runOnce();

        // ensure the transform times out by setting initialDelay > timeout
        TestPollingTransform testedPollingTransform = spy(new TestPollingTransform(numberOfRetries, timeoutMs, timeoutMs + 100, null, null));

        fixtures.argusClientReturns(singletonList(response))
                .callRealArgusExtractProcessor()
                .argusToRefocusConfigurationWithTransformsAndRepeatInterval(singletonList(testedPollingTransform), 1_000L)
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();

        // shutdown
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        verify(testedPollingTransform, times(1)).sendRequest(any());
        verify(testedPollingTransform, atLeast(0)).retrieveResult(any());
        TestPollingTransform.verifyLoggedException(testedPollingTransform, TimeoutException.class);


        // nothing to load, since the timeout should cause an empty response
        verify(fixtures.refocusLoadProcessor(), times(0)).execute(any(), any());
    }

    @Test(timeOut = 5000L)
    public void testPollingTransformCannotProcessEmpty() throws Exception {
        // ARRANGE
        // determine the number of retries
        final int numberOfRetries = 5;
        List<MetricResponse> inputMetrics = emptyList();

        // only run once
        fixtures.appConfigMocks()
                .runOnce();

        // ensure the transform times out by setting initialDelay = timeout
        TestPollingTransform testedPollingTransform = spy(new TestPollingTransform(numberOfRetries, 0, 5_000L, null, null));

        fixtures.argusClientReturns(inputMetrics)
                .callRealArgusExtractProcessor()
                .argusToRefocusConfigurationWithTransformsAndRepeatInterval(singletonList(testedPollingTransform), 1_000L)
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();

        // shutdown
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        verify(testedPollingTransform, times(0)).sendRequest(any());
        verify(testedPollingTransform, times(0)).retrieveResult(any());

        // nothing to load, since the timeout should cause an empty response
        verify(fixtures.refocusLoadProcessor(), times(0)).execute(any(), any());
    }


    /**
     * {@link PollingTransform} implementation, used for testing
     */
    static class TestPollingTransform extends PollingTransform<String> {
        private final AtomicInteger countDown;
        private List<List<Transmutation>> request;

        private final long initialDelayMillis;
        private final long backoffIntervalMillis;
        private final long timeoutMillis;

        private final Double threshold;
        private final ThresholdType type;

        /**
         * Initialize the number of retries after which the retrieve method will return a valid response
         */
        private TestPollingTransform(int retryThisManyTimes, long initialDelay, long timeout, Double threshold, ThresholdType type) {
            this.countDown = new AtomicInteger(retryThisManyTimes);
            this.initialDelayMillis = initialDelay;
            this.timeoutMillis = timeout;
            this.backoffIntervalMillis = 10L;
            this.threshold = threshold;
            this.type = type;
        }

        @Override
        public String endpoint() {
            return "endpoint";
        }

        @Override
        public long backoffIntervalMillis() {
            return backoffIntervalMillis;
        }

        @Override
        public long initialDelayMillis() {
            return initialDelayMillis;
        }

        @Override
        public long timeoutMillis() {
            return timeoutMillis;
        }

        @Override
        public Double threshold() {
            return threshold;
        }

        @Override
        public ThresholdType type() {
            return type;
        }

        @Override
        public String sendRequest(List<List<Transmutation>> input) {
            this.request = input;

            return "id";
        }

        @Override
        public List<List<Transmutation>> retrieveResult(String request) {
            // return null until condition is satisfied
            if (countDown.getAndDecrement() > 0) {
                return null;
            }

            return this.request;
        }

        private static void verifyLoggedException(TestPollingTransform pt, Class<? extends Throwable> t) {
            verify(pt, times(1)).logAsyncError(any(t));
        }
    }
}