/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AuthenticatedEndpointProviderTest {
    @Mock
    AbstractRemoteClient client;

    @Mock
    ClientFactory<AbstractRemoteClient> factory;

    @Mock
    AuthenticatedEndpointProvider<AbstractRemoteClient> provider;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        doReturn(factory).when(provider).factory();
        doCallRealMethod().when(provider).remoteClient(anyString());
    }

    @Test
    public void testWillAuthenticateClient() throws Exception {
        // ARRANGE
        client = mockUnauthenticatedClient("endpoint");

        // ACT
        AbstractRemoteClient actual = provider.remoteClient("endpoint");

        // ASSERT
        assertThat("The same client object should have been returned", actual, sameInstance(client));
        verify(client).auth();
    }


    @Test
    public void testSameClientShouldBeReturnedForSameEndpoint() throws Exception {
        // ARRANGE
        AbstractRemoteClient client1 = mockUnauthenticatedClient("client1");
        AbstractRemoteClient client2 = mockUnauthenticatedClient("client2");

        // ACT
        AbstractRemoteClient actual1 = provider.remoteClient("client1");
        AbstractRemoteClient actual2 = provider.remoteClient("client2");

        // ASSERT
        assertThat("Expecting a client object", actual1, sameInstance(client1));
        assertThat("Expecting a client object", actual2, sameInstance(client2));
        assertThat("Expecting the correct(same) client object to be returned", actual1, sameInstance(client1));
    }

    @Test
    public void testAuthenticationShouldHappenOnceOnSameClientObject() throws Exception {
        // ARRANGE
        client = mockUnauthenticatedClient("client");

        // init executor and two threads that will attempt to retrieve the same client
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AuthThread thread1 = new AuthThread(provider, "client");
        AuthThread thread2 = new AuthThread(provider, "client");

        // lock provider lock to prevent threads from starting independently to one another
        ReentrantLock startExecution = AuthenticatedEndpointProvider.authLock;
        startExecution.lock();

        // ACT
        try {
            // add threads for execution
            try {
                executor.execute(thread1);
                executor.execute(thread2);

                // do not accept any new tasks
                executor.shutdown();

            } finally {
                // unlock and allow threads to attempt to authenticate
                //   this is wrapped in a try-finally, to ensure the lock
                //   is property released in the case the executor throws a runtime exception
                startExecution.unlock();
            }

            // wait until executor finishes executing the two threads
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // ASSERT
            verify(client, times(1)).auth();
            assertThat("Thread should have obtained the same client", thread1.client, sameInstance(client));
            assertThat("Thread should have obtained the same client", thread2.client, sameInstance(client));
            assertThat("Client should be authenticated", client.isAuthenticated(), is(true));

        } finally {
            // ensure we shutdown the executor
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }


    @Test(expectedExceptions = ClientFactoryException.class)
    public void testFactoryFailure() throws Exception {
        // ARRANGE
        doThrow(new ClientFactoryException("Could not find client for endpoint", null)).when(factory).getClient(anyString());

        // ACT
        factory.getClient("endpoint");
    }


    @Test(expectedExceptions = UnauthorizedException.class)
    public void testCannotAuthenticateFailure() throws Exception {
        // ARRANGE
        client = mockClientThatCannotAuthenticate("endpoint");

        // ACT
        provider.remoteClient("endpoint");
    }


    /**
     * Thread that tries to authenticate the client
     */
    private static class AuthThread extends Thread {
        final AuthenticatedEndpointProvider<AbstractRemoteClient> provider;
        final String endpoint;
        AbstractRemoteClient client;

        public AuthThread(AuthenticatedEndpointProvider<AbstractRemoteClient> provider, String endpoint) {
            this.provider = provider;
            this.endpoint = endpoint;
        }

        @Override
        public void run() {
            try {
                client = provider.remoteClient(endpoint);

            } catch (UnauthorizedException e) {
                fail("This should not happen");
            }
        }
    }

    /**
     * Returns a mocked client that is not authenticated, but will be after retrieving it through
     * AuthenticatedEndpointProvider.remoteClient
     */
    private AbstractRemoteClient mockUnauthenticatedClient(String endpoint) throws UnauthorizedException {
        AbstractRemoteClient client = mock(AbstractRemoteClient.class);

        // holds the current authentication state
        final AtomicBoolean isAuthenticatedState = new AtomicBoolean(false);

        // returns the current authentication state
        doAnswer(answerIsAuthenticated(isAuthenticatedState)).when(client).isAuthenticated(); // FindBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT - IGNORE

        // authenticate, if auth() is called
        doAnswer((invocation) -> isAuthenticatedState.getAndSet(true)).when(client).auth();

        // allow the client to be returned by the factory's getClient(String) implementation
        doReturn(client).when(factory).getClient(endpoint);

        return client;
    }


    /**
     * Returns a mocked client that is not authenticated, but will be after retrieving it through
     * AuthenticatedEndpointProvider.remoteClient
     */
    private AbstractRemoteClient mockClientThatCannotAuthenticate(String endpoint) throws UnauthorizedException {
        AbstractRemoteClient client = mock(AbstractRemoteClient.class);

        // returns the current authentication state
        doReturn(Boolean.FALSE).when(client).isAuthenticated(); // FindBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT - IGNORE

        // authenticate, if auth() is called
        doThrow(new UnauthorizedException("Cannot authenticate")).when(client).auth();

        // allow the client to be returned by the factory's getClient(String) implementation
        doReturn(client).when(factory).getClient(endpoint);

        return client;
    }


    /**
     * Used to change the authentication state
     */
    private Answer answerIsAuthenticated(final AtomicBoolean isAuth) {
        return invocation -> isAuth.get();
    }
}