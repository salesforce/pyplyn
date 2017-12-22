/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.load.argus;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;
import static java.lang.String.format;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.argus.ArgusClient;
import com.salesforce.argus.model.ImmutableMetricResponse;
import com.salesforce.argus.model.MetricCollectionResponse;
import com.salesforce.argus.model.MetricResponse;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.connector.AppConnectors;
import com.salesforce.pyplyn.model.Transmutation;
import com.salesforce.pyplyn.processor.AbstractMeteredLoadProcessor;

/**
 * Pushes {@link com.salesforce.argus.model.MetricResponse}s into Argus
 * <p/>Annotated as Singleton as there should only be one instance of this class in operation.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.2.0
 */
@Singleton
public class ArgusLoadProcessor extends AbstractMeteredLoadProcessor<Argus> {
    private static final Logger logger = LoggerFactory.getLogger(ArgusLoadProcessor.class);

    private final AppConnectors appConnectors;
    private final ShutdownHook shutdownHook;

    @Inject
    public ArgusLoadProcessor(AppConnectors appConnectors, ShutdownHook shutdownHook) {
        this.appConnectors = appConnectors;
        this.shutdownHook = shutdownHook;
    }

    /**
     * Posts the data as Argus datapoints on the specified endpoint
     *
     * @return Empty list if nothing was processed
     */
    @Override
    public List<Boolean> process(final List<Transmutation> data, List<Argus> destinations) {
        // if data is empty, stop here as there is nothing to do
        if (data.isEmpty()) {
            return Collections.emptyList();
        }

        boolean allUpserted = destinations.stream()
                // group by endpoint
                .collect(Collectors.groupingBy(Argus::endpoint))

                // process each endpoint individually
                .entrySet().parallelStream()

                .map(destinationEntry -> {
                    // retrieve client endpoint
                    String endpointId = destinationEntry.getKey();
                    final List<Argus> loadDestinations = destinationEntry.getValue();

                    // retrieve the Argus client for the specified endpoint
                    AppConnectors.ClientAndCache<ArgusClient, MetricResponse> cc = appConnectors.retrieveOrBuildClient(endpointId, ArgusClient.class, MetricResponse.class);
                    final ArgusClient client = cc.client();

                    // TODO: move this someplace better
                    try {
                        client.authenticate();

                    } catch (UnauthorizedException e) {
                        // log auth failure if this exception type was thrown
                        authenticationFailure();

                        // stop here if we could not authenticate
                        logger.warn("", e);
                        return null;
                    }

                    // for all expressions belonging to this client
                    List<MetricResponse> metricsToPublish = loadDestinations.stream()
                            .map(loadDestination -> {
                                final String scope = loadDestination.scope();

                                return data.stream()
                                        // map data by name
                                        .collect(Collectors.groupingBy(Transmutation::name))
                                        .entrySet().stream()

                                        // not interested in results with no datapoints
                                        .filter(e -> !e.getValue().isEmpty())

                                        // map all datapoints for the same metric and return
                                        .map(result -> {
                                            final String name = result.getKey();
                                            final List<Transmutation> results = result.getValue();
                                            final Transmutation.Metadata metadata = Iterables.getLast(results).metadata();

                                            Map<String, String> datapoints = results.stream()
                                                    .collect(Collectors.toMap(
                                                            t -> Long.toString(t.time().toInstant().toEpochMilli()),
                                                            t -> formatNumber(t.value())));

                                            return ImmutableMetricResponse.builder()
                                                    .scope(scope)
                                                    .metric(name)
                                                    .putAllTags(metadata.tags())
                                                    .putAllTags(loadDestination.tags())
                                                    .putAllDatapoints(datapoints)
                                                    .build();

                                        });
                            })

                            // flatten stream and collect to list of samples that need to be processed by this endpoint
                            .flatMap(s -> s)
                            .collect(Collectors.toList());

                    // if shutting down, do not post to the Argus endpoint
                    if (shutdownHook.isShutdown()) {
                        return null;
                    }

                    // send expressions to the Argus endpoint
                    try (Timer.Context context = systemStatus.timer(meterName(), "collect-metrics." + endpointId).time()) {
                        MetricCollectionResponse response = client.postMetrics(metricsToPublish);
                        logger.info("Argus metric collection response for {}: {}", metricsToPublish, response);
                        return Optional.ofNullable(response).orElse(null);

                        // return failure
                    } catch (UnauthorizedException e) {
                        logger.error("Could not complete request for {}; failed samples={}", endpointId, metricsToPublish);
                        return null;
                    }
                })

                // return true if all metrics are successfully stored into all endpoints
                .allMatch(Objects::nonNull);

        // log result of operation
        if (allUpserted) {
            succeeded();
        } else {
            failed();
        }

        // return final result
        return Collections.singletonList(allUpserted);
    }

    @Override
    public Class<Argus> filteredType() {
        return Argus.class;
    }

    @Override
    protected String meterName() {
        return "Argus";
    }
}
