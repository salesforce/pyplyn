/*
 * Copyright, 1999-2018, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */
package com.salesforce.pyplyn.duct.etl.load.refocus;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codahale.metrics.Timer;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.systemstatus.SystemStatusRunnable;
import com.salesforce.refocus.RefocusClient;
import com.salesforce.refocus.model.ImmutableSample;
import com.salesforce.refocus.model.ImmutableUpsertResponse;
import com.salesforce.refocus.model.Sample;
import com.salesforce.refocus.model.UpsertResponse;


/**
 * RefocusBatcherTest
 *
 * @author Chris Coraggio &lt;chris.coraggio@salesforce.com&gt;
 * @since 11.0.0
 */
public class RefocusBatcherTest {

    @Mock
    private RefocusClient client;
    @Mock
    private ShutdownHook shutdownHook;
    @Mock
    private SystemStatusRunnable systemStatus;
    @Mock
    Timer timer;
    @Mock
    Timer.Context context;

    private RefocusBatcher rebatcher;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // create a list of 10 identical samples

        doReturn(timer).when(systemStatus).timer(any(), any());
        doReturn(context).when(timer).time();

        doReturn(true).when(client).isAuthenticated();

        rebatcher = spy(new RefocusBatcher(client, "endpoint-1", systemStatus, shutdownHook));
        rebatcher.enqueue(getSampleList(10));
    }

    @Test
    public void sendSamples() throws Exception {
        // ARRANGE
        UpsertResponse response = ImmutableUpsertResponse.of("OK", 0);
        doReturn(response).when(client).upsertSamplesBulk(any());
        doReturn(false).when(shutdownHook).isShutdown();

        // ACT
        rebatcher.run();

        // ASSERT
        verify(client, times(1)).upsertSamplesBulk(any());
    }

    @Test
    public void handleShutdown() throws UnauthorizedException {
        // ARRANGE
        doReturn(true).when(shutdownHook).isShutdown();

        // ACT
        rebatcher.run();

        // ASSERT
        verify(client, times(0)).upsertSamplesBulk(any());
    }

    @Test
    public void handleAuthError() throws UnauthorizedException {
        // ARRANGE
        doThrow(UnauthorizedException.class).when(client).authenticate();

        // ACT
        rebatcher.run();

        // ASSERT
        verify(client, times(0)).upsertSamplesBulk(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void sendMoreSamplesThanBatchSize() throws UnauthorizedException{
        // ARRANGE
        UpsertResponse response = ImmutableUpsertResponse.of("OK", 0);
        doReturn(response).when(client).upsertSamplesBulk(any());

        // ensure that the run() method only runs once
        doReturn(false).doReturn(true).when(shutdownHook).isShutdown();

        rebatcher.enqueue(getSampleList(1000));

        // ACT
        rebatcher.run();

        // ASSERT
        ArgumentCaptor<List<Sample>> captor = ArgumentCaptor.forClass(List.class);

        verify(client, times(2)).upsertSamplesBulk(captor.capture());

        List<List<Sample>> upsertedBatches = captor.getAllValues();

        assertThat(upsertedBatches, not(nullValue()));
        assertThat("Should have been split into two batches.", upsertedBatches.size(), is(2));

        // NOTE: There are 10 batches enqueued in the setup method and another 1000 here so the total is 1010.
        assertThat("Initial batch should have been 1000.", upsertedBatches.get(0).size(), is(1000));
        assertThat("Second batch should have been 10.", upsertedBatches.get(1).size(), is(10));

    }

    public List<Sample> getSampleList(int size){
        Sample sample = ImmutableSample.builder().id("id").name("name").value("value").build();
        List<Sample> samples = new ArrayList<>(size);
        for(int i = 0; i < size; i++){
            samples.add(sample);
        }
        return samples;
    }
}
