/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus;

import com.google.common.base.Preconditions;
import com.salesforce.argus.model.*;
import com.salesforce.pyplyn.client.AbstractRemoteClient;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Argus client implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ArgusClient extends AbstractRemoteClient<ArgusService> {
    private static final Logger logger = LoggerFactory.getLogger(ArgusClient.class);
    private final AbstractConnector connector;
    private String cookie;

    /* Http client timeouts (in seconds) */
    private static final long DEFAULT_CONNECT_TIMEOUT = 60L;
    private static final long DEFAULT_READ_TIMEOUT = 300L;
    private static final long DEFAULT_WRITE_TIMEOUT = 60L;


    /**
     * Default simplified constructor that specifies connection param defaults
     *
     * @param connector The Argus endpoint to use in all the calls made by this collector
     */
    public ArgusClient(AbstractConnector connector) {
        this(connector, ArgusService.class, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, DEFAULT_WRITE_TIMEOUT);
    }

    /**
     * Class constructor that allows setting connection params
     */
    public ArgusClient(AbstractConnector connector, Class<ArgusService> cls, Long connectTimeout, Long readTimeout, Long writeTimeout) {
        super(connector, cls, connectTimeout, readTimeout, writeTimeout);
        this.connector = connector;
    }

    /**
     * Authenticates the session with the Argus endpoint
     *   and stores the session cookie, for future calls
     *
     * @throws UnauthorizedException if cannot authenticate
     */
    @Override
    public boolean auth() throws UnauthorizedException {
        return Optional.ofNullable(executeAndRetrieveHeaders(svc().auth(new AuthRequest(connector.username(), connector.password()))))
                .map(headers -> {
                    logger.info("Successfully logged in");
                    storeAuthenticationCookie(headers.get("Set-Cookie"));
                    return Boolean.TRUE;
                })
                .orElse(Boolean.FALSE);
    }

    /**
     * Returns true if a cookie was previously stored
     * <p/>
     * <p/>Does not guarantee that the session is still valid!
     */
    @Override
    public boolean isAuthenticated() {
        return nonNull(cookie);
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
        return executeAndRetrieveBody(svc().getMetrics(cookie, expressions), null);
    }

    /**
     * Create a new alert
     *
     * @throws IllegalArgumentException if null alert was passed
     */
    public AlertObject createAlert(AlertObject alert) throws UnauthorizedException {
        Preconditions.checkNotNull(alert, "Alert should not be null");
        return executeAndRetrieveBody(svc().createAlert(cookie, alert), null);
    }

    /**
     * Update an existing alert
     *
     * @throws IllegalArgumentException if null alert was passed
     */
    public AlertObject updateAlert(AlertObject alert) throws UnauthorizedException {
        Preconditions.checkNotNull(alert, "Alert should not be null");
        return executeAndRetrieveBody(svc().updateAlert(cookie, alert.id(), alert), null);
    }

    /**
     * Get all alerts owned by the target user
     *
     * @throws IllegalArgumentException if null owner name was passed
     */
    public List<AlertObject> loadAlertsByOwner(String ownerName) throws UnauthorizedException {
        Preconditions.checkNotNull(ownerName, "Owner name should not be null");
        return executeAndRetrieveBody(svc().getAlertsByOwner(cookie, ownerName), Collections.emptyList());
    }

    /**
     * Delete an existing alert by ID
     *
     * @throws IllegalArgumentException if null alertId was passed
     */
    public void deleteAlert(long alertId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        executeAndRetrieveBody(svc().deleteAlert(cookie, alertId), null);
    }


    /* Trigger operations. */

    /**
     * Load all triggers associated with an alert ID
     *
     * @throws IllegalArgumentException if null alertId was passed
     */
    public List<TriggerObject> loadTriggersForAlert(long alertId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        return executeAndRetrieveBody(svc().getTriggersForAlert(cookie, alertId), Collections.emptyList());
    }

    /**
     * Create a new trigger tied to an alert
     *
     * @throws IllegalArgumentException if null alertId or trigger were passed
     */
    public List<TriggerObject> createTrigger(long alertId, TriggerObject trigger) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(trigger, "Trigger should not be null");
        return executeAndRetrieveBody(svc().createTrigger(cookie, alertId, trigger), Collections.emptyList());
    }

    /**
     * Update an existing trigger
     *
     * @throws IllegalArgumentException if null alertId or trigger were passed
     */
    public TriggerObject updateTrigger(long alertId, TriggerObject trigger) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(trigger, "Trigger should not be null");
        return executeAndRetrieveBody(svc().updateTrigger(cookie, alertId, trigger.id(), trigger), null);
    }

    /**
     * Delete a trigger
     *
     * @throws IllegalArgumentException if null alertId or trigger were passed
     */
    public void deleteTrigger(long alertId, long triggerId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(triggerId, "Trigger id should not be null");
        executeAndRetrieveBody(svc().deleteTrigger(cookie, alertId, triggerId), null);
    }


    /* Notification operations. */

    /**
     * Load an existing notification
     *
     * @throws IllegalArgumentException if null alertId or notificationId were passed
     */
    public List<NotificationObject> getNotification(long alertId, long notificationId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(notificationId, "Notification id should not be null");
        return executeAndRetrieveBody(svc().getNotificationsForAlert(cookie, alertId), Collections.emptyList());
    }

    /**
     * Load existing notifications tied to an alert
     *
     * @throws IllegalArgumentException if null alertId was passed
     */
    public List<NotificationObject> getNotificationsForAlert(long alertId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        return executeAndRetrieveBody(svc().getNotificationsForAlert(cookie, alertId), Collections.emptyList());
    }

    /**
     * Create a new notification
     *
     * @throws IllegalArgumentException if null alertId or notificationId were passed
     */
    public List<NotificationObject> createNotification(long alertId, NotificationObject notification) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(notification, "Notification should not be null");
        return executeAndRetrieveBody(svc().createNotification(cookie, alertId, notification), Collections.emptyList());
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
        return executeAndRetrieveBody(svc().updateNotification(cookie, alertId, notificationId, notification), null);
    }

    /**
     * Delete a notification
     *
     * @throws IllegalArgumentException if null alertId or notificationId were passed
     */
    public void deleteNotification(long alertId, long notificationId) throws UnauthorizedException {
        Preconditions.checkNotNull(alertId, "Alert id should not be null");
        Preconditions.checkNotNull(notificationId, "Notification id should not be null");
        executeAndRetrieveBody(svc().deleteNotification(cookie, alertId, notificationId), null);
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
        return executeAndRetrieveBody(svc().attachTriggerToNotification(cookie, alertId, notificationId, triggerId), null);
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
        executeAndRetrieveBody(svc().removeTriggerFromNotification(cookie, alertId, notificationId, triggerId), null);
    }

    /**
     * Load all dashboards available in Argus available to this user
     */
    public List<DashboardObject> getAllDashboards() throws UnauthorizedException {
        return executeAndRetrieveBody(svc().getAllDashboards(cookie), Collections.emptyList());
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
        List<DashboardObject> result = executeAndRetrieveBody(svc().getDashboardByName(cookie, owner, dashboardName), null);
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
        return executeAndRetrieveBody(svc().createDashboard(cookie, dashboard), null);
    }

    /**
     * Update the existing dashboard with the specified ID
     *
     * @throws IllegalArgumentException if null dashboardId or dashboard were passed
     */
    public DashboardObject updateDashboard(long dashboardId, DashboardObject dashboard) throws UnauthorizedException {
        Preconditions.checkNotNull(dashboardId, "Dashboard id should not be null");
        Preconditions.checkNotNull(dashboard, "Dashboard should not be null");
        return executeAndRetrieveBody(svc().updateDashboard(cookie, dashboardId, dashboard), null);
    }

    /**
     * Delete the dashboard with the specified ID
     *
     * @throws IllegalArgumentException if null dashboardId or dashboard were passed
     */
    public void deleteDashboard(long dashboardId) throws UnauthorizedException {
        Preconditions.checkNotNull(dashboardId, "Dashboard id should not be null");
        executeAndRetrieveBody(svc().deleteDashboard(cookie, dashboardId), null);
    }

    /**
     * Load the specified dashboard that is either public or owned by the current user
     *
     * @param dashboardId The id of the dashboard to return
     * @throws IllegalArgumentException if null dashboardId was passed
     */
    public DashboardObject getDashboardById(long dashboardId) throws UnauthorizedException {
        Preconditions.checkNotNull(dashboardId, "Dashboard id should not be null");
        return executeAndRetrieveBody(svc().getDashboardById(cookie, dashboardId), null);
    }

    /**
     * Extracts the cookie from the response header and memoizes it into the current object
     */
    void storeAuthenticationCookie(String rawCookie) {
        this.cookie = rawCookie.substring(0, rawCookie.indexOf(';'));
    }
}
