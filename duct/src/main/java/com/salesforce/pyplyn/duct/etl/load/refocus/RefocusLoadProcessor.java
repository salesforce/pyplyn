package com.salesforce.pyplyn.duct.etl.load.refocus;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.connector.AppConnector;
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
import java.util.concurrent.ConcurrentHashMap;
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
public class RefocusLoadProcessor extends AbstractMeteredLoadProcessor<Refocus> {
    private static final Logger logger = LoggerFactory.getLogger(RefocusLoadProcessor.class);

    private final ConcurrentHashMap<String, RefocusClient> endpoints = new ConcurrentHashMap<>();

    private final AppConnector appConnector;
    private final ShutdownHook shutdownHook;

    @Inject
    public RefocusLoadProcessor(AppConnector appConnector, ShutdownHook shutdownHook) {
        this.appConnector = appConnector;
        this.shutdownHook = shutdownHook;
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
                .entrySet().parallelStream()

                .map(destinationEntry -> {
                    // retrieve client endpoint
                    String endpointId = destinationEntry.getKey();
                    final List<Refocus> loadDestinations = destinationEntry.getValue();

                    final RefocusClient client = client(endpointId);
                    if (isNull(client)) {
                        // stop here if we couldn't get a client
                        return Boolean.FALSE;
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
     * Returns a previously initialized client for the specified endpoint
     *   or initializes one and returns it
     */
    public RefocusClient client(String endpointId) {
        // TODO: move this at interface level and abstract just the factory
        return endpoints.computeIfAbsent(endpointId, key -> {
            RefocusClient refocusClient = new RefocusClient(appConnector.get(endpointId));
            try {
                refocusClient.auth();
                return refocusClient;

            } catch (UnauthorizedException e) {
                // log auth failure if this exception type was thrown
                authenticationFailure();

                logger.warn("", e);
                return null;
            }
        });
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
