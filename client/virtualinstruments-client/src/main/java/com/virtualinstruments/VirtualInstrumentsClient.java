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
import com.virtualinstruments.model.ImmutableAuthRequest;
import com.virtualinstruments.model.ImmutableReportPayload;
import com.virtualinstruments.model.ReportPayload;
import com.virtualinstruments.model.ReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.nonNull;

/**
 * VirtualInstruments client implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.2.0
 */
public class VirtualInstrumentsClient extends AbstractRemoteClient<VirtualInstrumentsService> {
    private static final Logger logger = LoggerFactory.getLogger(VirtualInstrumentsClient.class);
    public static final String RESPONSE_SUCCESS = "Success";
    public static final String RESPONSE_FAILURE = "Failure";

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
        Response<Map<String, Object>> response = execute(svc().login(ImmutableAuthRequest.of(connector().username(), connector().password())));

        // if the auth call succeeds, retrieve and store the session id
        if (nonNull(response.body()) && Objects.equals(response.body().getOrDefault("status", RESPONSE_FAILURE), RESPONSE_SUCCESS)) {
            sessionId = response.headers().get("JSESSIONID");
            return true;
        }

        Map<String, Object> body = response.body();
        logger.error("Unsuccessful authentication call to {}; response={}", connector().endpoint(), body);

        return false;
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
        Response<Void> execute = execute(svc().reportBatch(report));

        // return the UUID if the call is successful, or null otherwise
        return Optional.ofNullable(execute).filter(Response::isSuccessful).map(r -> report.uuid()).orElse(null);
    }

    /**
     * @return a {@link ReportResponse} object representing the processed report or a failure
     */
    public ReportResponse pollReport(String uuid) throws UnauthorizedException {
        Preconditions.checkNotNull(uuid, "The report UUID should not be null!");
        return executeAndRetrieveBody(svc().reportPoll(uuid), null);
    }

    /**
     * Reset the known authentication credentials (session id)
     */
    @Override
    protected void resetAuth() {
        sessionId = null;
    }

}