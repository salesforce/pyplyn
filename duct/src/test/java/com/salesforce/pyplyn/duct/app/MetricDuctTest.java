/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import com.salesforce.argus.model.MetricResponse;
import com.salesforce.argus.model.builder.MetricResponseBuilder;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationUpdateManager;
import com.salesforce.pyplyn.model.PollingTransform;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.ConfigurationsTestHelper.createThresholdTransforms;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

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

    @Test
    public void testRun() throws Exception {
        // ARRANGE
        // init transformation results
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<TransformationResult> series1 = Arrays.asList(
                new TransformationResult(now.minusMinutes(2), "metric-ok", 1d, 1d),
                new TransformationResult(now.minusMinutes(1), "metric-ok", 2d, 2d),
                new TransformationResult(now, "metric-ok", 3d, 3d)
        );
        List<TransformationResult> series2 = Arrays.asList(
                new TransformationResult(now.minusMinutes(1), "metric-warn", 100d, 100d),
                new TransformationResult(now, "metric-warn", 100d, 100d)
        );

        // init transforms
        Transform[] transforms = createThresholdTransforms(null);

        // only run once
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.returnTransformationResultFromAllExtractProcessors(Arrays.asList(series1, series2))
                .argusToRefocusConfigurationWithTransformsAndRepeatInterval(transforms, 100)
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TransformationResult>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixtures.refocusLoadProcessor()).executeAsync(dataCaptor.capture(), any());

        List<TransformationResult> data = dataCaptor.getValue();
        assertThat(data, hasSize(1));

        TransformationResult result = data.get(0);
        assertThat(result.name(), equalTo("metric-warn"));
        assertThat(result.value(), equalTo(2d));
        assertThat(result.originalValue(), equalTo(100d));
        assertThat(result.metadata().messageCode(), equalTo("100"));
        assertThat(result.metadata().messages(), hasItems("metric-ok=3.00", "metric-warn=100.00"));
        assertThat(result.metadata().messages(), hasItem(containsString("WARN threshold hit by metric-warn")));
        assertThat(result.metadata().messages(), hasItem(containsString("value=100.00 GREATER_THAN 100.00")));
        assertThat(result.metadata().messages(), hasItem(containsString(now.toString())));
    }

    @Test
    public void testPollingTransform() throws Exception {
        // ARRANGE
        String now = Long.valueOf(Instant.now().toEpochMilli()).toString();
        MetricResponse response = new MetricResponseBuilder()
                .withMetric("argus-metric")
                .withDatapoints(new TreeMap<>(Collections.singletonMap(now, "1.2")))
                .build();

        // determine the number of retries
        final int numberOfRetries = 3;

        // only run once
        fixtures.appConfigMocks()
                .runOnce();

        // init the polling transform and have it respond after the third try
        TestPollingTransform testedPollingTransform = spy(new TestPollingTransform(numberOfRetries, 50));

        fixtures.argusClientReturns(Collections.singletonList(response))
                .callRealArgusExtractProcessor()
                .argusToRefocusConfigurationWithTransformsAndRepeatInterval(new Transform[]{testedPollingTransform}, 1000)
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



    @Test
    public void testPollingTransformTimeout() throws Exception {
        // ARRANGE
        MetricResponse response = new MetricResponseBuilder()
                .withMetric("argus-metric")
                .build();

        // determine the number of retries
        final int numberOfRetries = 5;
        final long timeoutMs = 50;

        // only run once
        fixtures.appConfigMocks()
                .runOnce();

        // init the polling transform and have it respond after the third try
        TestPollingTransform testedPollingTransform = spy(new TestPollingTransform(numberOfRetries, timeoutMs));

        fixtures.argusClientReturns(Collections.singletonList(response))
                .callRealArgusExtractProcessor()
                .argusToRefocusConfigurationWithTransformsAndRepeatInterval(new Transform[]{testedPollingTransform}, 1000)
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();

        // ensure the transform times out
        testedPollingTransform.setTimeout(timeoutMs);


        // ACT
        manager.run();

        // shutdown
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        verify(testedPollingTransform, times(1)).sendRequest(any());
        verify(testedPollingTransform, atLeast(1)).retrieveResult(any());
        verify(fixtures.taskManager()).onError(ArgumentMatchers.any(TimeoutException.class));
    }



    /**
     * {@link PollingTransform} implementation, used for testing
     */
    private static class TestPollingTransform extends PollingTransform<String> {
        private List<List<TransformationResult>> results;
        private final AtomicInteger countDown;

        /**
         * Initialize the number of retries after which the retrieve method will return a valid response
         */
        private TestPollingTransform(int retryThisManyTimes, long initialDelay) {
            this.countDown = new AtomicInteger(retryThisManyTimes);
            this.initialDelayMillis = initialDelay;
            this.backoffIntervalMillis = 10L;
        }

        private TestPollingTransform setTimeout(long timeout) {
            this.timeoutMillis = timeout;
            return this;
        }

        @Override
        public String sendRequest(List<List<TransformationResult>> input) {
            results = input;
            return "id";
        }

        @Override
        public List<List<TransformationResult>> retrieveResult(String request) {
            // return null until condition is satisfied
            if (countDown.getAndDecrement() > 0) {
                return null;
            }

            return results;
        }
    }
}