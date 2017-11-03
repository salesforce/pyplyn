/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.pyplyn.duct.etl.extract.virtualinstruments;

import static com.salesforce.pyplyn.util.FormatUtils.cleanMeasurementName;
import static io.reactivex.Flowable.defer;
import static java.util.Objects.isNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.virtualinstruments.model.ImmutableReportResponse;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.cache.Cacheable;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.connector.AppConnectors;
import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.Transmutation;
import com.salesforce.pyplyn.processor.AbstractMeteredExtractProcessor;
import com.virtualinstruments.VirtualInstrumentsClient;
import com.virtualinstruments.model.ReportResponse;

import io.reactivex.Flowable;

/**
 * Extracts data from VirtualInstruments endpoints
 * <p/>Annotated as Singleton as there should only be one instance of this class in operation.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.2.0
 */
@Singleton
public class VirtualInstrumentsExtractProcessor extends AbstractMeteredExtractProcessor<VirtualInstruments> {
    private static final Logger logger = LoggerFactory.getLogger(VirtualInstrumentsExtractProcessor.class);

    private final AppConnectors appConnectors;
    private final ShutdownHook shutdownHook;

    @Inject
    public VirtualInstrumentsExtractProcessor(AppConnectors appConnectors, ShutdownHook shutdownHook) {
        this.appConnectors = appConnectors;
        this.shutdownHook = shutdownHook;
    }

    /**
     * @return a list of metrics returned by executing the passed Argus expressions
     */
    @Override
    public List<List<Transmutation>> process(List<VirtualInstruments> reportRequests) {
        // stream of metrics to be loaded from the endpoints or from cache
        return reportRequests.stream()
                .map(reportParameters -> {
                    // retrieve a VirtualInstruments client
                    AppConnectors.ClientAndCache<VirtualInstrumentsClient, Cacheable> cc = appConnectors.retrieveOrBuildClient(reportParameters.endpoint(), VirtualInstrumentsClient.class, Cacheable.class);
                    final VirtualInstrumentsClient client = cc.client();

                    try {
                        client.authenticate();

                    } catch (UnauthorizedException e) {
                        // log auth failure if this exception type was thrown
                        authenticationFailure();
                        failed();

                        // stop here if we cannot authenticate
                        logger.warn("", e);
                        return null;
                    }

                    // short circuit if app was shutdown
                    if (shutdownHook.isShutdown()) {
                        return null;
                    }


                    // retrieve data
                    try (Timer.Context context = systemStatus.timer(meterName(), "submitReport." + reportParameters.endpoint()).time()) {
                        String uuid = client.submitReport(reportParameters.startTime(), reportParameters.endTime(), reportParameters.entityType(), reportParameters.metricName());

                        // determine if the batch request failed; stop here if that's the case
                        if (isNull(uuid)) {
                            failed();
                            return null;
                        }

                        // attempt to load the result
                        ReportResponse reportResponse = null;
                        try {
                            reportResponse = pollReport(client, uuid, reportParameters.pollingIntervalMillis(), 1)
                                    // time out after the specified number of ms
                                    .timeout(reportParameters.pollingTimeoutMillis(), TimeUnit.MILLISECONDS)
                                    .doOnError(e -> logger.warn("Exception when retrieving report metrics: {}", e.getMessage()))
                                    .onErrorResumeNext(defer(() -> Flowable.just(ImmutableReportResponse.of(
                                                    "ERROR",
                                            ImmutableReportResponse.Result.builder()
                                                    .uuid("")
                                                    .finished(false)
                                                    .build(),
                                            false)))
                                    )
                                    .blockingFirst();


                            // mark successful operation and continue processing
                            succeeded();

                            // retrieve data points
                            List<ReportResponse.ChartData> chartData = Iterables.getOnlyElement(reportResponse.result().charts()).chartData();

                            // log cache debugging data
                            logger.info("{} metrics loaded from endpoint {}", chartData.size(), reportParameters.endpoint());

                            return mapDatapointsAsResults(chartData, reportParameters);

                        } catch (NoSuchElementException | IllegalArgumentException e) {
                            logger.error("Could not complete request for {}; failed to retrieve chart data for {}; response was {}; exception message {}",
                                    reportParameters.endpoint(), reportParameters.metricName(), reportResponse, e.getMessage());
                            failed();
                            return null;
                        }

                    } catch (UnauthorizedException e) {
                        logger.error("Could not complete request for {}; failed to authorize and execute query={}; due to {}", reportParameters.endpoint(), reportParameters.metricName(), e.getMessage());
                        failed();

                        return null;
                    }

                })

                // filter out errors
                .filter(Objects::nonNull)

                // merge into one matrix
                .flatMap(Collection::stream)

                // and return
                .collect(Collectors.toList());
    }

    /**
     * Attempts to load the results by polling, gradually increasing the interval between requests.
     * <p/>
     * <p/> The attempts are triggered using the following formula:
     * <p/> TICK(cnt) = (cnt * (cnt+1) / 2) * interval
     */
    private Flowable<ReportResponse> pollReport(final VirtualInstrumentsClient client, final String uuid, long interval, int cnt) {
        return Flowable.timer(interval * cnt, TimeUnit.MILLISECONDS)
                // poll the endpoint for a response
                .map(time -> client.pollReport(uuid))

                // if the response is not finished, return null to generate an error downstream
                .map(response -> response.status().equals("OK")?response:null)
                .map(response -> response.result().finished()?response:null)

                // retry on errors
                .onErrorResumeNext(defer(() -> pollReport(client, uuid, interval, cnt + 1)).take(1));
    }

    /**
     * Maps datapoints returned by VirtualInstruments as a {@link Transmutation} matrix
     */
    private List<List<Transmutation>> mapDatapointsAsResults(List<ReportResponse.ChartData> chartData, final VirtualInstruments parameters) {

        // log no-data events
        if (chartData.isEmpty()) {
            logger.warn("No data for {}, endpoint {}", parameters.metricName(), parameters.endpoint());
            noData();
            return null;
        }

        return chartData.stream()
                .map(data -> data.data().stream()
                        .map(point -> {
                            ZonedDateTime time = Instant.ofEpochMilli(point[0].longValue()).atZone(ZoneOffset.UTC);
                            Number value = point[0];

                            // tag datapoint with originating statement
                            Transmutation.Metadata metadata = ImmutableTransmutation.Metadata.builder()
                                    .source(chartData)
                                    .build();

                            String entityName = cleanMeasurementName(data.entityName());
                            return createResult(time, value, metadata, entityName, parameters.endpoint());
                        })

                        .collect(Collectors.toList())).collect(Collectors.toList());

    }

    /**
     * Creates an extract result
     *
     * @param time time of data point
     * @param value value of data point
     * @param metadata Metadata to include
     * @param measurement name of data point
     * @return Null if the value could not be parsed
     */
    private Transmutation createResult(ZonedDateTime time, Number value, Transmutation.Metadata metadata, String measurement, String endpointId) {
        return ImmutableTransmutation.of(time, measurement, value, value, metadata);
    }

    @Override
    public Class<VirtualInstruments> filteredType() {
        return VirtualInstruments.class;
    }

    @Override
    protected String meterName() {
        return "VirtualInstruments";
    }

}
