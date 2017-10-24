/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.pyplyn.duct.etl.extract.trust1;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.cache.ConcurrentCacheMap;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.connector.AppConnectors;
import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.Transmutation;
import com.salesforce.pyplyn.processor.AbstractMeteredExtractProcessor;
import com.salesforce.status.Trust1Client;
import com.salesforce.status.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.salesforce.pyplyn.util.FormatUtils.parseUTCTime;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Extracts data from Trust1 endpoints
 * <p/>Annotated as Singleton as there should only be one instance of this class in operation.
 *
 * @author Mihai Bojin
 * @since 10.1.0
 */
@Singleton
public class Trust1ExtractProcessor extends AbstractMeteredExtractProcessor<Trust1> {
    private static final Logger logger = LoggerFactory.getLogger(Trust1ExtractProcessor.class);

    private final AppConnectors appConnectors;
    private final ShutdownHook shutdownHook;

    @Inject
    public Trust1ExtractProcessor(AppConnectors appConnectors, ShutdownHook shutdownHook) {
        this.appConnectors = appConnectors;
        this.shutdownHook = shutdownHook;
    }

    /**
     * @return a list of metrics returned by executing the passed Argus expressions
     */
    @Override
    public List<List<Transmutation>> process(List<Trust1> apiCalls) {
        // stream of metrics to be loaded from the endpoints or from cache
        return apiCalls.stream()
        .map(instance -> {
            // retrieve a InfluxDB client
            AppConnectors.ClientAndCache<Trust1Client, Instance> cc = appConnectors.retrieveOrBuildClient(instance.endpoint(), Trust1Client.class, Instance.class);
            final Trust1Client client = cc.client();
            final ConcurrentCacheMap<Instance> cache = cc.cache();

            // short circuit if app was shutdown
            if (shutdownHook.isShutdown()) {
                return null;
            }

            // if the queried instance is cached, return it
            Instance cachedInstance = cache.isCached(instance.instance());
            if (nonNull(cachedInstance)) {
                return mapDatapointsAsResults(singletonList(cachedInstance), instance);
            }

            // retrieve data
            try (Timer.Context context = systemStatus.timer(meterName(), "instancePreview." + instance.endpoint()).time()) {
                List<Instance> instances = client.instancePreview();

                // determine if the retrieval failed; stop here if that's the case
                if (isNull(instances)) {
                    failed();
                    return null;
                }

                // mark successful operation and continue processing
                succeeded();

                // log cache debugging data
                logger.info("{} instances loaded from endpoint {}", instances.size(), instance.endpoint());

                if (instances.isEmpty()) {
                    // log no-data events
                    logger.warn("No data for {}, endpoint {}", instance.instance(), instance.endpoint());
                    noData();
                    return null;
                }

                // cache all instances
                if (instance.cacheMillis() > 0) {
                    instances.forEach(row -> cache.cache(row, instance.cacheMillis()));
                }

                return mapDatapointsAsResults(instances, instance);
            }

        })

        // filter errors and return
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    }

    /**
     * Maps instance data returned by Trust1 as a {@link Transmutation} matrix
     */
    private List<Transmutation> mapDatapointsAsResults(List<Instance> results, final Trust1 query) {
        // retrieve the searched instance from the results object
        Optional<Instance> matchingInstance = results.stream().filter(instance -> Objects.equals(instance.key(), query.instance())).findAny();

        // no data
        if (!matchingInstance.isPresent()) {
            logger.warn("No data for {}, endpoint {}; instance not found", query.instance(), query.endpoint());
            noData();
            return null;
        }

        // retrieve the searched instance
        final Instance instance = matchingInstance.get();

        // current time
        String time = Long.toString(Instant.now().toEpochMilli());

        // tag datapoint with originating instance object
        Transmutation.Metadata metadata = ImmutableTransmutation.Metadata.builder()
                .source(instance)
                .addMessages("Release: " + instance.releaseNumber())
                .addMessages("Status: " + instance.status().name())
                .addMessages("Incidents: " + instance.incidents().size())
                .addMessages("Maintenances: " + instance.maintenances().size())
                .build();

        return singletonList(createResult(time, instance.status().codeVal(), metadata, query.instance(), query.endpoint()));

    }

    /**
     * Creates an extract result
     *
     * @param time time of data point
     * @param value value of data point
     * @param metadata Metadata to include
     * @param instanceName name of instance that the result points to
     * @return Null if either time or value could not be parsed
     */
    private Transmutation createResult(String time, Double value, Transmutation.Metadata metadata, String instanceName, String endpointId) {
        try {
            ZonedDateTime parsedTime = parseUTCTime(time);
            return ImmutableTransmutation.of(parsedTime, instanceName, value, value, metadata);

        } catch (DateTimeParseException e) {
            logger.warn("No data for {}, endpoint {}; invalid time or value: {}", instanceName, endpointId, e.getMessage());
            noData();
            return null;
        }
    }

    @Override
    public Class<Trust1> filteredType() {
        return Trust1.class;
    }

    @Override
    protected String meterName() {
        return "Trust1";
    }

}
