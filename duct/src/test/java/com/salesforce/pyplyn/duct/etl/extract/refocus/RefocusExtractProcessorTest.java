/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.refocus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationUpdateManager;
import com.salesforce.pyplyn.model.Transmutation;
import com.salesforce.pyplyn.status.MeterType;
import com.salesforce.refocus.model.ImmutableSample;
import com.salesforce.refocus.model.Sample;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class RefocusExtractProcessorTest {
    AppBootstrapFixtures fixtures;
    ArgumentCaptor<MeterType> meterTypeArgumentCaptor;

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

        fixtures.oneRefocusToRefocusConfiguration()
                .callRealRefocusExtractProcessor()
                .refocusClientCanNotAuth()
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();

        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure (ExtractFailure and AuthenticationFailure)
        verify(fixtures.systemStatus(), times(2)).meter(eq("Refocus"), meterTypeArgumentCaptor.capture());
        assert(meterTypeArgumentCaptor.getAllValues().stream().anyMatch(meterType -> meterType.processStatus().name().equals("ExtractFailure")));
    }

    @Test
    public void testDefaultValueProvidedOnTimeout() throws Exception {
        // ARRANGE
        Sample timedOutValue = ImmutableSample.builder()
                .name("subject|aspect")
                .updatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .value(RefocusExtractProcessor.RESPONSE_TIMEOUT)
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneRefocusToRefocusConfigurationWithDefaultValue(1.2d)
                .callRealRefocusExtractProcessor()
                .refocusClientReturns(Collections.singletonList(timedOutValue))
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        verify(fixtures.systemStatus(), times(1)).meter(eq("Refocus"), meterTypeArgumentCaptor.capture());
        assert(meterTypeArgumentCaptor.getValue().processStatus().name().equals("ExtractSuccess"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Transmutation>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixtures.refocusLoadProcessor()).executeAsync(dataCaptor.capture(), any());

        List<Transmutation> data = dataCaptor.getValue();
        assertThat(data, hasSize(1));

        Transmutation result = data.get(0);
        assertThat(result.name(), equalTo("subject|aspect"));
        assertThat(result.value(), equalTo(1.2d));
        assertThat(result.originalValue(), equalTo(1.2d));
        assertThat(result.metadata().messages(), hasItem(containsString("Default value")));
    }

    @Test
    public void testSamplesAreCached() throws Exception {
        // ARRANGE
        // create a sample
        Sample sample = ImmutableSample.builder()
                .name("subject|aspect")
                .updatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .value("1.2")
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneRefocusToRefocusConfigurationWithCache()
                .realSampleCache()
                .callRealRefocusExtractProcessor()
                .refocusClientReturns(Collections.singletonList(sample))
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
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        verify(fixtures.systemStatus(), times(2)).meter(eq("Refocus"), meterTypeArgumentCaptor.capture());
        for(MeterType m: meterTypeArgumentCaptor.getAllValues()){
            assert(m.processStatus().name().equals("ExtractSuccess"));
        }
        verify(fixtures.sampleCache(), times(2)).isCached("subject|aspect");
        verify(fixtures.sampleCache(), times(1)).cache(any(), anyLong());
    }

    @Test
    public void testTimedOutSamplesAreNotCached() throws Exception {
        // ARRANGE
        // create a sample
        Sample sample = ImmutableSample.builder()
                .name("subject|aspect")
                .updatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .value(RefocusExtractProcessor.RESPONSE_TIMEOUT)
                .build();

        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneRefocusToRefocusConfigurationWithCache()
                .realSampleCache()
                .callRealRefocusExtractProcessor()
                .refocusClientReturns(Collections.singletonList(sample))
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        verify(fixtures.sampleCache(), times(1)).isCached("subject|aspect");
        verify(fixtures.sampleCache(), times(0)).cache(any(), anyLong());
    }

    @Test
    public void testProcessShouldFailWhenDataInvalid() throws Exception {
        // create a sample
        Sample badSample = ImmutableSample.builder()
                .name("subject|aspect")
                .updatedAt("INVALID_DATE")
                .build();

        assertFailureWithSample(badSample);
    }


    @Test
    public void testProcessShouldFailWithTimedOutSample() throws Exception {
        // create a sample
        Sample badSample = ImmutableSample.builder()
                .name("subject|aspect")
                .updatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .value(RefocusExtractProcessor.RESPONSE_TIMEOUT)
                .build();

        assertFailureWithSample(badSample);
    }

    @Test
    public void testProcessShouldFailWithInvalidSampleValue() throws Exception {
        // create a sample
        Sample badSample = ImmutableSample.builder()
                .name("subject|aspect")
                .updatedAt(ZonedDateTime.now(ZoneOffset.UTC).toString())
                .value("INVALID")
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
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        verify(fixtures.systemStatus(), times(1)).meter(eq("Refocus"), meterTypeArgumentCaptor.capture());
        assert(meterTypeArgumentCaptor.getValue().processStatus().name().equals("ExtractFailure"));
    }

    /**
     * Executes a test that assumes a failure when a bad sample is returned from the Endpoint
     */
    private void assertFailureWithSample(Sample badSample) throws UnauthorizedException, InterruptedException {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneRefocusToRefocusConfiguration()
                .callRealRefocusExtractProcessor()
                .refocusClientReturns(Collections.singletonList(badSample))
                .initializeFixtures();

        // init app
        ConfigurationUpdateManager manager = fixtures.configurationManager();


        // ACT
        manager.run();
        fixtures.awaitUntilAllTasksHaveBeenProcessed(true);


        // ASSERT
        // since we had no real client, expecting RefocusExtractProcessor to have logged a failure
        // need one of each of ExtractFailure and ExtractNoDataReturned

        verify(fixtures.systemStatus(), times(2)).meter(eq("Refocus"), meterTypeArgumentCaptor.capture());
        List<String> processStatuses = new ArrayList<>();
        meterTypeArgumentCaptor.getAllValues().forEach((meterType) -> {
            processStatuses.add(meterType.processStatus().name());
        });
        assert(processStatuses.contains("ExtractFailure"));
        assert(processStatuses.contains("ExtractNoDataReturned"));
        assert(processStatuses.size() == 2);
    }
}