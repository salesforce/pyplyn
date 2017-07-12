/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.pyplyn.duct.etl.extract.argus;

import com.codahale.metrics.Timer;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.argus.ArgusClient;
import com.salesforce.argus.model.MetricResponse;
import com.salesforce.pyplyn.cache.Cache;
import com.salesforce.pyplyn.cache.CacheFactory;
import com.salesforce.pyplyn.cache.ConcurrentCacheMap;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.connector.AppConnector;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;
import com.salesforce.pyplyn.processor.AbstractMeteredExtractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.salesforce.pyplyn.util.FormatUtils.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Extracts data from Argus endpoints
 * <p/>Annotated as Singleton as there should only be one instance of this class in operation.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class ArgusExtractProcessor extends AbstractMeteredExtractProcessor<Argus> {
    private static final Logger logger = LoggerFactory.getLogger(ArgusExtractProcessor.class);

    private final AppConnector appConnector;
    private final CacheFactory cacheFactory;
    private final ShutdownHook shutdownHook;

    private final Map<String, ArgusClient> endpoints = new ConcurrentHashMap<>();
    private final Map<ArgusClient, ConcurrentCacheMap<MetricResponse>> caches = new HashMap<>();

    @Inject
    public ArgusExtractProcessor(AppConnector appConnector, CacheFactory cacheFactory, ShutdownHook shutdownHook) {
        this.appConnector = appConnector;
        this.cacheFactory = cacheFactory;
        this.shutdownHook = shutdownHook;
    }

    /**
     * @return a list of metrics returned by executing the passed Argus expressions
     */
    @Override
    public List<List<TransformationResult>> process(List<Argus> data) {
        // prepare a map of the datapoints that can be cached
        final Map<String, Integer> cacheSettings = data.stream().filter(argus -> argus.cacheMillis() > 0).collect(Collectors.toMap(Argus::cacheKey, Argus::cacheMillis));

        // prepare a map of default values, in case no data is found for some of the expressions
        final Map<String, Double> defaultValueMap = data.stream().filter(argus -> nonNull(argus.defaultValue())).collect(Collectors.toMap(Argus::name, Argus::defaultValue));

        // stream of metrics to be loaded from the endpoints or from cache
        return data.stream()
                // separate each metric by endpoint
                .collect(Collectors.groupingBy(Argus::endpoint))

                // then process each endpoint in parallel
                .entrySet().parallelStream()

                // process expressions for each endpoint
                .map(endpointExpressions -> {
                    final String endpointId = endpointExpressions.getKey();

                    // retrieve Argus client and cache for the specified endpoint
                    final ArgusClient client = client(endpointId);
                    final Cache<MetricResponse> endpointCache = cache(client);

                    // stop here if we cannot initialize an endpoint for this client
                    if (isNull(client)) {
                        failed();
                        return null;
                    }

                    // first load the cached responses
                    final List<MetricResponse> cachedResponses = endpointExpressions.getValue().stream()
                            .map(s -> endpointCache.isCached(s.cacheKey()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());


                    // prepare Argus expressions as strings
                    List<String> expressions = endpointExpressions.getValue().stream()

                            // only processes expressions that aren't already cached
                            .filter(s -> isNull(endpointCache.isCached(s.cacheKey())))

                            // always alias the expression with the expected name,
                            //   in order to be able to identify it in the response
                            .map(ArgusExtractProcessor::aliasExpression)
                            .collect(Collectors.toList());

                    try {
                        // short circuit if app was shutdown
                        if (shutdownHook.isShutdown()) {
                            return null;
                        }

                        // retrieve metrics from Argus endpoint, only if we have expressions to retrieve
                        List<MetricResponse> metricResponses;
                        if (!expressions.isEmpty()) {
                            try (Timer.Context context = systemStatus.timer(meterName(), "get-metrics." + endpointId).time()) {
                                metricResponses = client.getMetrics(expressions);
                            }

                            // determine if the retrieval failed; stop here if that's the case
                            if (isNull(metricResponses)) {
                                failed();
                                return null;
                            }

                            // cache expressions that should be cached, based on their cacheMillis() settings mapped in canCache
                            metricResponses.stream()
                                    // we are not caching results with no data
                                    .filter(ArgusExtractProcessor::responseHasDatapoints)
                                    .forEach(result -> tryCache(result, client, cacheSettings));
                        } else {
                            metricResponses = Collections.emptyList();
                        }

                        // mark successful operation and continue processing
                        succeeded();

                        // log cache debugging data
                        logger.info("{} metrics loaded from cache, {} from endpoint {}",
                                cachedResponses.size(), metricResponses.size(), endpointId);

                        // check all metrics with noData and populate with defaults, if required
                        return Stream.concat(cachedResponses.stream(), metricResponses.stream())

                                // if there is missing data, add default datapoints
                                .map(result -> {
                                    // nothing to do if the response already has datapoints
                                    if (responseHasDatapoints(result)) {
                                        logger.info("Loaded data for {}, endpoint {}", result.metric(), endpointId);
                                        return mapDatapointsAsResults(result, endpointId);
                                    }

                                    // if the response does not have any datapoints and a default value was not specified
                                    Double defaultValue = defaultValueMap.get(result.metric());
                                    if (isNull(defaultValue)) {
                                        // log no-data events
                                        logger.warn("No data for {}, endpoint {}", result.metric(), endpointId);
                                        noData();

                                        // stop here, cannot create a TransformationResult from no points
                                        return null;
                                    }

                                    // creates a default datapoint, based on the specified defaultValueMap
                                    final Map.Entry<String, String> defaultMetricEntry = createDefaultDatapoint(defaultValue);

                                    // tags the result with a message, to denote that this is a default value and not extracted from the endpoint
                                    final String defaultValueMessage =
                                            generateDefaultValueMessage(result.metric(), defaultValue);

                                    // return the default value, tagged with
                                    return Optional.ofNullable(
                                            // attempt to create a result
                                            createResult(defaultMetricEntry.getKey(),
                                                    defaultMetricEntry.getValue(),
                                                    result.metric(),
                                                    endpointId))

                                            // add a default message
                                            .map(transResult -> {
                                                logger.info("Default data provided for {}={}, endpoint {}", result.metric(), transResult.value(), endpointId);
                                                return new TransformationResultBuilder(transResult)
                                                        .metadata((metadata) -> metadata
                                                                .addMessage(defaultValueMessage))
                                                        .build();
                                            })

                                            // and map to a list, which is the expected return type
                                            .map(Collections::singletonList)

                                            // or return an empty collection, for any failures
                                            .orElse(null);

                                })

                                // filter out any errors due to no-data or when creating the default response
                                .filter(Objects::nonNull)

                                .collect(Collectors.toList());

                        // catch any endpoint failures
                    } catch (UnauthorizedException e) {
                        logger.error("Could not complete request for {}; failed expressions={}; due to {}", endpointId, expressions, e.getMessage());
                        failed();
                    }

                    // if we end up here, it means something failed
                    return null;
                })

                // filter failures and return as List<MetricResponse>
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Maps datapoints returned by Argus as a {@link TransformationResult} matrix
     */
    private List<TransformationResult> mapDatapointsAsResults(MetricResponse metricResponse, String endpointId) {
        // store metric name, as it's the same for all data points
        final String metricName = metricResponse.metric();

        // map each datapoint to a TransformationResult object
        return metricResponse.datapoints().entrySet().stream()
                // create TransformResultStage object
                .map(metricEntry -> createResult(metricEntry.getKey(), metricEntry.getValue(), metricName, endpointId))

                // filter any failures and collect
                .filter(Objects::nonNull)

                // tag each datapoint with the originating MetricResponse object
                .map(result -> new TransformationResultBuilder(result)
                        // TODO: do this for Refocus too, and/or define global flag for skipping this step (non Gadget flows do not need it)
                        //       alternatively, either specify this flag per configuration (+1) or detect if it will be required (based on some annotation
                        //       implemented by Transforms - i.e.: @RequiresSourceObject
                        .metadata((metadata) -> metadata.addSource(metricResponse))
                        .build())

                .collect(Collectors.toList());
    }

    /**
     * Creates a datapoint map, containing a single result
     * <p/>Creates one point marked at the current time with the value specified in the defaultValueMap object
     *
     * @return returns a Map.Entry containing the time and value of the default point
     */
    private Map.Entry<String, String> createDefaultDatapoint(Double defaultValue) {
        SortedMap<String, String> defaultDatapoint = new TreeMap<>();

        // init time and value and add to map
        String time = Long.toString(Instant.now().toEpochMilli());
        String value = formatNumber(defaultValue);
        defaultDatapoint.put(time, value);

        return Iterables.getOnlyElement(defaultDatapoint.entrySet());
    }

    /**
     * Aliases a specified expression so the result can be identified (in a batch)
     *
     * @param argus Configuration parameter that holds the expression to load and its name
     */
    static String aliasExpression(Argus argus) {
        return String.format("ALIAS(%s,#%s#,#literal#)", argus.expression(), argus.name());
    }

    /**
     * Checks that returned metric response has datapoints
     * <p/>This method is used to determine if the result should be cached.
     */
    static boolean responseHasDatapoints(MetricResponse response) {
        return !response.datapoints().isEmpty();
    }

    /**
     * Creates an extract result
     *
     * @param time time of data point
     * @param value value of data point
     * @param metric name of data point
     * @return Null if either time or value could not be parsed
     */
    private TransformationResult createResult(String time, String value, String metric, String endpointId) {
        try {
            ZonedDateTime parsedTime = parseUTCTime(time);
            Number parsedNumber = parseNumber(value);
            return new TransformationResult(parsedTime, metric, parsedNumber, parsedNumber);

        } catch (DateTimeParseException |ParseException e) {
            logger.warn("No data for {}, endpoint {}; invalid time or value: {}", metric, endpointId, e.getMessage());
            noData();
            return null;
        }
    }

    /**
     * Attempts to cache getMetrics responses, if they are registered for caching
     *
     * @param metric MetricResponse to cache
     * @param howLongToCacheFor Cache duration map, per each cacheable metric key
     */
    private void tryCache(MetricResponse metric, ArgusClient client, Map<String, Integer> howLongToCacheFor) {
        if (howLongToCacheFor.containsKey(metric.cacheKey())) {
            // retrieves the MetricResponse cache object and caches the specified metric for the specified duration
            cache(client).cache(metric, howLongToCacheFor.get(metric.cacheKey()));
        }
    }


    /**
     * Returns a previously initialized client for the specified endpoint
     *   or initializes one and returns it
     */
    public ArgusClient client(String endpointId) {
        return endpoints.computeIfAbsent(endpointId, key -> {
            ArgusClient client = new ArgusClient(appConnector.get(endpointId));
            try {
                client.auth();

                // init cache and processing queue
                caches.computeIfAbsent(client, k -> cacheFactory.newCache());

                return client;

            } catch (UnauthorizedException e) {
                // log auth failure if this exception type was thrown
                authenticationFailure();

                logger.warn("", e);
                return null;
            }
        });
    }

    /**
     * @return a {@link Cache} for the corresponding {@link ArgusClient}
     * @throws IllegalStateException if a cache does not exist for this client
     */
    public Cache<MetricResponse> cache(ArgusClient client) {
        return Optional.ofNullable(caches.get(client)).orElseThrow(() ->
                new IllegalStateException("Cannot proceed with un-initialized cache for " + client.cacheKey()));
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
