/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus;

import com.salesforce.argus.model.*;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.*;

import java.util.List;

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

    /* Authentication */

    @POST("auth/login")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> auth(@Body AuthRequest request);


    /* Metrics operations. */

    @GET("metrics")
    Call<List<MetricResponse>> getMetrics(@Header("Cookie") String cookie, @Query("expression") List<String> expressions);


    /* Dashboard operations. */

    @GET("dashboards")
    Call<List<DashboardObject>> getAllDashboards(@Header("Cookie") String cookie);

    @GET("dashboards")
    Call<List<DashboardObject>> getDashboardByName(@Header("Cookie") String cookie, @Query("owner") String owner, @Query("dashboardName") String dashboardName);

    @GET("dashboards/{dashboard_id}")
    Call<DashboardObject> getDashboardById(@Header("Cookie") String cookie, @Path("dashboard_id") long dashboardId);

    @POST("dashboards")
    Call<DashboardObject> createDashboard(@Header("Cookie") String cookie, @Body DashboardObject dashboard);

    @PUT("dashboards/{dashboard_id}")
    Call<DashboardObject> updateDashboard(@Header("Cookie") String cookie, @Path("dashboard_id") long dashboardId, @Body DashboardObject dashboard);

    @DELETE("dashboards/{dashboard_id}")
    Call<Void> deleteDashboard(@Header("Cookie") String cookie, @Path("dashboard_id") long dashboardId);


    /* Alert operations. */

    @GET("alerts")
    Call<List<AlertObject>> getAllAlerts(@Header("Cookie") String cookie);

    @GET("alerts")
    Call<List<AlertObject>> getAlertsByOwner(@Header("Cookie") String cookie, @Query("ownername") String ownername);

    @POST("alerts")
    @Headers("Content-Type: application/json")
    Call<AlertObject> createAlert(@Header("Cookie") String cookie, @Body AlertObject alert);

    @PUT("alerts/{id}")
    Call<AlertObject> updateAlert(@Header("Cookie") String cookie, @Path("id") long id, @Body AlertObject alert);

    @DELETE("alerts/{id}")
    Call<Void> deleteAlert(@Header("Cookie") String cookie, @Path("id") long id);


    /* Trigger operations. */

    @GET("alerts/{alert_id}/triggers")
    Call<List<TriggerObject>> getTriggersForAlert(@Header("Cookie") String cookie, @Path("alert_id") long alertId);

    @POST("alerts/{alert_id}/triggers")
    Call<List<TriggerObject>> createTrigger(@Header("Cookie") String cookie, @Path("alert_id") long alertId, @Body TriggerObject trigger);

    @PUT("alerts/{alert_id}/triggers/{trigger_id}")
    Call<TriggerObject> updateTrigger(@Header("Cookie") String cookie, @Path("alert_id") long alertId, @Path("trigger_id") long triggerId, @Body TriggerObject trigger);

    @DELETE("alerts/{alert_id}/triggers/{trigger_id}")
    Call<Void> deleteTrigger(@Header("Cookie") String cookie, @Path("alert_id") long alertId, @Path("trigger_id") long triggerId);


    /* Notification operations. */

    @GET("alerts/{alert_id}/notifications/{notification_id}")
    Call<NotificationObject> getNotification(@Header("Cookie") String cookie, @Path("alert_id") long alertId, @Path("notification_id") long notificationId);

    @GET("alerts/{alert_id}/notifications")
    Call<List<NotificationObject>> getNotificationsForAlert(@Header("Cookie") String cookie, @Path("alert_id") long alertId);

    @POST("alerts/{alert_id}/notifications")
    Call<List<NotificationObject>> createNotification(@Header("Cookie") String cookie, @Path("alert_id") long alertId, @Body NotificationObject notification);

    @PUT("alerts/{alert_id}/notifications/{notification_id}")
    Call<NotificationObject> updateNotification(@Header("Cookie") String cookie, @Path("alert_id") long alertId, @Path("notification_id") long notificationId, @Body NotificationObject notification);

    @DELETE("alerts/{alert_id}/notifications/{notification_id}")
    Call<Void> deleteNotification(@Header("Cookie") String cookie, @Path("alert_id") long alertId, @Path("notification_id") long notificationId);

    @POST("alerts/{alert_id}/notifications/{notification_id}/triggers/{trigger_id}")
    Call<TriggerObject> attachTriggerToNotification(@Header("Cookie") String cookie, @Path("alert_id") long alertId, @Path("notification_id") long notificationId, @Path("trigger_id") long triggerId);

    @DELETE("alerts/{alert_id}/notifications/{notification_id}/triggers/{trigger_id}")
    Call<Void> removeTriggerFromNotification(@Header("Cookie") String cookie, @Path("alert_id") long alertId, @Path("notification_id") long notificationId, @Path("trigger_id") long triggerId);
}
