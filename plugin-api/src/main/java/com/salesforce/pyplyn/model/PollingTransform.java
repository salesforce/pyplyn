package com.salesforce.pyplyn.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;

import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Transform} implementation that can poll an endpoint until it retrieves the results or it times out
 * <p/>
 * <p/><b>All implementations should be {@link Serializable}!</b>
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public abstract class PollingTransform<T> implements Transform {
    @JsonProperty(required = true)
    protected String endpoint;

    @JsonProperty(defaultValue = "200")
    protected Long backoffIntervalMillis;

    @JsonProperty(defaultValue = "1000")
    protected Long initialDelayMillis;

    @JsonProperty(defaultValue = "7500")
    protected Long timeoutMillis;


    /**
     * This method will send a request to the specified endpoint
     * @return the request to poll for
     */
    public abstract T sendRequest(List<List<TransformationResult>> input);

    /**
     * The retrieve operation will be called until either the result is not null or the timeout expires
     * <p/>
     * <p/> <strong>It is important that this method returns null if the polling operation has not completed yet!</strong>
     *
     * @param request The request object to poll for
     * @return the completed result or null if the request is not ready
     */
    public abstract List<List<TransformationResult>> retrieveResult(T request);

    /**
     * Overrides the default implementation, to simplify implementing async transforms
     * <p/>
     * <p/> {@link PollingTransform}s are observed on the specified scheduler
     */
    @Override
    public Flowable<List<List<TransformationResult>>> applyAsync(List<List<TransformationResult>> input, Scheduler scheduler) {
        // counts how many times we have retried
        final AtomicInteger retryCount = new AtomicInteger(-1);

        return Flowable.just(input).observeOn(scheduler)

                // send payload
                .map(this::sendRequest)

                // poll for responses
                .flatMap(a -> Flowable.just(a)

                        // attempts to retrieve the results
                        .map(this::retrieveResult)

                        // retry on failures (when result is null)
                        .retryWhen((Flowable<Throwable> attempts) -> attempts
                                // retry with exponential backoff
                                .flatMap(err -> Flowable.timer(nextTick(retryCount.getAndIncrement()), TimeUnit.MILLISECONDS)))

                        // or timeout after the specified number of ms
                        .timeout(timeoutMillis(), TimeUnit.MILLISECONDS)
                );
    }

    /**
     * This method is not used by default
     * <p/> it is implemented to ensure to validate the send/receive contract at compile time
     *
     * @throws NoSuchElementException if the value cannot be retrieved in one try
     */
    @Override
    public List<List<TransformationResult>> apply(List<List<TransformationResult>> input) {
        return retrieveResult(sendRequest(input));
    }

    /**
     * @return initial delay (ms) before first load attempt is made
     */
    public final Long initialDelayMillis() {
        return Optional.ofNullable(initialDelayMillis).orElse(1000L);
    }

    /**
     * @return the backoff value (ms); the second and all all other retries will be attempted after backoff*2^N ms
     */
    public final Long backoffIntervalMillis() {
        return Optional.ofNullable(backoffIntervalMillis).orElse(200L);
    }

    /**
     * @return the timeout after which the retrieval will be aborted
     */
    public final Long timeoutMillis() {
        return Optional.ofNullable(timeoutMillis).orElse(10000L);
    }

    /**
     * @return the delay after which the next retrieval will be attempted;
     *         it will respect the following rule: INITIAL + SUM(BACKOFF*2^N) < TIMEOUT
     */
    private Long nextTick(int retryCount) {
        // the first retry will be attempted after the specified initial delay
        if (retryCount < 0) {
            return initialDelayMillis();
        }

        // all other retries will respect the exponential backoff algorithm
        return (long)Math.pow(2.0, retryCount) * backoffIntervalMillis();
    }
}