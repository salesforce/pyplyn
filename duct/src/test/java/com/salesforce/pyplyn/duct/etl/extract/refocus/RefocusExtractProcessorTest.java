/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.refocus;

import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.MetricDuct;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.status.MeterType;
import com.salesforce.refocus.model.Sample;
import com.salesforce.refocus.model.builder.SampleBuilder;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class RefocusExtractProcessorTest {
    AppBootstrapFixtures fixtures;

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

        fixtures.oneRefocusToRefocusConfiguration()
                .refocusClientCanNotAuth()
                .callRealRefocusExtractProcessor()
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        verify(fixtures.systemStatus(), times(1)).meter("Refocus", MeterType.ExtractFailure);
    }

    @Test
    public void testDefaultValueProvidedOnTimeout() throws Exception {
        // ARRANGE
        Sample timedOutValue = new SampleBuilder()
                .withName("subject|aspect")
                .withUpdatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .withValue(RefocusExtractProcessor.RESPONSE_TIMEOUT)
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneRefocusToRefocusConfigurationWithDefaultValue(1.2d)
                .callRealRefocusExtractProcessor()
                .refocusClientReturns(Collections.singletonList(timedOutValue))
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        verify(fixtures.systemStatus(), times(0)).meter("Refocus", MeterType.ExtractFailure);
        verify(fixtures.systemStatus(), times(0)).meter("Refocus", MeterType.ExtractNoDataReturned);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TransformationResult>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixtures.refocusLoadProcessor()).execute(dataCaptor.capture(), any());

        List<TransformationResult> data = dataCaptor.getValue();
        assertThat(data, hasSize(1));

        TransformationResult result = data.get(0);
        assertThat(result.name(), equalTo("subject|aspect"));
        assertThat(result.value(), equalTo(1.2d));
        assertThat(result.originalValue(), equalTo(1.2d));
        assertThat(result.metadata().messages(), hasItem(containsString("Default value")));
    }

    @Test
    public void testSamplesAreCached() throws Exception {
        // ARRANGE
        // create a sample
        Sample sample = new SampleBuilder()
                .withName("subject|aspect")
                .withUpdatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .withValue("1.2")
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneRefocusToRefocusConfigurationWithCache()
                .realSampleCache()
                .callRealRefocusExtractProcessor()
                .refocusClientReturns(Collections.singletonList(sample))
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();
        app.run();

        // ACT
        app.run();

        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        verify(fixtures.systemStatus(), times(2)).meter("Refocus", MeterType.ExtractSuccess);
        verify(fixtures.sampleCache(), times(2)).isCached("subject|aspect");
        verify(fixtures.sampleCache(), times(1)).cache(any(), anyLong());
    }

    @Test
    public void testTimedOutSamplesAreNotCached() throws Exception {
        // ARRANGE
        // create a sample
        Sample sample = new SampleBuilder()
                .withName("subject|aspect")
                .withUpdatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .withValue(RefocusExtractProcessor.RESPONSE_TIMEOUT)
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneRefocusToRefocusConfigurationWithCache()
                .realSampleCache()
                .callRealRefocusExtractProcessor()
                .refocusClientReturns(Collections.singletonList(sample))
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        verify(fixtures.sampleCache(), times(1)).isCached("subject|aspect");
        verify(fixtures.sampleCache(), times(0)).cache(any(), anyLong());
    }

    @Test
    public void testProcessShouldFailWhenDataInvalid() throws Exception {
        // create a sample
        Sample badSample = new SampleBuilder()
                .withName("subject|aspect")
                .withUpdatedAt("INVALID_DATE")
                .build();

        assertFailureWithSample(badSample);
    }


    @Test
    public void testProcessShouldFailWithTimedOutSample() throws Exception {
        // create a sample
        Sample badSample = new SampleBuilder()
                .withName("subject|aspect")
                .withUpdatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .withValue(RefocusExtractProcessor.RESPONSE_TIMEOUT)
                .build();

        assertFailureWithSample(badSample);
    }

    @Test
    public void testProcessShouldFailWithInvalidSampleValue() throws Exception {
        // create a sample
        Sample badSample = new SampleBuilder()
                .withName("subject|aspect")
                .withUpdatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .withValue("INVALID")
                .build();

        assertFailureWithSample(badSample);
    }

    @Test
    public void testProcessShouldFailWhenGetSampleThrowsException() throws Exception {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneRefocusToRefocusConfiguration()
                .callRealRefocusExtractProcessor()
                .refocusClientThrowsExceptionOnGetSample()
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        verify(fixtures.systemStatus(), times(1)).meter("Refocus", MeterType.ExtractFailure);
    }

    /**
     * Executes a test that assumes a failure when a bad sample is returned from the Endpoint
     */
    private void assertFailureWithSample(Sample badSample) throws UnauthorizedException {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneRefocusToRefocusConfiguration()
                .callRealRefocusExtractProcessor()
                .refocusClientReturns(Collections.singletonList(badSample))
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        verify(fixtures.systemStatus(), times(1)).meter("Refocus", MeterType.ExtractFailure);
        verify(fixtures.systemStatus(), times(1)).meter("Refocus", MeterType.ExtractNoDataReturned);
    }
}