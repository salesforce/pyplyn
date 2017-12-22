package com.salesforce.pyplyn.duct.etl.load.refocus;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;
import static java.util.Objects.nonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.salesforce.refocus.model.UpsertResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.connector.AppConnectors;
import com.salesforce.pyplyn.model.Transmutation;
import com.salesforce.pyplyn.processor.AbstractMeteredLoadProcessor;
import com.salesforce.refocus.RefocusClient;
import com.salesforce.refocus.model.ImmutableSample;
import com.salesforce.refocus.model.Link;
import com.salesforce.refocus.model.Sample;

/**
 * Pushes data into Refocus
 * <p/>Annotated as Singleton as there should only be one instance of this class in operation.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class RefocusLoadProcessor extends AbstractMeteredLoadProcessor<Refocus> {
    private static final Logger logger = LoggerFactory.getLogger(RefocusLoadProcessor.class);

    private final AppConnectors appConnectors;
    private final ShutdownHook shutdownHook;

    @Inject
    public RefocusLoadProcessor(AppConnectors appConnectors, ShutdownHook shutdownHook) {
        this.appConnectors = appConnectors;
        this.shutdownHook = shutdownHook;
    }

    /**
     * Posts the data as Refocus samples on the specified endpoint
     *
     * @return Empty list if nothing was processed
     */
    @Override
    public List<Boolean> process(final List<Transmutation> data, List<Refocus> destinations) {
        // if data is empty, stop here as there is nothing to do
        if (data.isEmpty()) {
            return Collections.emptyList();
        }

        boolean allUpserted = destinations.stream()
                // group by endpoint
                .collect(Collectors.groupingBy(Refocus::endpoint))

                // process each endpoint individually
                .entrySet().parallelStream()

                .map(destinationEntry -> {
                    // retrieve client endpoint
                    String endpointId = destinationEntry.getKey();
                    final List<Refocus> loadDestinations = destinationEntry.getValue();

                    // retrieve Refocus client and cache for the specified endpoint
                    AppConnectors.ClientAndCache<RefocusClient, Sample> cc = appConnectors.retrieveOrBuildClient(endpointId, RefocusClient.class, Sample.class);
                    final RefocusClient client = cc.client();

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
                    List<Sample> allSamplesForEndpoint = loadDestinations.stream()
                            .map(loadDestination -> {
                                final String sampleName = loadDestination.name();
                                final List<Link> relatedLinks = loadDestination.relatedLinks();
                                String defaultMessageCode = loadDestination.defaultMessageCode();
                                String defaultMessageBody = loadDestination.defaultMessageBody();

                                return data.stream().map(result -> {
                                    // create message code and body, based on previously defined values
                                    Transmutation.Metadata metadata = result.metadata();
                                    String messageCodeString = Optional.ofNullable(metadata.messageCode()).orElse(defaultMessageCode);
                                    String messageBodyString = Optional.ofNullable(convertMessagesToString(metadata.messages())).orElse(defaultMessageBody);

                                    return ImmutableSample.builder()
                                            .name(sampleName)
                                            .value(formatNumber(result.value()))
                                            .relatedLinks(relatedLinks)
                                            .messageCode(messageCodeString)
                                            .messageBody(messageBodyString)
                                            .build();
                                });
                            })

                            // flatten stream and collect to list of samples that need to be processed by this endpoint
                            .flatMap(s -> s)
                            .collect(Collectors.toList());

                    // if shutting down, do not post to the Refocus endpoint
                    if (shutdownHook.isShutdown()) {
                        return null;
                    }

                    // send expressions to Refocus endpoint
                    try (Timer.Context context = systemStatus.timer(meterName(), "upsert-samples-bulk." + endpointId).time()) {
                        UpsertResponse upsertResponse = client.upsertSamplesBulk(allSamplesForEndpoint);
                        logger.info("Refocus bulk upsert response: {}", upsertResponse);
                        return Optional.ofNullable(upsertResponse).map(UpsertResponse::jobId).orElse(null);

                        // return failure
                    } catch (UnauthorizedException e) {
                        logger.error("Could not complete request for {}; failed samples={}", endpointId, allSamplesForEndpoint);
                        return null;
                    }
                })

                // return true if all Samples are successfully upserted into all endpoints
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

    @Override
    public Class<Refocus> filteredType() {
        return Refocus.class;
    }

    @Override
    protected String meterName() {
        return "Refocus";
    }
}
