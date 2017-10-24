/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.EndpointConnector;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Generic contract for Remote clients implemented in Pyplyn
 * @param <S> The generic type is intentionally left without an upper bound, to allow extending plugins to
 *           choose their own implementations and not have to rely on {@link Retrofit}
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public abstract class AbstractRemoteClient<S> implements RemoteClient {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRemoteClient.class);
    private ReentrantLock authLock = new ReentrantLock();

    private static int UNAUTHORIZED = 401;
    private static int ERR_CODES = 400;

    /**
     * Reference to the connector used by the current instance
     */
    private final EndpointConnector connector;

    /**
     * The retrofit object used to construct the service
     */
    private final Retrofit retrofit;

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
     * Implementations of this method should clear any authentication artifacts,
     *   in order to force a new authentication operation to take place
     */
    protected abstract void resetAuth();

    /**
     * This method should be used to authenticate the client to its endpoint
     *   to prevent duplicate operations from multiple parallel threads
     * <p/>
     * <p/> It uses a latch to ensure only one operation is performed
     * @return true if the operation has succeeded
     * @throws UnauthorizedException
     */
    public boolean authenticate() throws UnauthorizedException {
        authLock.lock();
        try {
            // if another thread has performed the authentication, do not repeat the operation
            return isAuthenticated() || auth();

        } finally {
            authLock.unlock();
        }
    }

    /**
     * Generates a header by combining a prefix with a byte[] token
     *
     * @return empty string, if a null token was passed
     */
    public static String prefixTokenHeader(byte[] token, @Nonnull String prefix) {
        // return empty string if token could not be retrieved
        if (isNull(token)) {
            return "";
        }

        return prefix + new String(token, Charset.defaultCharset());
    }

    /**
     * Default class constructor; initializes Retrofit with Jackson support via {@link JacksonConverterFactory}
     */
    protected AbstractRemoteClient(EndpointConnector connector, Class<S> cls) {
        this(connector, JacksonConverterFactory.create(), cls);
    }

    /**
     * Class constructor that allows replacing (or removing the {@link Converter.Factory}
     */
    protected AbstractRemoteClient(EndpointConnector connector, Converter.Factory converterFactory, Class<S> cls) {
        Preconditions.checkNotNull(connector, "Passed connector is null for " + this.getClass().getSimpleName());
        this.connector = connector;

        // Extended timeouts are needed to deal with extremely slow response times for some Argus API endpoints.
        OkHttpClient client;
        
        // set HTTP proxy, if configured
        if (connector.isProxyEnabled()) {
            client = httpClientBuilder(connector).proxy(createProxy(connector)).build();
        } else {
            client = httpClientBuilder(connector).build();
        }

        // build the retrofit service implementation, using a specified client and relying on Jackson serialization/deserialization
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(connector.endpoint())
                .client(client);

        // if a converter factory was specified, add one
        if (nonNull(converterFactory)) {
            builder.addConverterFactory(converterFactory);
        }

        retrofit = builder.build();
        this.svc = retrofit.create(cls);
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
    private static OkHttpClient.Builder httpClientBuilder(EndpointConnector connector) {
        // Set simple properties.
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                    .connectTimeout(connector.connectTimeout(), TimeUnit.SECONDS)
                    .readTimeout(connector.readTimeout(), TimeUnit.SECONDS)
                    .writeTimeout(connector.writeTimeout(), TimeUnit.SECONDS);
        
        // setup mutual authentication if needed
        if(connector.isMutualAuthEnabled()) {
            initSSLSocketFactory(connector, builder);
        }
        
        // Return builder.
        return builder;
    }

    /**
     * Configures an {@link SSLContext} on the specified {@link OkHttpClient.Builder}, taking into account
     *   the trust store specified by {@link EndpointConnector#keystorePath()} and {@link EndpointConnector#keystorePassword()}}
     */
    private static void initSSLSocketFactory(EndpointConnector connector, OkHttpClient.Builder builder) {
        char[] keystorePassword = byteToCharArray(connector.keystorePassword());
        try {
            // initialize keystore
            //   the default type is 'jks';  however, this can be changed by updating the `keystore.type` property
            //   in `$JAVA_HOME/lib/security/java.security`
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(new FileInputStream(connector.keystorePath()), keystorePassword);


            // setup trust manager factory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            X509TrustManager trustManager = (X509TrustManager)trustManagerFactory.getTrustManagers()[0];

            // setup key manager factory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keystorePassword);

            // initialize an SSL context using the same protocol as the default and init
            SSLContext sslContext = SSLContext.getInstance(connector.sslContextAlgorithm());
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            // set socket factory
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);

        } catch (IOException |CertificateException |UnrecoverableKeyException |NoSuchAlgorithmException |KeyStoreException |KeyManagementException e) {
            // rethrow as RTE, since we cannot continue if mutual auth was expected but could not be initialized
            throw new RuntimeException("Unexpected exception configuring mutual authentication for " + connector.id(), e);

        } finally {
            // clear password after using it
            Arrays.fill(keystorePassword, (char)0);
        }
    }

    /**
     * Convert a byte[] to char[]
     */
    public static char[] byteToCharArray(byte[] bytes) {
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        CharBuffer decode = Charset.defaultCharset().decode(wrap);
        return decode.array();
    }

    /**
     * Creates a proxy object that will be used to send any service calls through
     *
     * @return a {@link Proxy} object initialized with the values specified in this endpoint's corresponding
     *         {@link Connector}
     */
    private Proxy createProxy(EndpointConnector connector) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(connector.proxyHost(), connector.proxyPort()));
    }

    /**
     * Executes the remote call and returns the response or returns <b>defaultFailureResponse</b> if the operation fails
     *
     * @return if successful, returns the result of calling {@link Response}.body() on the resulting response
     * @throws UnauthorizedException if the endpoint could not be authenticated
     */
    protected <T> T executeNoRetry(Call<T> call, T defaultFailResponse) throws UnauthorizedException {
        return Optional.ofNullable(executeCallInternal(call))
                .map(Response::body)
                .orElse(defaultFailResponse);
    }

    /**
     * Executes the remote call and returns the response or returns <b>defaultFailureResponse</b> if the operation fails
     * <p/>
     * <p/>The call is retried once if the operation fails due to an {@link UnauthorizedException}.
     * Authentication is attempted before the retry to account for expired tokens.
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
    protected <T> Headers executeAndRetrieveHeaders(Call<T> call) throws UnauthorizedException {
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
            // resets any authentication tokens and attempts to re-authenticate
            resetAuth();
            authenticate();
            return executeCallInternal(call.clone());
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
                logger.info("Successful remote call {}/{} {}; response={}",
                    getClass().getSimpleName(), requestMethod, requestUrl, response);
                return response;
            }

            // check if we are not authorized
            if (response.code() == UNAUTHORIZED) {
                try {
                    throw new UnauthorizedException(generateExceptionDetails(response));

                } finally {
                    call.cancel();
                }
            }

            // log any failures
            final String errorBody = response.errorBody().string();
            logger.info("Unsuccessful remote call {}/{} {}; response={}",
                getClass().getSimpleName(), requestMethod, requestUrl, errorBody);

        } catch (IOException e) {
            logger.error("Error during remote call {}/{} {}: {}",
                getClass().getSimpleName(), requestMethod, requestUrl, e.getMessage());
            logger.debug("Error during remote call " + requestMethod + " " + requestUrl + " [stacktrace]: ", e);
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
     * @return connector used by the current instance
     */
    public final EndpointConnector connector() {
        return connector;
    }

    /**
     * @return the {@link Retrofit} object used to create the service
     */
    protected Retrofit retrofit() {
        return retrofit;
    }

    /**
     * Specifying the endpoint id as cache key should suffice for most implementations
     *   since connector endpoint ids are implicitly unique
     */
    @Override
    public String endpoint() {
        return connector.id();
    }
}
