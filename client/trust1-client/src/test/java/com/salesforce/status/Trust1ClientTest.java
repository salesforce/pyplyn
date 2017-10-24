/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.status;

import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.ImmutableConnector;
import com.salesforce.status.model.ImmutableInstance;
import com.salesforce.status.model.Instance;
import okhttp3.Request;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class Trust1ClientTest {

    @Mock
    private Trust1Service svc;

    @Mock
    private Call<List<Instance>> call;

    private Request request;

    private Connector connector;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // build a dummy request object
        Request.Builder builder = new Request.Builder();
        builder.url("http://tests/");
        builder.method("MOCK", null);
        request = builder.build();

        connector = ImmutableConnector.builder()
                .id("trust1")
                .endpoint("http://localhost:9999")
                .password("".getBytes())
                .build();
    }


    @Test
    public void testInstancePreview() throws Exception {
        // ARRANGE
        Trust1Client client = spy(new Trust1Client(connector));
        doReturn(svc).when(client).svc();

        // service returns data
        Instance results = ImmutableInstance.builder()
                .key("key")
                .location("location")
                .environment("environment")
                .releaseVersion("releaseVersion")
                .releaseNumber("releaseNumber")
                .status(Instance.Status.OK)
                .isActive(true)
                .build();

        doReturn(Response.success(singletonList(results))).when(call).execute();
        doReturn(request).when(call).request();
        doReturn(call).when(svc).instancePreview(any(), anyBoolean(), any());

        // ACT
        List<Instance> instances = client.instancePreview();

        // ASSERT
        assertThat(instances, notNullValue());
        assertThat(instances, hasSize(1));
        assertThat(instances, contains(results));

    }

    @Test
    public void testAuthentication() throws Exception {
        // ARRANGE
        Trust1Client client = new Trust1Client(connector);

        // ACT
        boolean authenticated = client.isAuthenticated();
        boolean authResult = client.auth();
        client.resetAuth();
        boolean authenticatedAfterReset = client.isAuthenticated();

        // ASSERT
        assertThat(authenticated, equalTo(true));
        assertThat(authResult, equalTo(true));
        assertThat(authenticatedAfterReset, equalTo(true));
    }

}