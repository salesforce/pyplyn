/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus;

import java.util.List;

import com.salesforce.argus.model.AlertObject;
import com.salesforce.argus.model.AuthRequest;
import com.salesforce.argus.model.AuthToken;
import com.salesforce.argus.model.DashboardObject;
import com.salesforce.argus.model.MetricCollectionResponse;
import com.salesforce.argus.model.MetricResponse;
import com.salesforce.argus.model.NotificationObject;
import com.salesforce.argus.model.TriggerObject;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Argus REST service class
 * <p/>
 * Note: operations are not commented in this interface, since this is a {@link Retrofit} service.
 *   See {@link ArgusClient} for an explanation of what each method does.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface ArgusService {
    /* Authorization header */
    String AUTHORIZATION = "Authorization";


    /* Authentication */

    @POST("v2/auth/login")
    @Headers("Content-Type: application/json")
    Call<AuthToken> login(@Body AuthRequest request);

    @POST("v2/auth/token/refresh")
    @Headers("Content-Type: application/json")
    Call<AuthToken> refresh(@Body AuthToken tokens);


    /* Metrics operations. */

    @GET("metrics")
    Call<List<MetricResponse>> getMetrics(@Header(AUTHORIZATION) String authorization, @Query("expression") List<String> expressions);

    @POST("collection/metrics")
    @Headers("Content-Type: application/json")
    Call<MetricCollectionResponse> postMetrics(@Header(AUTHORIZATION) String authorization, @Body List<MetricResponse> metrics);


    /* Dashboard operations. */

    @GET("dashboards")
    Call<List<DashboardObject>> getAllDashboards(@Header(AUTHORIZATION) String authorization);

    @GET("dashboards/meta")
    Call<List<DashboardObject>> getAllDashboardMetadata(@Header(AUTHORIZATION) String authorization);

    @GET("dashboards")
    Call<List<DashboardObject>> getDashboardByName(@Header(AUTHORIZATION) String authorization, @Query("owner") String owner, @Query("dashboardName") String dashboardName);

    @GET("dashboards/{dashboard_id}")
    Call<DashboardObject> getDashboardById(@Header(AUTHORIZATION) String authorization, @Path("dashboard_id") long dashboardId);

    @POST("dashboards")
    Call<DashboardObject> createDashboard(@Header(AUTHORIZATION) String authorization, @Body DashboardObject dashboard);

    @PUT("dashboards/{dashboard_id}")
    Call<DashboardObject> updateDashboard(@Header(AUTHORIZATION) String authorization, @Path("dashboard_id") long dashboardId, @Body DashboardObject dashboard);

    @DELETE("dashboards/{dashboard_id}")
    Call<Void> deleteDashboard(@Header(AUTHORIZATION) String authorization, @Path("dashboard_id") long dashboardId);


    /* Alert operations. */

    @GET("alerts")
    Call<List<AlertObject>> getAllAlerts(@Header(AUTHORIZATION) String authorization);

    @GET("alerts")
    Call<List<AlertObject>> getAlertsByOwner(@Header(AUTHORIZATION) String authorization, @Query("ownername") String ownername);
    
    @GET("alerts/meta")
    Call<List<AlertObject>> getAlertMetadataByOwner(@Header(AUTHORIZATION) String authorization, @Query("ownername") String ownername);

    @POST("alerts")
    @Headers("Content-Type: application/json")
    Call<AlertObject> createAlert(@Header(AUTHORIZATION) String authorization, @Body AlertObject alert);

    @GET("alerts/{id}")
    Call<AlertObject> getAlert(@Header(AUTHORIZATION) String authorization, @Path("id") long id);
    
    @PUT("alerts/{id}")
    Call<AlertObject> updateAlert(@Header(AUTHORIZATION) String authorization, @Path("id") long id, @Body AlertObject alert);

    @DELETE("alerts/{id}")
    Call<Void> deleteAlert(@Header(AUTHORIZATION) String authorization, @Path("id") long id);


    /* Trigger operations. */

    @GET("alerts/{alert_id}/triggers")
    Call<List<TriggerObject>> getTriggersForAlert(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId);

    @POST("alerts/{alert_id}/triggers")
    Call<List<TriggerObject>> createTrigger(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId, @Body TriggerObject trigger);

    @PUT("alerts/{alert_id}/triggers/{trigger_id}")
    Call<TriggerObject> updateTrigger(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId, @Path("trigger_id") long triggerId, @Body TriggerObject trigger);

    @DELETE("alerts/{alert_id}/triggers/{trigger_id}")
    Call<Void> deleteTrigger(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId, @Path("trigger_id") long triggerId);


    /* Notification operations. */

    @GET("alerts/{alert_id}/notifications/{notification_id}")
    Call<NotificationObject> getNotification(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId, @Path("notification_id") long notificationId);

    @GET("alerts/{alert_id}/notifications")
    Call<List<NotificationObject>> getNotificationsForAlert(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId);

    @POST("alerts/{alert_id}/notifications")
    Call<List<NotificationObject>> createNotification(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId, @Body NotificationObject notification);

    @PUT("alerts/{alert_id}/notifications/{notification_id}")
    Call<NotificationObject> updateNotification(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId, @Path("notification_id") long notificationId, @Body NotificationObject notification);

    @DELETE("alerts/{alert_id}/notifications/{notification_id}")
    Call<Void> deleteNotification(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId, @Path("notification_id") long notificationId);

    @POST("alerts/{alert_id}/notifications/{notification_id}/triggers/{trigger_id}")
    Call<TriggerObject> attachTriggerToNotification(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId, @Path("notification_id") long notificationId, @Path("trigger_id") long triggerId);

    @DELETE("alerts/{alert_id}/notifications/{notification_id}/triggers/{trigger_id}")
    Call<Void> removeTriggerFromNotification(@Header(AUTHORIZATION) String authorization, @Path("alert_id") long alertId, @Path("notification_id") long notificationId, @Path("trigger_id") long triggerId);
}
