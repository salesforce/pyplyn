/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Collections;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.ImmutableConnector;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AbstractRemoteClientTest {
    Connector connector;
    AbstractRemoteClientImpl client;
    Call<String> call;

    @Mock
    AbstractRemoteClientImpl.RetroService svc;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        connector = ImmutableConnector.builder()
            .id("connector")
            .endpoint("http://localhost:8080/")
            .password("".getBytes())
            .build();
        client = spy(new AbstractRemoteClientImpl(this.connector, AbstractRemoteClientImpl.RetroService.class));

        doReturn(svc).when(client).svc();

        // build a dummy request object
        Request.Builder builder = new Request.Builder();
        builder.url("http://tests/");
        builder.method("MOCK", null);
        Request request = builder.build();

        // prepare a call mock
        @SuppressWarnings("unchecked")
        Call<String> call = mock(Call.class);
        doReturn(call).when(svc).get();
        doReturn(request).when(call).request();
        this.call = call;
    }


    @Test
    public void testExecuteAndRetrieveBodySuccess() throws Exception {
        // ARRANGE
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
        Response<String> failure = createFailedResponse(401);
        doReturn(failure).when(call).execute();
        doReturn(call).when(call).clone();

        // ACT
        client.executeAndRetrieveBody(call, "failed");
    }


    @Test
    public void testExecuteAndRetrieveBodyAndFailOtherError() throws Exception {
        // ARRANGE
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
        Response<String> failure = createFailedUnreadableResponse(401);
        doReturn(failure).when(call).execute();
        doReturn(call).when(call).clone();

        // ACT
        try {
            client.executeAndRetrieveBody(call, "failed");
            fail("Expected this call to fail");

        } catch (UnauthorizedException e) {
            // ASSERT
            assertThat("Response should contain null for message and no error body", e.getMessage(), containsString("401/Response.error()"));
        }
    }

    @Test
    public void testExecuteAndRetrieveHeaders() throws Exception {
        // ARRANGE
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
        connector = ImmutableConnector.builder().from(connector)
                .proxyHost("127.0.0.1")
                .proxyPort(8901)
                .build();

        // ACT
        client = new AbstractRemoteClientImpl(connector, AbstractRemoteClientImpl.RetroService.class);
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
    public void testClientWithMutualAuthentication() throws Exception {
        Path tempFile = Files.createTempFile("keystore", ".jks");
        try {
            // ARRANGE
            final String keystorePassword = "password";

            connector = ImmutableConnector.builder().from(connector)
                    .keystorePath(tempFile.toString())
                    .keystorePassword(keystorePassword.getBytes(Charset.defaultCharset()))
                    .build();

            // create a keystore
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null, keystorePassword.toCharArray());

            // export the keystore to the temp file
            OutputStream os = Files.newOutputStream(tempFile);
            keystore.store(os, keystorePassword.toCharArray());
            os.close();

            // ACT
            client = new AbstractRemoteClientImpl(connector, AbstractRemoteClientImpl.RetroService.class);

        } finally {
            Files.delete(tempFile);
        }
    }

    @Test
    public void testCannotInitializeClientWithInvalidTruststore() throws Exception {
        try {
            // ARRANGE
            connector = ImmutableConnector.builder().from(connector)
                    .keystorePath("/invalid/path")
                    .keystorePassword("password".getBytes(Charset.defaultCharset()))
                    .build();

            // ACT
            client = new AbstractRemoteClientImpl(connector, AbstractRemoteClientImpl.RetroService.class);
            fail("Expected initialization to fail!");

        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Unexpected exception configuring mutual authentication"));
            assertThat(e.getMessage(), containsString(connector.id()));
        }
    }

    @Test
    public void testCacheKey() throws Exception {
        // ACT
        String cacheKey = client.endpoint();

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