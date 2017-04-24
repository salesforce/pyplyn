/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

import com.salesforce.pyplyn.cache.Cacheable;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Generic contract for Remote clients implemented in Pyplyn
 * @param <S> The generic type is intentionally left without an upper bound, to allow extending plugins to
 *           choose their own implementations and not have to rely on {@link Retrofit}
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public abstract class AbstractRemoteClient<S> implements Cacheable {
    private static int UNAUTHORIZED = 401;
    private static int ERR_CODES = 400;

    /**
     * Unique identifier used to discern the corresponding client to use for this endpoint
     */
    private final String endpointId;

    /**
     * The service object performing remote API operations
     */
    private final S svc;

    /**
     * @return true if this client has authenticated against its endpoint
     */
    protected abstract boolean isAuthenticated();

    /**
     * Authenticates the client to the specified endpoint
     *
     * @return true if auth succeeded
     * @throws UnauthorizedException thrown if authentication failed (i.e.: invalid credentials, endpoint inaccessible, etc.)
     */
    protected abstract boolean auth() throws UnauthorizedException;

    /**
     * Override this method and return the implementing class' logger, to make the messages contextually relevant
     *
     * @return logger to use for reporting errors
     */
    protected abstract Logger logger();

    /**
     * Class constructor that allows setting timeout parameters
     */
    public AbstractRemoteClient(AbstractConnector connector, Class<S> cls, Long connectTimeout, Long readTimeout, Long writeTimeout) {
        // Extended timeouts are needed to deal with extremely slow response times for some Argus API endpoints.
        OkHttpClient client;

        // set HTTP proxy, if configured
        if (connector.isProxyEnabled()) {
            client = httpClientBuilder(connectTimeout, readTimeout, writeTimeout).proxy(createProxy(connector)).build();
        } else {
            client = httpClientBuilder(connectTimeout, readTimeout, writeTimeout).build();
        }

        // build the retrofit service implementation, using a specified client and relying on Jackson serialization/deserialization
        this.endpointId = connector.connectorId();
        this.svc = new Retrofit.Builder()
                .baseUrl(connector.endpoint())
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
                .create(cls);
    }

    /**
     * @return the initialized service object
     */
    public S svc() {
        return svc;
    }

    /**
     * Initializes an {@link OkHttpClient.Builder} object, with the specified timeouts
     */
    private OkHttpClient.Builder httpClientBuilder(Long connectTimeout, Long readTimeout, Long writeTimeout) {
        return new OkHttpClient().newBuilder()
                    .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                    .readTimeout(readTimeout, TimeUnit.SECONDS)
                    .writeTimeout(writeTimeout, TimeUnit.SECONDS);
    }

    /**
     * Creates a proxy object that will be used to send any service calls through
     *
     * @return a {@link Proxy} object initialized with the values specified in this endpoint's corresponding
     *         {@link AbstractConnector}
     */
    private Proxy createProxy(AbstractConnector connector) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(connector.proxyHost(), connector.proxyPort()));
    }

    /**
     * Executes the remote call and returns the response or returns <b>defaultFailureResponse</b> if the operation fails
     *
     * @return if successful, returns the result of calling {@link Response}.body() on the resulting response
     * @throws UnauthorizedException if the endpoint could not be authenticated
     */
    protected <T> T executeAndRetrieveBody(Call<T> call, T defaultFailResponse) throws UnauthorizedException {
        return Optional.ofNullable(executeCallInternalRetryIfUnauthorized(call))
                .map(Response::body)
                .orElse(defaultFailResponse);
    }

    /**
     * Executes the remote call and returns the HTTP response headers
     *
     * @throws UnauthorizedException if the endpoint could not be authenticated
     */
    protected  <T> Headers executeAndRetrieveHeaders(Call<T> call) throws UnauthorizedException {
        return Optional.ofNullable(executeCallInternal(call))
                .map(Response::headers)
                .orElse(null);
    }

    /**
     * Executes the {@link Retrofit} call
     *   if the initial call fails with {@link UnauthorizedException}, the authentication operation is called one more
     *   time and the call is then retried
     *
     * @throws UnauthorizedException if the call fails after attempting to re-authenticate
     */
    private <T> Response<T> executeCallInternalRetryIfUnauthorized(Call<T> call) throws UnauthorizedException {
        try {
            return executeCallInternal(call);

        } catch (UnauthorizedException e) {
            auth();
            return executeCallInternal(call);
        }
    }

    /**
     * Executes the {@link Retrofit} call and handles error logging; fails immediately if unauthenticated
     *
     * @throws UnauthorizedException if the endpoint is not authenticated
     */
    private <T> Response<T> executeCallInternal(Call<T> call) throws UnauthorizedException {
        final HttpUrl requestUrl = call.request().url();
        final String requestMethod = call.request().method();

        try {
            Response<T> response = call.execute();

            // success
            if(response.code() < ERR_CODES && response.isSuccessful()) {
                logger().info("Successful remote call {} {}; response={}", requestMethod, requestUrl, response.body());
                return response;
            }

            // check if we are not authorized
            if (response.code() == UNAUTHORIZED) {
                throw new UnauthorizedException(generateExceptionDetails(response));
            }

            // log any failures
            final String errorBody = response.errorBody().string();
            logger().info("Unsuccessful remote call {} {}; response={}", requestMethod, requestUrl, errorBody);

        } catch (IOException e) {
            logger().error("Error during remote call {} {}: {}", requestMethod, requestUrl, e.getMessage());
            logger().debug("Error during remote call " + requestMethod + " " + requestUrl + " [stacktrace]: ", e);
            call.cancel();
        }

        return null;
    }

    /**
     * Generates a standardized exception string from details passed in a {@link Response} object
     *
     * <p/>In case an {@link IOException} is thrown when constructing the response, it will be sent
     *   to the specified {@link Logger}
     */
    static String generateExceptionDetails(Response response) {
        String method = response.raw().request().method();
        HttpUrl url = response.raw().request().url();
        int code = response.code();
        String message = response.message();

        String errorBody;
        try {
            errorBody = response.errorBody().string();
        } catch (IOException e) {
            errorBody = "IOException while trying to retrieve error body: " + e.getMessage();
        }

        return String.format("Remote call failed %s %s [%d/%s]: %s", method, url, code, message, errorBody);
    }

    /**
     * Specifying the endpoint id as cache key should suffice for most implementations
     *   since connector endpoint ids are implicitly unique
     */
    @Override
    public String cacheKey() {
        return endpointId;
    }
}
