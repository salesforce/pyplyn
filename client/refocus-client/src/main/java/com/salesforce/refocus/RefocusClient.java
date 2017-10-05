/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus;

import static com.salesforce.pyplyn.util.CollectionUtils.nullOutByteArray;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.salesforce.pyplyn.client.AbstractRemoteClient;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.EndpointConnector;
import com.salesforce.refocus.model.*;

/**
 * Refocus API implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public class RefocusClient extends AbstractRemoteClient<RefocusService> {

    // authentication token
    private volatile byte[] accessToken;


    /**
     * Default simplified constructor that uses the specified connection defaults
     *
     * @param connector The Refocus API endpoint to use in calls
     */
    public RefocusClient(EndpointConnector connector) {
        super(connector, RefocusService.class);
    }

    /**
     * @return true if the current client is authenticated against its endpoint
     */
    @Override
    public boolean isAuthenticated() {
        return nonNull(accessToken);
    }

    /**
     * Authenticates to a Refocus endpoint, using either a user and password or a token
     * <p/>
     * <p/>To specify a token, pass <b>username</b> as null and the token in the <b>password</b> field,
     *   base64 encoding its string value, in the corresponding {@link EndpointConnector} implementation.
     *
     * @throws UnauthorizedException if the passed credentials are invalid
     */
    @Override
    protected boolean auth() throws UnauthorizedException {
        // if we know the token already, we can consider the state as authenticated
        if (nonNull(accessToken)) {
            return true;
        }

        // if an username is not specified, we consider the password to be the accessToken
        String username = connector().username();
        if (isNull(username)) {
            this.accessToken = connector().password();
            return true;
        }

        // retrieve password
        byte[] password = connector().password();
        try {
            AuthResponse response = executeNoRetry(svc().authenticate(ImmutableAuthRequest.of(connector().username(), password)), null);

            // failed to retrieve a token, stop here
            if (isNull(response)) {
                return false;
            }

            // memoize the token
            this.accessToken = response.token();

            // mark success
            return true;

        // null out password bytes, once used
        } finally {
            nullOutByteArray(password);
        }
    }

    /**
     * Clears the authentication tokens
     */
    @Override
    protected void resetAuth() {
        this.accessToken = null;
    }

    /**
     * Construct an authorization header
     */
    String authorizationHeader() {
        return prefixTokenHeader(accessToken, "");
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
        return executeAndRetrieveBody(svc().getSample(authorizationHeader(), key, fields), null);
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
        return executeAndRetrieveBody(svc().getSample(authorizationHeader(), name), emptyList());
    }


    /**
     * Upsert a list of samples
     *
     * @throws IllegalArgumentException if null samples were passed
     * @return true if operation succeeded
     */
    public boolean upsertSamplesBulk(List<Sample> samples) throws UnauthorizedException {
        Preconditions.checkNotNull(samples, "Samples should not be null");
        return nonNull(executeAndRetrieveBody(svc().upsertSamplesBulk(authorizationHeader(), samples), null));
    }


    /**
     * Delete sample from endpoint
     *
     * @throws IllegalArgumentException if null key was passed
     * @return the deleted sample that will no longer be available on the remote endpoint
     */
    public Sample deleteSample(String key) throws UnauthorizedException {
        Preconditions.checkNotNull(key, "Key should not be null");
        return executeAndRetrieveBody(svc().deleteSample(authorizationHeader(), key), null);
    }


    /**
     * Retrieves all known subjects from the Refocus endpoint
     *
     * @param fields list of fields to retrieve
     * @return empty collection if any errors encountered
     */
    public List<Subject> getSubjects(List<String> fields) throws UnauthorizedException {
        return executeAndRetrieveBody(svc().getSubjects(authorizationHeader(), fields), emptyList());
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
        return executeAndRetrieveBody(svc().getSubject(authorizationHeader(), key, fields), null);
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
        return executeAndRetrieveBody(svc().getSubjectHierarchy(authorizationHeader(), key, status), null);
    }

    /**
     * Inserts a new subject into the remote endpoint
     *
     * @throws IllegalArgumentException if null subject was passed
     * @return true if successful
     */
    public Subject postSubject(Subject subject) throws UnauthorizedException {
        Preconditions.checkNotNull(subject, "Subject should not be null");
        return executeAndRetrieveBody(svc().postSubject(authorizationHeader(), subject), null);
    }

    /**
     * Patches a given subject
     *
     * @throws IllegalArgumentException if null subject was passed
     * @return null on failures
     */
    public Subject patchSubject(Subject subject) throws UnauthorizedException {
        Preconditions.checkNotNull(subject, "Subject should not be null");
        String subjectKey = Optional.ofNullable(subject.id()).orElse(subject.name());
        return executeAndRetrieveBody(svc().patchSubject(authorizationHeader(), subjectKey, subject), null);
    }

    /**
     * Replaces all set fields for the given subject
     *
     * @throws IllegalArgumentException if null subject was passed
     * @return null on failures
     */
    public Subject putSubject(Subject subject) throws UnauthorizedException {
        Preconditions.checkNotNull(subject, "Subject should not be null");
        String subjectKey = Optional.ofNullable(subject.id()).orElse(subject.name());
        return executeAndRetrieveBody(svc().putSubject(authorizationHeader(), subjectKey, subject), null);
    }

    /**
     * Deletes the specified subject
     *
     * @throws IllegalArgumentException if null subject key (ID or subject path) was passed
     * @return null on failures
     */
    public Subject deleteSubject(String subjectKey) throws UnauthorizedException {
        Preconditions.checkNotNull(subjectKey, "Subject key (ID or path) should not be null");
        return executeAndRetrieveBody(svc().deleteSubject(authorizationHeader(), subjectKey), null);
    }

    /**
     * Retrieves all known aspects from the Refocus endpoint
     *
     * @param fields List of fields to retrieve
     * @return empty collection if any errors encountered
     */
    public List<Aspect> getAspects(List<String> fields) throws UnauthorizedException {
        return executeAndRetrieveBody(svc().getAspects(authorizationHeader(), fields), null);
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
        return executeAndRetrieveBody(svc().getAspect(authorizationHeader(), key, fields), null);
    }

    /**
     * Inserts a new aspect into the remote endpoint
     *
     * @throws IllegalArgumentException if null aspect was passed
     * @return true if successful
     */
    public Aspect postAspect(Aspect aspect) throws UnauthorizedException {
        Preconditions.checkNotNull(aspect, "Aspect should not be null");
        return executeAndRetrieveBody(svc().postAspect(authorizationHeader(), aspect), null);
    }

    /**
     * Patches a given aspect
     *
     * @throws IllegalArgumentException if null aspect was passed
     * @return null on failures, or the patched aspect
     */
    public Aspect patchAspect(Aspect aspect) throws UnauthorizedException {
        Preconditions.checkNotNull(aspect, "Aspect should not be null");
        return executeAndRetrieveBody(svc().patchAspect(authorizationHeader(), aspect.name(), aspect), null);
    }
}