/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.pyplyn.duct.etl.extract.influxdb;

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
import io.pyplyn.influxdb.InfluxDbClient;
import io.pyplyn.influxdb.model.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.salesforce.pyplyn.util.FormatUtils.parseNumber;
import static com.salesforce.pyplyn.util.FormatUtils.parseUTCTime;
import static java.util.Objects.isNull;

/**
 * Extracts data from InfluxDB endpoints
 * <p/>Annotated as Singleton as there should only be one instance of this class in operation.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.1.0
 */
@Singleton
public class InfluxDBExtractProcessor extends AbstractMeteredExtractProcessor<InfluxDB> {
    private static final Logger logger = LoggerFactory.getLogger(InfluxDBExtractProcessor.class);
    static final String TIME_PARAMETER = "time";
    public static final String VALUE_PARAMETER = "value";

    private final AppConnectors appConnectors;
    private final ShutdownHook shutdownHook;

    @Inject
    public InfluxDBExtractProcessor(AppConnectors appConnectors, ShutdownHook shutdownHook) {
        this.appConnectors = appConnectors;
        this.shutdownHook = shutdownHook;
    }

    /**
     * @return a list of metrics returned by executing the passed Argus expressions
     */
    @Override
    public List<List<Transmutation>> process(List<InfluxDB> queries) {
        // stream of metrics to be loaded from the endpoints or from cache
        return queries.stream()
        .map(query -> {
            // retrieve a InfluxDB client
            AppConnectors.ClientAndCache<InfluxDbClient, Cacheable> cc = appConnectors.retrieveOrBuildClient(query.endpoint(), InfluxDbClient.class, Cacheable.class);
            final InfluxDbClient client = cc.client();

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
            try (Timer.Context context = systemStatus.timer(meterName(), "query." + query.endpoint()).time()) {
                Results data = client.query(query.endpoint(), query.query());

                // determine if the retrieval failed; stop here if that's the case
                if (isNull(data)) {
                    failed();
                    return null;
                }

                // mark successful operation and continue processing
                succeeded();

                // log cache debugging data
                logger.info("{} measurements loaded from endpoint {}", data.results().size(), query.endpoint());

                if (data.results().isEmpty()) {
                    // log no-data events
                    logger.warn("No data for {}, endpoint {}", query.query(), query.endpoint());
                    noData();
                    return null;
                }

                return mapDatapointsAsResults(data, query);

            } catch (UnauthorizedException e) {
                logger.error("Could not complete request for {}; failed query={}; due to {}", query.endpoint(), query.query(), e.getMessage());
                failed();

                return null;
            }

        })

        // filter errors and return
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    }

    /**
     * Maps datapoints returned by InfluxDB as a {@link Transmutation} matrix
     */
    private List<Transmutation> mapDatapointsAsResults(Results results, final InfluxDB query) {
        // retrieve series columns and values
        List<Results.Statement> series = Iterables.getOnlyElement(results.results()).series();

        // no data
        if (series.isEmpty()) {
            logger.warn("No data for {}, endpoint {}; no results", query.query(), query.endpoint());
            noData();
            return null;
        }

        final Results.Statement statement = Iterables.getOnlyElement(series);

        // determine indexes of data columns
        int time = -1;
        int value = -1;
        for (int i = 0; i < statement.columns().size(); i++) {
            if (statement.columns().get(i).equals(TIME_PARAMETER)) {
                time = i;

            } else if (statement.columns().get(i).equals(VALUE_PARAMETER)) {
                value = i;
            }
        }

        // final indexes to use for identifying data
        final int idxTime = time;
        final int idxValue = value;

        // cannot retrieve time series data point
        if (time < 0 || value < 0) {
            return null;
        }


        return statement.values().stream()
                .map(row -> {
                    String timeString = row.get(idxTime).toString();
                    String valueString = row.get(idxValue).toString();

                    // tag datapoint with originating statement
                    Transmutation.Metadata metadata = ImmutableTransmutation.Metadata.builder()
                            .source(statement)
                            .build();

                    return createResult(timeString, valueString, metadata, query.name(), query.endpoint());
                })
                // filter any failures and collect
                .filter(Objects::nonNull)

                .collect(Collectors.toList());
    }

    /**
     * Creates an extract result
     *
     * @param time time of data point
     * @param value value of data point
     * @param metadata Metadata to include
     * @param measurement name of data point
     * @return Null if either time or value could not be parsed
     */
    private Transmutation createResult(String time, String value, Transmutation.Metadata metadata, String measurement, String endpointId) {
        try {
            ZonedDateTime parsedTime = parseUTCTime(time);
            Number parsedNumber = parseNumber(value);
            return ImmutableTransmutation.of(parsedTime, measurement, parsedNumber, parsedNumber, metadata);

        } catch (DateTimeParseException |ParseException e) {
            logger.warn("No data for {}, endpoint {}; invalid time or value: {}", measurement, endpointId, e.getMessage());
            noData();
            return null;
        }
    }

    @Override
    public Class<InfluxDB> filteredType() {
        return InfluxDB.class;
    }

    @Override
    protected String meterName() {
        return "InfluxDB";
    }

}
