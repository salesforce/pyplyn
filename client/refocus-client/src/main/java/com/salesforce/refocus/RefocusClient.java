/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus;

import com.google.common.base.Preconditions;
import com.salesforce.pyplyn.client.AbstractRemoteClient;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.refocus.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.salesforce.pyplyn.util.CollectionUtils.nullOutByteArray;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Refocus API implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public class RefocusClient extends AbstractRemoteClient<RefocusService> {
    private static final Logger logger = LoggerFactory.getLogger(RefocusClient.class);
    private final AbstractConnector connector;

    // authorization header used for all calls
    private static final String HEADER_PREFIX = "";
    private String authorizationHeader;


    /**
     * Default simplified constructor that uses the specified connection defaults
     *
     * @param connector The Refocus API endpoint to use in calls
     */
    public RefocusClient(AbstractConnector connector) {
        this(connector, RefocusService.class, connector.connectTimeout(), connector.readTimeout(), connector.writeTimeout());
    }

    /**
     * Class constructor that allows setting connection params
     *
     * @param connectTimeout How long to wait for connections to be established
     * @param readTimeout How long to wait for reads
     * @param writeTimeout How long to wait for writes
     */
    private RefocusClient(AbstractConnector connector, Class<RefocusService> cls, Long connectTimeout, Long readTimeout, Long writeTimeout) {
        super(connector, cls, connectTimeout, readTimeout, writeTimeout);
        this.connector = connector;
    }

    /**
     * @return true if the current client is authenticated against its endpoint
     */
    @Override
    public boolean isAuthenticated() {
        return nonNull(authorizationHeader);
    }

    /**
     * @return the current class' {@link Logger} object, required by its supertype
     */
    @Override
    protected Logger logger() {
        return logger;
    }

    /**
     * Authenticates to a Refocus endpoint, using either a user and password or a token
     * <p/>
     * <p/>To specify a token, pass <b>username</b> as null and the token in the <b>password</b> field,
     *   base64 encoding its string value, in the corresponding {@link AbstractConnector} implementation.
     *
     * @throws UnauthorizedException if the passed credentials are invalid
     */
    @Override
    public boolean auth() throws UnauthorizedException {
        // if a token is specified (null user, password specified as byte[])
        byte[] password = connector.password();
        try {
            if (isNull(connector.username()) && nonNull(password)) {
                // store it so we can authenticate all calls with it
                this.authorizationHeader = generateAuthorizationHeader(new String(password, Charset.defaultCharset()));
                return true;
            }

            // otherwise attempt to auth user/pass
            AuthResponse authResponse =
                    executeAndRetrieveBody(svc().authenticate(new AuthRequest(connector.username(), password)), null);

            // if successful, generate and store the auth header
            if (nonNull(authResponse)) {
                this.authorizationHeader = generateAuthorizationHeader(authResponse.token());
                return true;
            }

            return false;

        // null out password bytes, once used
        } finally {
            nullOutByteArray(password);
        }
    }

    /**
     * Generates the Authorization header
     *
     * @return empty string, if a null token was passed
     */
    String generateAuthorizationHeader(String token) {
        return Optional.ofNullable(token).map(t -> HEADER_PREFIX + t).orElse("");
    }

    /**
     * Retrieves a sample from the remote endpoint
     *
     * @param key Id or name
     * @throws IllegalArgumentException if null key was passed
     * @return null if not found
     */
    public Sample getSample(String key, List<String> fields) throws UnauthorizedException {
        Preconditions.checkNotNull(key, "Key should not be null");
        return executeAndRetrieveBody(svc().getSample(authorizationHeader, key, fields), null);
    }

    /**
     * Retrieves a sample by name from the remote endpoint
     *
     * @param name Name of sample; can include wildcards, i.e.: Subject.Path.*|ASPECT_NAME
     * @throws IllegalArgumentException if a null name was passed
     * @return null if not found
     */
    public List<Sample> getSamples(String name) throws UnauthorizedException {
        Preconditions.checkNotNull(name, "Name should not be null");
        return executeAndRetrieveBody(svc().getSample(authorizationHeader, name), Collections.emptyList());
    }


    /**
     * Upsert a list of samples
     *
     * @throws IllegalArgumentException if null samples were passed
     * @return true if operation succeeded
     */
    public boolean upsertSamplesBulk(List<Sample> samples) throws UnauthorizedException {
        Preconditions.checkNotNull(samples, "Samples should not be null");
        return nonNull(executeAndRetrieveBody(svc().upsertSamplesBulk(authorizationHeader, samples), null));
    }


    /**
     * Delete sample from endpoint
     *
     * @throws IllegalArgumentException if null key was passed
     * @return the deleted sample that will no longer be available on the remote endpoint
     */
    public Sample deleteSample(String key) throws UnauthorizedException {
        Preconditions.checkNotNull(key, "Key should not be null");
        return executeAndRetrieveBody(svc().deleteSample(authorizationHeader, key), null);
    }


    /**
     * Retrieves all known subjects from the Refocus endpoint
     *
     * @param fields list of fields to retrieve
     * @return empty collection if any errors encountered
     */
    public List<Subject> getSubjects(List<String> fields) throws UnauthorizedException {
        return executeAndRetrieveBody(svc().getSubjects(authorizationHeader, fields), Collections.emptyList());
    }

    /**
     * Retrieves a subject from the remote endpoint
     *
     * @param key id or subject path
     * @throws IllegalArgumentException if null key was passed
     * @return null if not found
     */
    public Subject getSubject(String key, List<String> fields) throws UnauthorizedException {
        Preconditions.checkNotNull(key, "Key should not be null");
        return executeAndRetrieveBody(svc().getSubject(authorizationHeader, key, fields), null);
    }

    /**
     * Retrieves a subject hierarchy from the remote endpoint
     *
     * @param key id or subject path
     * @param status status to include (filter by)
     * @throws IllegalArgumentException if null key was passed
     * @return null if not found
     */
    public Subject getSubjectHierarchy(String key, String status) throws UnauthorizedException {
        Preconditions.checkNotNull(key, "Key should not be null");
        return executeAndRetrieveBody(svc().getSubjectHierarchy(authorizationHeader, key, status), null);
    }

    /**
     * Inserts a new subject into the remote endpoint
     *
     * @throws IllegalArgumentException if null subject was passed
     * @return true if successful
     */
    public Subject postSubject(Subject subject) throws UnauthorizedException {
        Preconditions.checkNotNull(subject, "Subject should not be null");
        return executeAndRetrieveBody(svc().postSubject(authorizationHeader, subject), null);
    }

    /**
     * Patches a given subject
     *
     * @throws IllegalArgumentException if null subject was passed
     * @return null on failures
     */
    public Subject patchSubject(Subject subject) throws UnauthorizedException {
        Preconditions.checkNotNull(subject, "Subject should not be null");
        return executeAndRetrieveBody(svc().patchSubject(authorizationHeader, subject.id(), subject), null);
    }

    /**
     * Retrieves all known aspects from the Refocus endpoint
     *
     * @param fields List of fields to retrieve
     * @return empty collection if any errors encountered
     */
    public List<Aspect> getAspects(List<String> fields) throws UnauthorizedException {
        return executeAndRetrieveBody(svc().getAspects(authorizationHeader, fields), null);
    }

    /**
     * Retrieves an aspect from the remote endpoint
     *
     * @param key id or aspect name
     * @throws IllegalArgumentException if null key was passed
     * @return null if not found
     */
    public Aspect getAspect(String key, List<String> fields) throws UnauthorizedException {
        Preconditions.checkNotNull(key, "Key should not be null");
        return executeAndRetrieveBody(svc().getAspect(authorizationHeader, key, fields), null);
    }

    /**
     * Inserts a new aspect into the remote endpoint
     *
     * @throws IllegalArgumentException if null aspect was passed
     * @return true if successful
     */
    public Aspect postAspect(Aspect aspect) throws UnauthorizedException {
        Preconditions.checkNotNull(aspect, "Aspect should not be null");
        return executeAndRetrieveBody(svc().postAspect(authorizationHeader, aspect), null);
    }

    /**
     * Patches a given aspect
     *
     * @throws IllegalArgumentException if null aspect was passed
     * @return null on failures, or the patched aspect
     */
    public Aspect patchAspect(Aspect aspect) throws UnauthorizedException {
        Preconditions.checkNotNull(aspect, "Aspect should not be null");
        return executeAndRetrieveBody(svc().patchAspect(authorizationHeader, aspect.name(), aspect), null);
    }
}