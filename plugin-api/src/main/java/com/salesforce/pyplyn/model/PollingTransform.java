package com.salesforce.pyplyn.model;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import org.immutables.value.Value;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.reactivex.Flowable.defer;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * {@link Transform} implementation that can poll an endpoint until it retrieves the results or it times out
 * <p/>
 * <p/><b>Since the {@link Transform} interface implements {@link Serializable}, all subclasses should define
 * <code>serialVersionUID</code>!</b>
 * <p/>
 * <p/>Polling will take place as follows:
 * <p/>- first attempt will take place after the initialDelay has passed
 * <p/>- if the result is not ready yet, further attempts will be made every <code>2^attempt * backoffInterval </code> milliseconds
 * <p/>- if more time passes than the specified timeout, the transform will return an empty response
 * <p/>
 * <p/> NOTE: since the exponential backoff algorithm can and will rapidly increment the delay until the next attempt,
 *            it is important that you choose a good <code>initialDelayMillis</code> that will be close to the expected response time
 *            and use a small <code>backoffIntervalMillis</code> to ensure a high rate of success
 * <p/> NOTE 2: each retry will be performed after the {@link PollingTransform#retrieveResult(Object)} completes, to ensure that
 *              the corresponding endpoint is not overloaded
 * <p/>
 * <p/>
 * <p/> By default, any passed value(s) will be sent and their results retrieved by polling. However, exceptions might be required,
 *      for which case this plugin defines a simple way to filter any unwanted processing operations.
 * <p/>
 * <p/> Example (processing only values greater than or equal to zero):
 * <code>
 *     {
 *       "threshold: "1.0",
 *       "type": "GREATER_THAN"
 *     }
 * </code>
 *
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public abstract class PollingTransform<T> implements Transform {
    private static final Logger logger = LoggerFactory.getLogger(PollingTransform.class);

    /**
     * @return the endpoint that should be queried by the implementing class; the credentials are retrieved from AppConnectors at runtime
     */
    public abstract String endpoint();

    /**
     * @return the backoff value (ms); the second and all all other retries will be attempted after backoff*2^N ms
     */
    @Value.Default
    @Value.Auxiliary
    public long backoffIntervalMillis() {
        return 200L;
    }

    /**
     * @return initial delay (ms) before first load attempt is made
     */
    @Value.Default
    @Value.Auxiliary
    public long initialDelayMillis() {
        return 1_000L;
    }

    /**
     * @return the timeout after which the retrieval will be aborted
     */
    @Value.Default
    @Value.Auxiliary
    public long timeoutMillis() {
        return 7_500L;
    }

    @Nullable
    @Value.Auxiliary
    public abstract Double threshold();

    @Nullable
    @Value.Auxiliary
    public abstract ThresholdType type();

    /**
     * This method will send a request to the specified endpoint
     * @return the request to poll for
     */
    @Value.Auxiliary
    public abstract T sendRequest(List<List<Transmutation>> input);

    /**
     * The retrieve operation will be called until either the result is not null or the timeout expires
     * <p/>
     * <p/> <strong>It is important that this method returns null if the polling operation has not completed yet!</strong>
     *
     * @param request The request object to poll for
     * @return the completed result or null if the request is not ready
     */
    @Value.Auxiliary
    public abstract List<List<Transmutation>> retrieveResult(T request);

    /**
     * Overrides the default implementation, to simplify implementing async transforms
     * <p/>
     * <p/> {@link PollingTransform}s are observed on the specified scheduler
     */
    @Override
    public Flowable<List<List<Transmutation>>> applyAsync(List<List<Transmutation>> input, Scheduler scheduler) {
        // remove items that should not be processed
        List<List<Transmutation>> toProcess = filter(input);
        if (isNull(toProcess)) {
            logger.info("Stopping processing in {} due to threshold not met...", this.getClass().getSimpleName());
            return Flowable.empty();
        }

        return Flowable.just(toProcess)
                .observeOn(scheduler)

                // send payload
                .map(this::sendRequest)

                .flatMap(req -> tryRetrieve(req, -1))

                // or timeout after the specified number of ms
                .timeout(timeoutMillis(), TimeUnit.MILLISECONDS)
                .doOnError(this::logAsyncError)
                .onErrorResumeNext(Flowable.empty());
    }

    /**
     * Log errors encountered in async flow
     */
    protected void logAsyncError(Throwable e) {
        if (e instanceof TimeoutException) {
            logger.warn("Timed out while waiting for operation to complete");
        } else {
            logger.warn("Unexpected error while processing PollingTransform for " + endpoint(), e);
        }
    }

    /**
     * Filters out data points that should not be processed
     */
    public List<List<Transmutation>> filter(List<List<Transmutation>> input) {
        // all values can be passed if type or threshold are not specified
        List<List<Transmutation>> toProcess = input;

        // if threshold and type were specified, apply them on the record set
        final ThresholdType type = type();
        final Double threshold = threshold();
        if (nonNull(type) && nonNull(threshold)) {
            toProcess = input.stream()
                    .map(points -> points.stream()
                            // only include elements that match
                            .filter(transmutation -> type.matches(transmutation.value(), threshold))
                            .collect(Collectors.toList())
                    )
                    .collect(Collectors.toList());
        }

        // determine if there are any elements to process
        Optional<Transmutation> hasElements = toProcess.stream().flatMap(Collection::stream).findAny();

        // if there are no records to process, return null to cause a failure downstream
        return hasElements.map(e -> input).orElse(null);
    }

    /**
     * Attempts a retrieval operation taking into account the number of attempts
     */
    private Flowable<List<List<Transmutation>>> tryRetrieve(T request, int retryCount) {
        return Flowable.timer(nextTick(retryCount), TimeUnit.MILLISECONDS).map(time -> retrieveResult(request))
                .onErrorResumeNext(defer(() -> tryRetrieve(request, retryCount+1)).take(1));
    }

    /**
     * @return the delay after which the next retrieval will be attempted;
     *         it will respect the following rule: INITIAL + SUM(BACKOFF*2^N) < TIMEOUT
     */
    private long nextTick(int retryCount) {
        // the first retry will be attempted after the specified initial delay
        if (retryCount < 0) {
            return initialDelayMillis();
        }

        // all other retries will respect the exponential backoff algorithm
        return (long)Math.pow(2.0, retryCount) * backoffIntervalMillis();
    }

    /**
     * This method is not used by default for this plugin.
     * <p/> It is implemented to ensure to validate the send/receive contract at compile time
     *
     * @throws NoSuchElementException if the value cannot be retrieved in one try
     */
    @Override
    public List<List<Transmutation>> apply(List<List<Transmutation>> input) {
        return retrieveResult(sendRequest(input));
    }
}