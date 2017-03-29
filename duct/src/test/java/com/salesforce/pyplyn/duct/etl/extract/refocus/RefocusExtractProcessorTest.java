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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public void testProcessShouldFailWhenDataInvalid() throws Exception {
        // create a sample
        Sample badSample = new SampleBuilder()
                .withName("null|null")
                .withUpdatedAt("INVALID_DATE")
                .build();

        assertFailureWithSample(badSample);
    }


    @Test
    public void testProcessShouldFailWithTimedOutSample() throws Exception {
        // create a sample
        Sample badSample = new SampleBuilder()
                .withName("null|null")
                .withUpdatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .withValue("Timeout")
                .build();

        assertFailureWithSample(badSample);
    }

    @Test
    public void testProcessShouldFailWithInvalidSampleValue() throws Exception {
        // create a sample
        Sample badSample = new SampleBuilder()
                .withName("null|null")
                .withUpdatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .withValue("INVALID")
                .build();

        assertFailureWithSample(badSample);
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