/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.virtualinstruments;

import com.google.common.base.Preconditions;
import com.salesforce.pyplyn.client.AbstractRemoteClient;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.EndpointConnector;
import com.virtualinstruments.model.ImmutableReportPayload;
import com.virtualinstruments.model.ReportPayload;
import com.virtualinstruments.model.ReportResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * VirtualInstruments client implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.2.0
 */
public class VirtualInstrumentsClient extends AbstractRemoteClient<VirtualInstrumentsService> {
    private static final Logger logger = LoggerFactory.getLogger(VirtualInstrumentsClient.class);

    private volatile String sessionId;


    /**
     * Default simplified constructor that uses the specified connection defaults
     *
     * @param connector The InfluxDB API endpoint to use in calls
     */
    public VirtualInstrumentsClient(EndpointConnector connector) {
        super(connector, VirtualInstrumentsService.class);
    }

    /**
     * @return true if the current client is authenticated against its endpoint;
     *   defaults to true since this API does not support authentication
     */
    @Override
    public boolean isAuthenticated() {
        return nonNull(sessionId);
    }

    /**
     * Authenticate
     */
    @Override
    protected boolean auth() throws UnauthorizedException {
        // retrieve response
        String credentials = format("username=%s&password=%s", connector().username(), new String(connector().password(), Charset.defaultCharset()));
        RequestBody body = RequestBody.create(MediaType.parse(""), credentials);
        Response<ResponseBody> response = execute(svc().login(body));

        if (!response.isSuccessful()) {
            logFailedLogin(response, connector().endpoint());
            return false;
        }

        // since VI redirects us, we need to retrieve the JSESSIONID Cookie
        okhttp3.Response priorResponse = response.raw().priorResponse();
        if (isNull(priorResponse)) {
            logFailedLogin(response, connector().endpoint());
            return false;
        }

        String cookie = priorResponse.header("Set-Cookie");

        // session header was not set
        if (isNull(cookie)) {
            logFailedLogin(response, connector().endpoint());
            return false;
        }

        // store session id, getting rid of extraneous information
        this.sessionId = cookie.split(";")[0];

        return true;
    }

    /**
     * Log a failed login and its corresponding {@link Response}
     */
    static void logFailedLogin(Response<ResponseBody> response, String endpoint) {
        try {
            logger.error("Unsuccessful authentication call to {}; response={}", endpoint, response.body().string());

        } catch (IOException e) {
            logger.warn("Could not read response from VirtualInstruments endpoint {}", endpoint, e);
        }
    }

    /**
     * Submits a report for processing
     *
     * @return the generated UUID for the report, or null if the call was not successful
     */
    public String submitReport(Long startTime, Long endTime, String entityType, String metricName) throws UnauthorizedException {
        // initialize the query parameters
        ReportPayload.ChartQueryParam queryParams = ImmutableReportPayload.ChartQueryParam.builder()
                .entityType(entityType)
                .metricName(metricName)
                .build();

        // create a report object
        ReportPayload report = ImmutableReportPayload.builder()
                .startTimestamp(startTime)
                .endTimestamp(endTime)
                .addCharts(ImmutableReportPayload.Chart.builder().addChartQueryParams(queryParams).build())
                .build();

        // execute call
        Response<Void> execute = execute(svc().reportBatch(sessionId, report));

        // return the UUID if the call is successful, or null otherwise
        return Optional.ofNullable(execute).filter(Response::isSuccessful).map(r -> report.uuid()).orElse(null);
    }

    /**
     * @return a {@link ReportResponse} object representing the processed report or a failure
     */
    public ReportResponse pollReport(String uuid) throws UnauthorizedException {
        Preconditions.checkNotNull(uuid, "The report UUID should not be null!");
        return executeAndRetrieveBody(svc().reportPoll(sessionId, uuid), null);
    }

    /**
     * Reset the known authentication credentials (session id)
     */
    @Override
    protected void resetAuth() {
        sessionId = null;
    }

}