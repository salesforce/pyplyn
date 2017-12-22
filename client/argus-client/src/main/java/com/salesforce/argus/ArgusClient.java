/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus;

import static com.salesforce.pyplyn.util.CollectionUtils.nullOutByteArray;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.salesforce.argus.model.*;
import com.salesforce.pyplyn.client.AbstractRemoteClient;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.EndpointConnector;

/**
 * Argus client implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ArgusClient extends AbstractRemoteClient<ArgusService> {
    
    private static final String AUTH_HEADER_PREFIX = "Bearer ";

    // authentication tokens
    private volatile byte[] accessToken;
    private volatile byte[] refreshToken;


    /**
     * Default constructor
     *
     * @param connector The Argus endpoint to use in all the calls made by this collector
     */
    public ArgusClient(EndpointConnector connector) {
        super(connector, ArgusService.class);
    }

    /**
     * Returns true if an authentication token is known
     */
    @Override
    public boolean isAuthenticated() {
        return nonNull(accessToken);
    }

    /**
     * Authenticates the session with the Argus endpoint
     *   and stores the accessToken to be used for all calls
     *
     * @throws UnauthorizedException if it cannot authenticate
     */
    @Override
    protected boolean auth() throws UnauthorizedException {
        if (nonNull(refreshToken)) {
            AuthToken token = refresh(refreshToken);

            // if we have successfully retrieved the tokens, memoize them and mark success
            if (nonNull(token)) {
                this.accessToken = token.accessToken();
                return true;
            }

            // failed to retrieve a token, stop here
            return false;
        }

        // if an username is not specified, we consider the password to be the refreshToken and call itself recursively
        //   this is safe to do as long as this call is made through AbstractRemoteClient.authenticate() (thread-safe)
        String username = connector().username();
        if (isNull(username)) {
            this.refreshToken = connector().password();
            return auth();
        }

        // retrieve password
        byte[] password = connector().password();
        try {
            AuthToken token = executeNoRetry(svc().login(ImmutableAuthRequest.of(connector().username(), password)), null);

            // failed to retrieve a token, stop here
            if (isNull(token)) {
                return false;
            }

            // memoize both refresh and access tokens
            this.refreshToken = token.refreshToken();
            this.accessToken = token.accessToken();

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
        this.refreshToken = null;
        this.accessToken = null;
    }


    /**
     * Retrieves new access and refresh tokens from an Argus endpoint
     *
     * @throws UnauthorizedException if it cannot successfully update the tokens
     */
    private AuthToken refresh(byte[] refreshToken) throws UnauthorizedException {
        return executeNoRetry(svc().refresh(ImmutableAuthToken.of(new byte[0], refreshToken)), null);
    }

    /**
     * Construct an authorization header
     */
    String authorizationHeader() {
        return prefixTokenHeader(accessToken, AUTH_HEADER_PREFIX);
    }

    /**
     * Retrieve metrics for a list of expressions
     *
     * @param expressions the list of expressions to retrieve
     * @throws IllegalArgumentException if null expressions were passed
     * @return the list of metric responses or null if an error during the API call occurred
     */
    public List<MetricResponse> getMetrics(List<String> expressions) throws UnauthorizedException {
        Preconditions.checkNotNull(expressions, "Expressions should not be null");
        return executeAndRetrieveBody(svc().getMetrics(authorizationHeader(), expressions), null);
    }

    /**
     * Posts metrics to a remote endpoint
     *
     * @return A list of error messages and count of successes and failures
     * @throws UnauthorizedException
     */
    public MetricCollectionResponse postMetrics(List<MetricResponse> metrics) throws UnauthorizedException {
        Preconditions.checkNotNull(metrics, "Metrics should not be null");
        Preconditions.checkArgument(!metrics.isEmpty(), "Metrics should not be empty");
        return executeAndRetrieveBody(svc().postMetrics(authorizationHeader(), metrics), null);
    }

    /**
     * Create a new alert
     *
     * @throws IllegalArgumentException if null alert was passed
     */
    public AlertObject createAlert(AlertObject alert) throws UnauthorizedException {
        Preconditions.checkNotNull(alert, "Alert should not be null");
        return executeAndRetrieveBody(svc().createAlert(authorizationHeader(), alert), null);
    }

    /**
     * Update an existing alert
     *
     * @throws IllegalArgumentException if null alert was passed
     */
    public AlertObject updateAlert(AlertObject alert) throws UnauthorizedException {
        Preconditions.checkNotNull(alert, "Alert should not be null");
        Preconditions.checkNotNull(alert.id(), "Alert id should not be null");
        return executeAndRetrieveBody(svc().updateAlert(authorizationHeader(), alert.id(), alert), null);
    }
    
    /**
     * Get an existing alert by ID.
     */
    public AlertObject loadAlert(long id) throws UnauthorizedException {
        return executeAndRetrieveBody(svc().getAlert(authorizationHeader(), id), null);
    }

    /**
     * Get all alerts owned by the logged in user
     */
    public List<AlertObject> loadAllAlerts() throws UnauthorizedException {
        return executeAndRetrieveBody(svc().getAllAlerts(authorizationHeader()), emptyList());
    }


    /**
     * Get all alerts owned by the target user
     *
     * @throws IllegalArgumentException if null owner name was passed
     */
    public List<AlertObject> loadAlertsByOwner(String ownerName) throws UnauthorizedException {
        Preconditions.checkNotNull(ownerName, "Owner name should not be null");

        // The Argus API does not currently respect just the ownername filter and returns all visible alerts so as
        // a workaround filter it here.
        return executeAndRetrieveBody(svc().getAlertsByOwner(authorizationHeader(), ownerName), emptyList())
                        .stream().filter(a -> ownerName.equals(a.ownerName())).collect(Collectors.toList());
    }
    
    /**
     * Load alert metadata for the specified user.
     * <p/>
     * WARNING: Use of this method is highly discouraged since the instances of {@link AlertObject} returned
     * do not have their expression field populated. This method exists because it returns more quickly than
     * {@link #loadAlertsByOwner(String)}, which should be used instead wherever possible. 
     */
    public List<AlertObject> loadAlertsMetadataByOwner(String ownerName) throws UnauthorizedException {
        Preconditions.checkNotNull(ownerName, "Owner name should not be null");
        return executeAndRetrieveBody(svc().getAlertMetadataByOwner(authorizationHeader(), ownerName), emptyList())
                        .stream().filter(a -> ownerName.equals(a.ownerName())).collect(Collectors.toList());
    }

    /**
     * Delete an existing alert by ID
     *
     * @throws IllegalArgumentException if null alertId was passed
     */
    public void deleteAlert(long alertId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        executeAndRetrieveBody(svc().deleteAlert(authorizationHeader(), alertId), null);
    }


    /* Trigger operations. */

    /**
     * Load all triggers associated with an alert ID
     *
     * @throws IllegalArgumentException if null alertId was passed
     */
    public List<TriggerObject> loadTriggersForAlert(long alertId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        return executeAndRetrieveBody(svc().getTriggersForAlert(authorizationHeader(), alertId), emptyList());
    }

    /**
     * Create a new trigger tied to an alert
     *
     * @throws IllegalArgumentException if null alertId or trigger were passed
     */
    public List<TriggerObject> createTrigger(long alertId, TriggerObject trigger) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(trigger, "Trigger should not be null");
        return executeAndRetrieveBody(svc().createTrigger(authorizationHeader(), alertId, trigger), emptyList());
    }

    /**
     * Update an existing trigger
     *
     * @throws IllegalArgumentException if null alertId or trigger were passed
     */
    public TriggerObject updateTrigger(long alertId, TriggerObject trigger) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(trigger, "Trigger should not be null");
        return executeAndRetrieveBody(svc().updateTrigger(authorizationHeader(), alertId, trigger.id(), trigger), null);
    }

    /**
     * Delete a trigger
     *
     * @throws IllegalArgumentException if null alertId or trigger were passed
     */
    public void deleteTrigger(long alertId, long triggerId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(triggerId, "Trigger id should not be null");
        executeAndRetrieveBody(svc().deleteTrigger(authorizationHeader(), alertId, triggerId), null);
    }


    /* Notification operations. */

    /**
     * Load an existing notification
     *
     * @throws IllegalArgumentException if null alertId or notificationId were passed
     */
    public NotificationObject getNotification(long alertId, long notificationId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(notificationId, "Notification id should not be null");
        return executeAndRetrieveBody(svc().getNotification(authorizationHeader(), alertId, notificationId), null);
    }

    /**
     * Load existing notifications tied to an alert
     *
     * @throws IllegalArgumentException if null alertId was passed
     */
    public List<NotificationObject> getNotificationsForAlert(long alertId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        return executeAndRetrieveBody(svc().getNotificationsForAlert(authorizationHeader(), alertId), emptyList());
    }

    /**
     * Create a new notification
     *
     * @throws IllegalArgumentException if null alertId or notificationId were passed
     */
    public List<NotificationObject> createNotification(long alertId, NotificationObject notification) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(notification, "Notification should not be null");
        return executeAndRetrieveBody(svc().createNotification(authorizationHeader(), alertId, notification), emptyList());
    }

    /**
     * Update an existing notification
     *
     * @throws IllegalArgumentException if null alertId, notificationId, or notification were passed
     */
    public NotificationObject updateNotification(long alertId, long notificationId, NotificationObject notification) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(notificationId, "Notification id should not be null");
        Preconditions.checkNotNull(notification, "Notification should not be null");
        return executeAndRetrieveBody(svc().updateNotification(authorizationHeader(), alertId, notificationId, notification), null);
    }

    /**
     * Delete a notification
     *
     * @throws IllegalArgumentException if null alertId or notificationId were passed
     */
    public void deleteNotification(long alertId, long notificationId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(notificationId, "Notification id should not be null");
        executeAndRetrieveBody(svc().deleteNotification(authorizationHeader(), alertId, notificationId), null);
    }

    /**
     * Attach a trigger to a notification
     *
     * @throws IllegalArgumentException if null alertId, notificationId, or triggerId were passed
     */
    public TriggerObject attachTriggerToNotification(long alertId, long notificationId, long triggerId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(notificationId, "Notification id should not be null");
        Preconditions.checkNotNull(triggerId, "Trigger id should not be null");
        return executeAndRetrieveBody(svc().attachTriggerToNotification(authorizationHeader(), alertId, notificationId, triggerId), null);
    }

    /**
     * Remove a trigger/notification association
     *
     * @throws IllegalArgumentException if null alertId, notificationId, or triggerId were passed
     */
    public void removeTriggerFromNotification(long alertId, long notificationId, long triggerId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(notificationId, "Notification id should not be null");
        Preconditions.checkNotNull(triggerId, "Trigger id should not be null");
        executeAndRetrieveBody(svc().removeTriggerFromNotification(authorizationHeader(), alertId, notificationId, triggerId), null);
    }

    /**
     * Load all dashboards available in Argus available to this user
     */
    public List<DashboardObject> getAllDashboards() throws UnauthorizedException {
        return executeAndRetrieveBody(svc().getAllDashboards(authorizationHeader()), emptyList());
    }

    /**
     * Load the specified dashboard owned by the specified user
     *
     * @param owner The owner of the dashboard.
     * @param dashboardName The name of the dashboard.
     * @throws IllegalArgumentException if null or empty owner or dashboardName were passed
     */
    public DashboardObject getDashboardByName(String owner, String dashboardName) throws UnauthorizedException {
        // Validate that both arguments are non-empty strings since the API could return back everything otherwise
        Preconditions.checkNotNull(owner, "Owner name must be non-null");
        Preconditions.checkArgument(!owner.isEmpty(), "Owner name must non-empty");
        Preconditions.checkNotNull(dashboardName, "Dashboard name must be non-null");
        Preconditions.checkArgument(!dashboardName.isEmpty(), "Dashboard name must non-empty");

        // The argus API returns a list of dashboards for this API call despite a unique constraint on owner and dashboard name. If there
        // is not a match it returns an empty list. To avoid making callers deal with this strangeness just handle it here.
        List<DashboardObject> result = executeAndRetrieveBody(svc().getDashboardByName(authorizationHeader(), owner, dashboardName), null);
        if (isNull(result) || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Create a new dashboard
     *
     * @throws IllegalArgumentException if null dashboard was passed
     */
    public DashboardObject createDashboard(DashboardObject dashboard) throws UnauthorizedException {
        Preconditions.checkNotNull(dashboard, "Dashboard should not be null");
        return executeAndRetrieveBody(svc().createDashboard(authorizationHeader(), dashboard), null);
    }

    /**
     * Update the existing dashboard with the specified ID
     *
     * @throws IllegalArgumentException if null dashboardId or dashboard were passed
     */
    public DashboardObject updateDashboard(long dashboardId, DashboardObject dashboard) throws UnauthorizedException {
        Preconditions.checkNotNull(dashboardId, "Dashboard id should not be null");
        Preconditions.checkNotNull(dashboard, "Dashboard should not be null");
        return executeAndRetrieveBody(svc().updateDashboard(authorizationHeader(), dashboardId, dashboard), null);
    }

    /**
     * Delete the dashboard with the specified ID
     *
     * @throws IllegalArgumentException if null dashboardId or dashboard were passed
     */
    public void deleteDashboard(long dashboardId) throws UnauthorizedException {
        Preconditions.checkNotNull(dashboardId, "Dashboard id should not be null");
        executeAndRetrieveBody(svc().deleteDashboard(authorizationHeader(), dashboardId), null);
    }

    /**
     * Load the specified dashboard that is either public or owned by the current user
     *
     * @param dashboardId The id of the dashboard to return
     * @throws IllegalArgumentException if null dashboardId was passed
     */
    public DashboardObject getDashboardById(long dashboardId) throws UnauthorizedException {
        Preconditions.checkNotNull(dashboardId, "Dashboard id should not be null");
        return executeAndRetrieveBody(svc().getDashboardById(authorizationHeader(), dashboardId), null);
    }
}
