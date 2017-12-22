/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

import static java.util.Objects.nonNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.configuration.EndpointConnector;

public class AbstractRemoteClientConcurrencyTest {
    private ClientImpl client;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        EndpointConnector connector = mock(EndpointConnector.class);
        doReturn("http://localhost").when(connector).endpoint();

        client = spy(new ClientImpl(connector, ServiceImpl.class));
    }

    @Test
    public void concurrentOperationsDoNotTriggerExternalCallMultipleTimes() throws Exception {
        // ARRANGE
        final int QUEUE_SIZE = 100;
        final int CONCURRENCY = 4;

        ScheduledExecutorService svc = Executors.newScheduledThreadPool(CONCURRENCY);

        // pre-auth
        client.authenticate();


        // ACT
        for (int i = 1; i < QUEUE_SIZE; i++) {
            final boolean shouldReset = i == QUEUE_SIZE/2;
            svc.submit(() -> {
                try {
                    // reset auth
                    if (shouldReset) {
                        client.resetAuth();
                    }

                    client.authenticate();

                } catch (UnauthorizedException e) {
                    fail("Should not throw an exception during auth!");
                }
            });
        }

        svc.shutdown();
        svc.awaitTermination(5, TimeUnit.SECONDS);

        // ASSERT
        assertThat(client.isAuthenticated(), equalTo(true));
        verify(client, times(1)).resetAuth();
        verify(client, times(2)).makeExternalCall();
    }

    // mock
    private static class ClientImpl extends AbstractRemoteClient<ServiceImpl> {
        private volatile byte[] authToken;

        protected ClientImpl(EndpointConnector connector, Class<ServiceImpl> cls) {
            super(connector, cls);
        }

        @Override
        protected boolean isAuthenticated() {
            return nonNull(authToken);
        }

        @Override
        protected boolean auth() throws UnauthorizedException {
            makeExternalCall();
            authToken = new byte[]{2};
            return true;
        }

        public void makeExternalCall() {
            // nothing to do
        }

        @Override
        protected void resetAuth() {
            this.authToken = null;
        }
    }

    private interface ServiceImpl {

    }
}