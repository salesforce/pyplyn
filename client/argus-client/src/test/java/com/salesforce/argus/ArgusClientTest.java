/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus;

import com.salesforce.argus.model.*;
import com.salesforce.argus.model.builder.*;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * ArgusCollector test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ArgusClientTest {
	private static final long LONG_ID = 1L;

    @Mock
	private ArgusService svc;

    @Mock
    private AbstractConnector connector;

    ArgusClient argus;
	Request request;


	@BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        doReturn("http://localhost/").when(connector).endpoint();

        argus = spy(new ArgusClient(connector));
        doReturn(svc).when(argus).svc(); // FindBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT - IGNORE

		// build a dummy request object
		Request.Builder builder = new Request.Builder();
		builder.url("http://tests/");
		builder.method("MOCK", null);
		request = builder.build();
	}

    @Test
    public void auth() throws Exception {
        // ARRANGE
		Headers headers = Headers.of("Set-Cookie", "expected;");
        Response<ResponseBody> response = Response.success(mock(ResponseBody.class), headers);

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = (Call<ResponseBody>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).auth(any());

        // ACT
        argus.auth();

        // ASSERT
		assertThat(argus.isAuthenticated(), equalTo(true));
        verify(argus).storeAuthenticationCookie("expected;");
    }

	@Test
	public void failedAuth() throws Exception {
		// ARRANGE
		ResponseBody fail = ResponseBody.create(MediaType.parse(""), "FAIL");
		Response<String> failedResponse = Response.error(400, fail);

		@SuppressWarnings("unchecked")
		Call<ResponseBody> responseCall = (Call<ResponseBody>)mock(Call.class);
		doReturn(failedResponse).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).auth(any());

		// ACT
		boolean authenticationResult = argus.auth();

		// ASSERT
		assertThat(authenticationResult, equalTo(false));
		assertThat(argus.isAuthenticated(), equalTo(false));
		verify(svc, times(1)).auth(any());
		verify(argus, times(0)).storeAuthenticationCookie(any());
	}

    @Test
    public void getMetrics() throws Exception {
        // ARRANGE
        MetricResponse expectedMetric = new MetricResponseBuilder().withMetric("metric").build();
        Response<List<MetricResponse>> response = Response.success(Collections.singletonList(expectedMetric));

        @SuppressWarnings("unchecked")
        Call<List<MetricResponse>> responseCall = (Call<List<MetricResponse>>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getMetrics(any(), any());

        // ACT
        List<MetricResponse> getMetrics = argus.getMetrics(Collections.singletonList("metric"));

        // ASSERT
        assertThat(getMetrics, contains(expectedMetric));
        assertThat(expectedMetric.datapoints(), notNullValue());
        assertThat(expectedMetric.datapoints().entrySet(), empty()); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
        assertThat(expectedMetric.metric(), is("metric"));
    }
    
    @Test
    public void createAlert() throws Exception {
    	// ARRANGE
    	String name = "cceDevils";
    	long dateTime = LONG_ID;
    	String cronExpr = "0 15 10 * * ? *";
    	AlertObject alertObject = new AlertObjectBuilder().withName(name).withCreatedDate(dateTime).withCronEntry(cronExpr).build();
    	Response<AlertObject>response = Response.success(alertObject);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<AlertObject>responseCall = (Call<AlertObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).createAlert(any(), any());
    	
    	// ASSERT
    	AlertObject alert = argus.createAlert(alertObject);
    	assertThat(alert.name(), is(name));
    	assertThat(alert.createdDate(), is(dateTime)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	assertThat(alert.cronEntry(), is(cronExpr));
    }
    
    @Test
    public void updateAlert() throws Exception {
    	// ARRANGE
    	long id = LONG_ID;
    	AlertObject alertObject = new AlertObjectBuilder().withId(id).build();
    	Response<AlertObject>response = Response.success(alertObject);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<AlertObject>responseCall = (Call<AlertObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).updateAlert(any(), anyLong(), any());
    	
    	// ASSERT
    	AlertObject alert = argus.updateAlert(alertObject);
    	assertThat(alert.id(), is(id)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }
    
    @Test
    public void loadAlertsByOwner() throws Exception {
    	// ARRANGE
    	String ownerName = "cceDevils";
    	AlertObject alert = new AlertObjectBuilder().withOwnerName(ownerName).build();
    	Response<List<AlertObject>> response = Response.success(Collections.singletonList(alert));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
        Call<List<AlertObject>> responseCall = (Call<List<AlertObject>>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getAlertsByOwner(any(), anyString());
        
        // ASSERT
        List<AlertObject> alerts = argus.loadAlertsByOwner(ownerName);
    	assertThat(alerts.get(0).ownerName(), is(ownerName));
    }
    
    @Test
    public void deleteAlert() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	Response<Void>response = Response.success((new Void[1])[0]);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Void>responseCall = (Call<Void>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).deleteAlert(any(), anyLong());
    	
    	// ASSERT
    	argus.deleteAlert(alertId);
    	verify(svc).deleteAlert(null, alertId);
    	
    }
    
    @Test
    public void loadTriggersForAlert() throws Exception {
    	// ARRANGE
    	String triggerName = "cceDevilsTrigger";
    	long id = LONG_ID;
    	TriggerObject triggerObj = new TriggerObjectBuilder().withName(triggerName).withId(id).build();
    	Response<List<TriggerObject>> response = Response.success(Collections.singletonList(triggerObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<List<TriggerObject>>responseCall = (Call<List<TriggerObject>>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getTriggersForAlert(any(), anyLong());
    	
    	// ASSERT
    	List<TriggerObject>triggerList = argus.loadTriggersForAlert(id);
    	assertThat(triggerList.get(0).name(), is(triggerName));
    	assertThat(triggerList.get(0).id(), is(id)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	
    }
    
    @Test
    public void createTrigger() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	String triggerName = "cceDevilsTrigger";
    	TriggerObject triggerObj = new TriggerObjectBuilder().withName(triggerName).withAlertId(alertId).build();
    	Response<List<TriggerObject>>response = Response.success(Collections.singletonList(triggerObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<List<TriggerObject>>responseCall = (Call<List<TriggerObject>>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).createTrigger(any(), anyLong(), any());
    	
    	// ASSERT
    	List<TriggerObject>triggerList = argus.createTrigger(alertId, triggerObj);
    	assertThat(triggerList.get(0).name(), is(triggerName));
    	assertThat(triggerList.get(0).alertId(), is(alertId)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }
    
    @Test
    public void updateTrigger() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	long triggerId = LONG_ID;
    	String triggerName = "cceDevilsTrigger";
    	TriggerObject triggerObj = new TriggerObjectBuilder().withName(triggerName).withAlertId(alertId).withId(triggerId).build(); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	Response<TriggerObject>response = Response.success(triggerObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<TriggerObject>responseCall = (Call<TriggerObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).updateTrigger(any(), anyLong(), anyLong(), any());
    	
    	// ASSERT
    	TriggerObject triggerList = argus.updateTrigger(alertId, triggerObj);
    	assertThat(triggerList.name(), is(triggerName));
    	assertThat(triggerList.alertId(), is(alertId));
    }
    
    @Test
    public void deleteTrigger() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	long triggerId = LONG_ID;
    	Response<Void>response = Response.success((new Void[1])[0]);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Void>responseCall = (Call<Void>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).deleteTrigger(any(), anyLong(), anyLong());
    	
    	// ASSERT
    	argus.deleteTrigger(alertId, triggerId);
    	verify(svc).deleteTrigger(null, alertId, triggerId);
    }
    
    
    @Test
    public void getNotification() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	long notificationId = LONG_ID;
    	NotificationObject notificationObj = new NotificationObjectBuilder().withAlertId(alertId).withId(notificationId).build(); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	Response<List<NotificationObject>> response = Response.success(Collections.singletonList(notificationObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = (Call<NotificationObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getNotificationsForAlert(any(), anyLong());
    	
    	// ASSERT
    	List<NotificationObject> notificationList = argus.getNotification(alertId, notificationId);
    	assertThat(notificationList.get(0).alertId(), is(alertId));
    	assertThat(notificationList.get(0).id(), is(notificationId)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }
    
    @Test
    public void getNotificationsForAlert() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	NotificationObject notificationObj = new NotificationObjectBuilder().withAlertId(alertId).build();
    	Response<List<NotificationObject>> response = Response.success(Collections.singletonList(notificationObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = (Call<NotificationObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getNotificationsForAlert(any(), anyLong());
    	
    	// ASSERT
    	List<NotificationObject> notificationList = argus.getNotificationsForAlert(alertId);
    	assertThat(notificationList.get(0).alertId(), is(alertId)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }
    
    @Test
    public void createNotification() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	String name = "cceDevils";
    	NotificationObject notificationObj = new NotificationObjectBuilder().withAlertId(alertId).withName(name).build();
    	Response<List<NotificationObject>> response = Response.success(Collections.singletonList(notificationObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = (Call<NotificationObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).createNotification(any(), anyLong(), any());
    	
    	// ASSERT
    	List<NotificationObject> notificationList = argus.createNotification(alertId, notificationObj);
    	assertThat(notificationList.get(0).alertId(), is(alertId)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	assertThat(notificationList.get(0).name(), is(name));
    }
    
    @Test
    public void updateNotification() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	long notificationId = LONG_ID;
    	String name = "cceDevils";
    	NotificationObject notificationObj = new NotificationObjectBuilder().withAlertId(alertId).withId(notificationId).withName(name).build(); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	Response<NotificationObject> response = Response.success(notificationObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = (Call<NotificationObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).updateNotification(any(), anyLong(), anyLong(), any());
    	
    	// ASSERT
    	NotificationObject notification = argus.updateNotification(alertId, notificationId, notificationObj);
    	assertThat(notification.alertId(), is(alertId));
    	assertThat(notification.name(), is(name));
    	assertThat(notification.id(), is(notificationId)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }
    
    @Test
    public void deleteNotification() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	long notificationId = LONG_ID;
    	String name = "cceDevils";
    	NotificationObject notificationObj = new NotificationObjectBuilder().withAlertId(alertId).withId(notificationId).withName(name).build(); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	Response<NotificationObject> response = Response.success(notificationObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = (Call<NotificationObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).deleteNotification(any(), anyLong(), anyLong());
    	
    	// ASSERT
    	argus.deleteNotification(alertId, notificationId);
    	verify(svc).deleteNotification(null, alertId, notificationId);
    }
    
    @Test
    public void attachTriggerToNotification() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	Long notificationIds[] = new Long[1];
    	notificationIds[0] = LONG_ID;
    	long triggerId = LONG_ID;
    	TriggerObject triggerObj = new TriggerObjectBuilder().withAlertId(alertId).withId(triggerId).withNotificationIds(notificationIds).build(); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	Response<TriggerObject> response = Response.success(triggerObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = (Call<NotificationObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).attachTriggerToNotification(any(), anyLong(), anyLong(), anyLong());
    	
    	// ASSERT
    	TriggerObject notification = argus.attachTriggerToNotification(alertId, notificationIds[0], triggerId);
    	assertThat(notification.alertId(), is(alertId)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	assertThat(notification.notificationIds()[0], is(notificationIds[0]));
    	assertThat(notification.id(), is(triggerId));
    }
    
    @Test
    public void removeTriggerFromNotification() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	Long notificationIds[] = new Long[1];
    	notificationIds[0] = LONG_ID;
    	long triggerId = LONG_ID;
    	TriggerObject triggerObj = new TriggerObjectBuilder().withAlertId(alertId).withId(triggerId).withNotificationIds(notificationIds).build(); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    	Response<TriggerObject> response = Response.success(triggerObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = (Call<NotificationObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).removeTriggerFromNotification(any(), anyLong(), anyLong(), anyLong());
    	
    	// ASSERT
    	argus.removeTriggerFromNotification(alertId, notificationIds[0], triggerId);
    	verify(svc).removeTriggerFromNotification(null, alertId, notificationIds[0], triggerId);
    }
    
    @Test
    public void getAllDashboards() throws Exception {
    	// ARRANGE
    	String name = "cceDevils";
    	DashboardObject dashboardObj = new DashboardObjectBuilder().withName(name).build();
    	Response<List<DashboardObject>> response = Response.success(Collections.singletonList(dashboardObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = (Call<NotificationObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getAllDashboards(any());
    	
    	// ASSERT
    	List<DashboardObject> dashboards = argus.getAllDashboards();
    	assertThat(dashboards.get(0).name(), is(name));
    }
    
    @Test
    public void getDashboardByName() throws Exception {
    	// ARRANGE
    	String name = "cceDevilsDashboard";
    	String owner = "cceDevils";
    	DashboardObject dashboardObj = new DashboardObjectBuilder().withName(name).withOwnerName(owner).build();
    	Response<List<DashboardObject>> response = Response.success(Collections.singletonList(dashboardObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<List<DashboardObject>>responseCall = (Call<List<DashboardObject>>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getDashboardByName(any(), anyString(), anyString());
    	
    	// ASSERT
    	DashboardObject dashboard = argus.getDashboardByName(owner, name);
    	assertThat(dashboard.name(), is(name));
    	assertThat(dashboard.ownerName(), is(owner));
    }
    
    @Test
    public void createDashboard() throws Exception {
    	// ARRANGE
    	String name = "cceDevilsDashboard";
    	String owner = "cceDevils";
    	DashboardObject dashboardObj = new DashboardObjectBuilder().withName(name).withOwnerName(owner).build();
    	Response<DashboardObject> response = Response.success(dashboardObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<DashboardObject>responseCall = (Call<DashboardObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).createDashboard(any(), any());
    	
    	// ASSERT
    	DashboardObject dashboard = argus.createDashboard(dashboardObj);
    	assertThat(dashboard.name(), is(name));
    	assertThat(dashboard.ownerName(), is(owner));
    }
    
    @Test
    public void updateDashboard() throws Exception {
    	// ARRANGE
    	String name = "cceDevilsDashboard";
    	String owner = "cceDevils";
    	long dashboardId = LONG_ID;
    	DashboardObject dashboardObj = new DashboardObjectBuilder().withName(name).withOwnerName(owner).withId(dashboardId).build();
    	Response<DashboardObject> response = Response.success(dashboardObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<DashboardObject>responseCall = (Call<DashboardObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).updateDashboard(any(), anyLong(), any());
    	
    	// ASSERT
    	DashboardObject dashboard = argus.updateDashboard(dashboardId, dashboardObj);
    	assertThat(dashboard.name(), is(name));
    	assertThat(dashboard.ownerName(), is(owner));
    	assertThat(dashboard.id(), is(dashboardId)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }
    
    @Test
    public void deleteDashboard() throws Exception {
    	// ARRANGE
    	String name = "cceDevilsDashboard";
    	String owner = "cceDevils";
    	long dashboardId = LONG_ID;
    	DashboardObject dashboardObj = new DashboardObjectBuilder().withName(name).withOwnerName(owner).withId(dashboardId).build();
    	Response<DashboardObject> response = Response.success(dashboardObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<DashboardObject>responseCall = (Call<DashboardObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).deleteDashboard(any(), anyLong());
    	
    	// ASSERT
    	argus.deleteDashboard(dashboardId);
    	verify(svc).deleteDashboard(null, dashboardId);
    }
    
    @Test
    public void getDashboardById() throws Exception {
    	// ARRANGE
    	long dashboardId = LONG_ID;
    	DashboardObject dashboardObj = new DashboardObjectBuilder().withId(dashboardId).build();
    	Response<DashboardObject> response = Response.success(dashboardObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<DashboardObject>responseCall = (Call<DashboardObject>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getDashboardById(any(), anyLong());
    	
    	// ASSERT
    	DashboardObject dashboard = argus.getDashboardById(dashboardId);
    	assertThat(dashboard.id(), is(dashboardId)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }

    @Test(expectedExceptions = UnauthorizedException.class)
    public void failingAuth() throws Exception {
        // ARRANGE
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "error");
        Response<ResponseBody> response = Response.error(401, errorBody);

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = (Call<ResponseBody>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).auth(any());

        // ACT
        argus.auth();
        verify(svc).auth(null);
    }

    @Test
    public void failingAuthWithOtherError() throws Exception {
        // ARRANGE
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "error");
        Response<ResponseBody> response = Response.error(405, errorBody);

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = (Call<ResponseBody>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).auth(any());

        // ACT
        boolean authResponse = argus.auth();

        // ASSERT
        assertThat(authResponse, is(false));
    }
}
