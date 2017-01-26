/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.load.refocus;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.client.AuthenticatedEndpointProvider;
import com.salesforce.pyplyn.client.ClientFactory;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.providers.client.RemoteClientFactory;
import com.salesforce.pyplyn.model.ETLMetadata;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.processor.AbstractMeteredLoadProcessor;
import com.salesforce.refocus.RefocusClient;
import com.salesforce.refocus.model.Link;
import com.salesforce.refocus.model.Sample;
import com.salesforce.refocus.model.builder.SampleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;
import static java.util.Objects.isNull;

/**
 * Pushes data into Refocus
 * <p/>Annotated as Singleton as there should only be one instance of this class in operation.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class RefocusLoadProcessor extends AbstractMeteredLoadProcessor<Refocus> implements AuthenticatedEndpointProvider<RefocusClient> {
    private static final Logger logger = LoggerFactory.getLogger(RefocusLoadProcessor.class);

    private final RemoteClientFactory<RefocusClient> refocusClientFactory;
    private final ShutdownHook shutdownHook;


    @Inject
    public RefocusLoadProcessor(RemoteClientFactory<RefocusClient> refocusClientFactory, ShutdownHook shutdownHook) {
        this.refocusClientFactory = refocusClientFactory;
        this.shutdownHook = shutdownHook;
    }

    /**
     * Returns the corresponding endpoint by id
     * <p/>Authenticates the endpoint if necessary.
     *
     * @return null if any errors occurred
     */
    private RefocusClient getEndpoint(String endpointId) {
        try {
            return remoteClient(endpointId);

        } catch (UnauthorizedException e) {
            // log auth failure if this exception type was thrown
            authenticationFailure();

            logger.error("", e);
            return null;
        }
    }

    /**
     * Posts the data as Refocus samples on the specified endpoint
     *
     * @return Empty list if nothing was processed
     */
    @Override
    public List<Boolean> process(final List<TransformationResult> data, List<Refocus> destinations) {
        // if data is empty, stop here as there is nothing to do
        if (data.isEmpty()) {
            return Collections.emptyList();
        }

        boolean allUpserted = destinations.stream()
                // group by endpoint
                .collect(Collectors.groupingBy(Refocus::endpoint))

                // process each endpoint individually
                .entrySet().stream()

                .map(destinationEntry -> {
                    // retrieve client endpoint
                    String endpointId = destinationEntry.getKey();
                    final RefocusClient client = getEndpoint(endpointId);
                    if (isNull(client)) {
                        // stop here if we couldn't get a client
                        return Boolean.FALSE;
                    }

                    // for all expressions belonging to this client
                    List<Sample> allSamplesForEndpoint = destinationEntry.getValue().stream()
                            .map(expr -> {
                                final String sampleName = expr.name();
                                final List<Link> relatedLinks = expr.relatedLinks();
                                String defaultMessageCode = expr.defaultMessageCode();
                                String defaultMessageBody = expr.defaultMessageBody();

                                return data.stream().map(result -> {
                                    // create message code and body, based on previously defined values
                                    ETLMetadata metadata = result.metadata();
                                    String messageCodeString = Optional.ofNullable(metadata.messageCode()).orElse(defaultMessageCode);
                                    String messageBodyString = Optional.ofNullable(convertMessagesToString(metadata.messages())).orElse(defaultMessageBody);

                                    return new SampleBuilder()
                                            .withName(sampleName)
                                            .withValue(formatNumber(result.value()))
                                            .withRelatedLinks(relatedLinks)
                                            .withMessageCode(messageCodeString)
                                            .withMessageBody(messageBodyString)
                                            .build();
                                });
                            })

                            // flatten stream and collect to list of samples that need to be processed by this endpoint
                            .flatMap(s -> s)
                            .collect(Collectors.toList());

                    // if shutting down, do not post to the Refocus endpoint
                    if (shutdownHook.isShutdown()) {
                        return Boolean.FALSE;
                    }

                    // send expressions to Refocus endpoint
                    try (Timer.Context context = systemStatus.timer(meterName(), "upsert-samples-bulk." + endpointId).time()) {
                        return client.upsertSamplesBulk(allSamplesForEndpoint);

                    // return failure
                    } catch (UnauthorizedException e) {
                        logger.error("Could not complete request for {}; failed samples={}", endpointId, allSamplesForEndpoint);
                        return Boolean.FALSE;
                    }
                })

                // return true if all Samples are successfully upserted into all endpoints
                .allMatch(s -> s.equals(Boolean.TRUE));

        // log result of operation
        //   Note: the upsertSamplesBulk Refocus API call, will always return OK, so failures will be marked
        //         only if the endpoint does not respond at all, or we cannot authorize against it
        if (allUpserted) {
            succeeded();
        } else {
            failed();
        }

        // return final result
        return Collections.singletonList(allUpserted);
    }


    /**
     * Create a string containing the sample's metadata messages, each on an individual line
     *
     * @return null if there are no messages
     */
    private static String convertMessagesToString(final List<String> messages) {
        // if there are no messages, stop here
        if (messages.isEmpty()) {
            return null;
        }

        return messages.stream().collect(Collectors.joining("\n"));
    }

    /**
     * Destination type this processor can load into
     */
    @Override
    public Class<Refocus> filteredType() {
        return Refocus.class;
    }

    /**
     * Meter name used to track this implementation's system status
     */
    @Override
    protected String meterName() {
        return "Refocus";
    }

    @Override
    public ClientFactory<RefocusClient> factory() {
        return refocusClientFactory;
    }
}
