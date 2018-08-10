/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package io.pyplyn.influxdb;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

import java.nio.charset.Charset;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.ImmutableConnector;

import io.pyplyn.influxdb.model.ImmutablePoint;
import io.pyplyn.influxdb.model.ImmutableResults;
import io.pyplyn.influxdb.model.Point;
import io.pyplyn.influxdb.model.Results;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Response;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class InfluxDbClientTest {

    @Mock
    private InfluxDbService svc;

    @Mock
    private retrofit2.Call<Void> call;

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
                .id("influxdb")
                .endpoint("http://localhost:8086")
                .password("".getBytes())
                .build();
    }


    @Test
    public void testWritePoint() throws Exception {
        // ARRANGE

        // create a data point to write
        Point<Double> point = ImmutablePoint.<Double>builder()
                .measurement("cpu_load_short")
                .value(1.0d)
                .build();

        // init client
        InfluxDbClient client = spy(new InfluxDbClient(connector));
        doReturn(svc).when(client).svc();

        // init request captor
        ArgumentCaptor<RequestBody> requestCaptor = ArgumentCaptor.forClass(RequestBody.class);
        Buffer requestBuffer = new Buffer();

        // service returns data
        Results results = ImmutableResults.of(singletonList(ImmutableResults.Wrapper.builder().statementId(123).build()));
        doReturn(Response.success(results)).when(call).execute();
        doReturn(request).when(call).request();
        doReturn(call).when(svc).writePoints(anyString(), any());


        // ACT
        client.writePoint("velocity", point);


        // ASSERT
        // write
        verify(svc).writePoints(anyString(), requestCaptor.capture());

        // retrieve request buffer contents
        requestCaptor.getValue().writeTo(requestBuffer);
        String requestData = requestBuffer.readString(Charset.defaultCharset());

        assertThat(requestData, notNullValue());
        assertThat(requestData, containsString(point.toString()));
    }

    @Test
    public void testReadResults() throws Exception {
        // ARRANGE

        // init client
        InfluxDbClient client = spy(new InfluxDbClient(connector));
        doReturn(svc).when(client).svc();

        // service returns data
        Results results = ImmutableResults.of(singletonList(ImmutableResults.Wrapper.builder().statementId(123).build()));
        doReturn(Response.success(results)).when(call).execute();
        doReturn(request).when(call).request();
        doReturn(call).when(svc).query(anyString(), anyString());


        // ACT
        Results data = client.query("velocity", "SELECT * FROM \"cpu_load_short\"");


        // ASSERT
        // read
        verify(svc).query(anyString(), anyString());
        assertThat(data, notNullValue());
        assertThat(data.results(), notNullValue());
        assertThat(data.results(), hasSize(greaterThan(0)));
        assertThat(data.results().get(0).statementId(), equalTo(123));
    }
}