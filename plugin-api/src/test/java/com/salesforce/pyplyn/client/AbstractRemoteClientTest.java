/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

import com.salesforce.pyplyn.configuration.AbstractConnectorImpl;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AbstractRemoteClientTest {
    AbstractConnectorImpl connector;
    AbstractRemoteClientImpl client;

    @Mock
    AbstractRemoteClientImpl.RetroService svc;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        connector = spy(new AbstractConnectorImpl("connector"));
        doReturn("http://localhost:8080/").when(connector).endpoint();
        client = spy(new AbstractRemoteClientImpl(connector, AbstractRemoteClientImpl.RetroService.class, 10L, 10L, 10L)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE

        doReturn(svc).when(client).svc(); // FindBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT - IGNORE
    }


    @Test
    public void testExecuteAndRetrieveBodySuccess() throws Exception {
        // ARRANGE
        @SuppressWarnings("unchecked")
        Call<String> call = (Call<String>)mock(Call.class);
        doReturn(call).when(svc).get();

        Response<String> failure = createSuccessfulResponse();
        doReturn(failure).when(call).execute();

        // ACT
        String response = client.executeAndRetrieveBody(call, "failed");

        // ASSERT
        assertThat(response, containsString("OK"));
    }


    @Test(expectedExceptions = UnauthorizedException.class)
    public void testExecuteAndRetrieveBodyAndFailUnauthorized() throws Exception {
        // ARRANGE
        @SuppressWarnings("unchecked")
        Call<String> call = (Call<String>)mock(Call.class);
        doReturn(call).when(svc).get();

        Response<String> failure = createFailedResponse(401);
        doReturn(failure).when(call).execute();

        // ACT
        client.executeAndRetrieveBody(call, "failed");
    }


    @Test
    public void testExecuteAndRetrieveBodyAndFailOtherError() throws Exception {
        // ARRANGE
        @SuppressWarnings("unchecked")
        Call<String> call = (Call<String>)mock(Call.class);
        doReturn(call).when(svc).get();

        Response<String> failure = createFailedResponse(500);
        doReturn(failure).when(call).execute();

        // ACT
        String response = client.executeAndRetrieveBody(call, "failed");

        // ASSERT
        assertThat(response, containsString("failed"));
    }


    @Test
    public void testExecuteAndRetrieveBodyCannotReadBody() throws Exception {
        // ARRANGE
        @SuppressWarnings("unchecked")
        Call<String> call = (Call<String>)mock(Call.class);
        doReturn(call).when(svc).get();

        Response<String> failure = createFailedUnreadableResponse(500);
        doReturn(failure).when(call).execute();

        // ACT
        String response = client.executeAndRetrieveBody(call, "failed");

        // ASSERT
        assertThat(response, containsString("failed"));
    }



    @Test
    public void testExecuteAndRetrieveBodyAndFailUnauthorizedCannotReadBody() throws Exception {
        // ARRANGE
        @SuppressWarnings("unchecked")
        Call<String> call = (Call<String>)mock(Call.class);
        doReturn(call).when(svc).get();

        Response<String> failure = createFailedUnreadableResponse(401);
        doReturn(failure).when(call).execute();

        // ACT
        try {
            client.executeAndRetrieveBody(call, "failed");
            fail("Expected this call to fail");

        } catch (UnauthorizedException e) {
            // ASSERT
            assertThat("Response should contain null for message and no error body", e.getMessage(), containsString("401/null"));
        }
    }

    @Test
    public void testExecuteAndRetrieveHeaders() throws Exception {
        // ARRANGE
        @SuppressWarnings("unchecked")
        Call<String> call = (Call<String>)mock(Call.class);
        doReturn(call).when(svc).get();

        Response<String> failure = createSuccessfulResponseWithHeaders();
        doReturn(failure).when(call).execute();

        // ACT
        Headers headers = client.executeAndRetrieveHeaders(call);

        // ASSERT
        assertThat(headers.size(), is(1));
        assertThat(headers.names(), hasItem("Custom"));
        assertThat(headers.get("Custom"), is("Header"));
    }

    @Test
    public void testClientWithProxy() throws Exception {
        // ARRANGE
        doReturn("127.0.0.1").when(connector).proxyHost();
        doReturn(8901).when(connector).proxyPort();

        // ACT
        client = new AbstractRemoteClientImpl(connector, AbstractRemoteClientImpl.RetroService.class, 10L, 10L, 10L); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
        Call<String> remoteCall = client.svc().get();

        try {
            remoteCall.execute();
            fail("This should not succeed! If it does, check any running processes that bind the specified port!");

        } catch (ConnectException e) {
            // ASSERT
            assertThat("Service was correctly initialized", client.svc(), notNullValue());
            assertThat("Attempting to execute the call, should result in a proxy connect failure", e.getMessage(), containsString("127.0.0.1:8901"));
        }
    }

    @Test
    public void testCacheKey() throws Exception {
        // ACT
        String cacheKey = client.cacheKey();

        // ASSERT
        assertThat("Cache key should be the connector id", cacheKey, equalTo("connector"));
    }

    /**
     * Creates a failure response with the specified code
     */
    private Response<String> createFailedResponse(int code) {
        ResponseBody failedResponse = ResponseBody.create(null, "fail");
        return Response.error(code, failedResponse);
    }

    /**
     * Creates a failure response that throws IOException when read
     */
    private Response<String> createFailedUnreadableResponse(int code) throws IOException {
        // ResponseBody.bytes() throws IOException if InputStream length larger than max int
        //  if this test starts failing, check that this implementation hasn't changed
        ResponseBody failedResponse = ResponseBody.create(null, Long.MAX_VALUE, mock(BufferedSource.class));
        return Response.error(code, failedResponse);
    }


    /**
     * Creates a success response
     */
    private Response<String> createSuccessfulResponse() {
        return Response.success("OK");
    }

    /**
     * Creates a success response
     */
    private Response<String> createSuccessfulResponseWithHeaders() {
        return Response.success("OK", Headers.of(Collections.singletonMap("Custom", "Header")));
    }
}