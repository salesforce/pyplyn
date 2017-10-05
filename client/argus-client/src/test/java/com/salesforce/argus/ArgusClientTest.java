/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Iterables;
import com.salesforce.argus.model.*;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.Connector;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * ArgusCollector test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ArgusClientTest {
	private static final long LONG_ID = 1L;
	private static final List<Long> LIST_OF_LONG_ID = Collections.singletonList(LONG_ID);
	private static final String NAME = "name";
	private static final String OWNER_NAME = "owner";
	private static final String TRIGGER_NAME = "triggerName";
	private static final String AUTHORIZATION_HEADER = "Authorization: Bearer of_fake_tokens";

    @Mock
	private ArgusService svc;

    @Mock
    private Connector connector;

    private ArgusClient argus;
	private Request request;


	@BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
		doReturn("http://localhost/").when(connector).endpoint();
		doReturn("password".getBytes(Charset.defaultCharset())).when(connector).password();

        argus = spy(new ArgusClient(connector));
        doReturn(svc).when(argus).svc();

		// build a dummy request object
		Request.Builder builder = new Request.Builder();
		builder.url("http://tests/");
		builder.method("MOCK", null);
		request = builder.build();

		// mock auth
		doReturn(AUTHORIZATION_HEADER).when(argus).authorizationHeader();
	}

	@Test
	public void authenticateWithToken() throws Exception {
		// ARRANGE
		AuthToken token = ImmutableAuthToken.of("access".getBytes(Charset.defaultCharset()), "refresh".getBytes(Charset.defaultCharset()));

		Response<AuthToken> response = Response.success(token);

		@SuppressWarnings("unchecked")
		Call<AuthToken> responseCall = mock(Call.class);
		doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).refresh(any());

		doReturn(null).when(argus).authorizationHeader();

		// ACT
		argus.authenticate();

		// ASSERT
		assertThat(argus.isAuthenticated(), equalTo(true));
		verify(svc, times(0)).login(any()); // did not log in with user/pass
		verify(svc).refresh(any()); // refreshed the token

		// should be called twice, as the method recursively calls itself
		verify(argus, times(2)).auth();
	}

	@Test
	public void authenticateWithUserAndPass() throws Exception {
		// ARRANGE
		doReturn("username").when(connector).username();

		AuthToken token = ImmutableAuthToken.of("access".getBytes(Charset.defaultCharset()), "refresh".getBytes(Charset.defaultCharset()));
		Response<AuthToken> response = Response.success(token);

		@SuppressWarnings("unchecked")
		Call<AuthToken> responseCall = mock(Call.class);
		doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).login(any());

		doReturn(null).when(argus).authorizationHeader();

		// ACT
		argus.authenticate();

		// ASSERT
		assertThat(argus.isAuthenticated(), equalTo(true));
		verify(svc).login(any()); // logged in with user/pass
		verify(svc, times(0)).refresh(any()); // did not refresh the token

		// should be called only once
		verify(argus, times(1)).auth();
	}

	@Test
	public void reauthenticateIfAnOperationFailsWithAuthException() throws Exception {
		// ARRANGE
		// auth response
		AuthToken token = ImmutableAuthToken.of("access".getBytes(Charset.defaultCharset()), "refresh".getBytes(Charset.defaultCharset()));
		Response<AuthToken> authResponseToken = Response.success(token);

		// token response call
		@SuppressWarnings("unchecked")
		Call<AuthToken> authResponseCall = mock(Call.class);
		doReturn(authResponseToken).when(authResponseCall).execute();
		doReturn(request).when(authResponseCall).request();
		doReturn(authResponseCall).when(svc).refresh(any());

		// alert object response
		AlertObject alertObject = ImmutableAlertObject.builder().id(LONG_ID).build();
		Response<AlertObject> response = Response.success(alertObject);

		// authentication failure
		ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "error");
		Response<AlertObject> authFailedResponse = Response.error(401, errorBody);

		// alert call
		@SuppressWarnings("unchecked")
		Call<AlertObject> responseCall = mock(Call.class);
		doReturn(responseCall).when(responseCall).clone();
		doReturn(authFailedResponse, response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getAlert(any(), anyLong());


		// ACT
		AlertObject alert = argus.loadAlert(LONG_ID);


		// ASSERT
		verify(argus).resetAuth(); // cleared the old token
		verify(argus, times(2)).auth(); // called twice for refresh token auth
		verify(svc).refresh(any()); // refreshed the token
		assertThat(argus.isAuthenticated(), equalTo(true));
		assertThat(alert.id(), is(LONG_ID));
	}


	@Test
	public void failedAuthWithToken() throws Exception {
		// ARRANGE
		ResponseBody fail = ResponseBody.create(MediaType.parse(""), "FAIL");
		Response<String> failedResponse = Response.error(400, fail);

		doReturn(null).when(argus).authorizationHeader();

		@SuppressWarnings("unchecked")
		Call<ResponseBody> responseCall = mock(Call.class);
		doReturn(failedResponse).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).refresh(any());

		// ACT
		boolean authenticationResult = argus.authenticate();

		// ASSERT
		verify(argus, times(2)).auth();
		assertThat(authenticationResult, equalTo(false));
		assertThat(argus.isAuthenticated(), equalTo(false));
		verify(svc, times(1)).refresh(any());
	}

	@Test(expectedExceptions = UnauthorizedException.class)
	public void failedAuthWithUserAndPass() throws Exception {
		// ARRANGE
		doReturn("username").when(connector).username();

		ResponseBody fail = ResponseBody.create(MediaType.parse(""), "FAIL");
		Response<String> failedResponse = Response.error(401, fail);

		doReturn(null).when(argus).authorizationHeader();

		@SuppressWarnings("unchecked")
		Call<ResponseBody> responseCall = mock(Call.class);
		doReturn(failedResponse).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).login(any());

		// ACT
		try {
			argus.authenticate();

		// ASSERT
		} finally {
			assertThat(argus.isAuthenticated(), equalTo(false));
			verify(argus, times(1)).auth();
			verify(svc, times(1)).login(any());
		}
	}

	@Test(expectedExceptions = UnauthorizedException.class)
	public void failedAuthWithTokenThrowsException() throws Exception {
		// ARRANGE
		doReturn(null).when(argus).authorizationHeader();

		ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "error");
		Response<AuthToken> response = Response.error(401, errorBody);

		@SuppressWarnings("unchecked")
		Call<AuthToken> responseCall = mock(Call.class);
		doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).refresh(any());

		// ACT
		try {
			argus.authenticate();

		// ASSERT
		} finally {
			assertThat(argus.isAuthenticated(), equalTo(false));
			verify(svc, times(1)).refresh(any());
			verify(argus, times(2)).auth();
		}
	}

	@Test
	public void failingAuthWithOtherError() throws Exception {
		// ARRANGE
		ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "error");
		Response<AuthToken> response = Response.error(405, errorBody);

		@SuppressWarnings("unchecked")
		Call<AuthToken> responseCall = mock(Call.class);
		doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).refresh(any());

		// ACT
		boolean authResponse = argus.authenticate();

		// ASSERT
		assertThat(authResponse, is(false));
		verify(svc).refresh(any());
	}

	@Test
    public void getMetrics() throws Exception {
        // ARRANGE
        MetricResponse expectedMetric = ImmutableMetricResponse.builder().metric("metric").build();
        Response<List<MetricResponse>> response = Response.success(Collections.singletonList(expectedMetric));

        @SuppressWarnings("unchecked")
        Call<List<MetricResponse>> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getMetrics(any(), any());

        // ACT
        List<MetricResponse> getMetrics = argus.getMetrics(Collections.singletonList("metric"));

        // ASSERT
        assertThat(getMetrics, contains(expectedMetric));
        assertThat(expectedMetric.datapoints(), notNullValue());
        assertThat(expectedMetric.datapoints().entrySet(), empty());
        assertThat(expectedMetric.metric(), is("metric"));
    }
    
    @Test
    public void createAlert() throws Exception {
    	// ARRANGE
    	String name = "name";
    	long dateTime = LONG_ID;
    	String cronExpr = "0 15 10 * * ? *";

    	AlertObject alertObject = ImmutableAlertObject.builder().name(name).createdDate(dateTime).cronEntry(cronExpr).build();
    	Response<AlertObject>response = Response.success(alertObject);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<AlertObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).createAlert(any(), any());
    	
    	// ASSERT
    	AlertObject alert = argus.createAlert(alertObject);
    	assertThat(alert.name(), is(name));
    	assertThat(alert.createdDate(), is(dateTime));
    	assertThat(alert.cronEntry(), is(cronExpr));
    }
    
    @Test
    public void updateAlert() throws Exception {
    	// ARRANGE
    	long id = LONG_ID;
    	AlertObject alertObject = ImmutableAlertObject.builder().id(id).build();
    	Response<AlertObject>response = Response.success(alertObject);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<AlertObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).updateAlert(any(), anyLong(), any());
    	
    	// ASSERT
    	AlertObject alert = argus.updateAlert(alertObject);
    	assertThat(alert.id(), is(id));
    }
    
    @Test
    public void loadAlert() throws Exception {
        // ARRANGE
		AlertObject alertObject = ImmutableAlertObject.builder().id(LONG_ID).build();
		Response<AlertObject> response = Response.success(alertObject);

        // ACT
        @SuppressWarnings("unchecked")
        Call<AlertObject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getAlert(any(), anyLong());
        
        // ASSERT
        AlertObject alert = argus.loadAlert(LONG_ID);
        assertThat(alert.id(), is(LONG_ID));
    }

	@Test
	public void loadAllAlerts() throws Exception {
		// ARRANGE
		AlertObject alert = ImmutableAlertObject.builder().ownerName(OWNER_NAME).build();
		Response<List<AlertObject>> response = Response.success(Collections.singletonList(alert));

		// ACT
		@SuppressWarnings("unchecked")
		Call<List<AlertObject>> responseCall = (Call<List<AlertObject>>)mock(Call.class);
		doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getAllAlerts(any());

		// ASSERT
		List<AlertObject> alerts = argus.loadAllAlerts();
		assertThat(alerts, hasItem(alert));
	}

	@Test
	public void loadAlertsByOwner() throws Exception {
		// ARRANGE
		AlertObject alert = ImmutableAlertObject.builder().ownerName(OWNER_NAME).build();
		Response<List<AlertObject>> response = Response.success(Collections.singletonList(alert));

		// ACT
		@SuppressWarnings("unchecked")
		Call<List<AlertObject>> responseCall = (Call<List<AlertObject>>)mock(Call.class);
		doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getAlertsByOwner(any(), anyString());

		// ASSERT
		List<AlertObject> alerts = argus.loadAlertsByOwner(OWNER_NAME);
		assertThat(alerts.get(0).ownerName(), is(OWNER_NAME));
	}


	@Test
    public void loadAlertMetadataByOwner() throws Exception {
        // ARRANGE
		AlertObject alert = ImmutableAlertObject.builder().ownerName(OWNER_NAME).build();
        Response<List<AlertObject>> response = Response.success(Collections.singletonList(alert));
        
        // ACT
        @SuppressWarnings("unchecked")
        Call<List<AlertObject>> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getAlertMetadataByOwner(any(), anyString());
        
        // ASSERT
        List<AlertObject> alerts = argus.loadAlertsMetadataByOwner(OWNER_NAME);
        assertThat(alerts.get(0).ownerName(), is(OWNER_NAME));
    }
    
    @Test
    public void deleteAlert() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	Response<Void>response = Response.success((new Void[1])[0]);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Void>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).deleteAlert(any(), anyLong());
    	
    	// ASSERT
    	argus.deleteAlert(alertId);
    	verify(svc).deleteAlert(AUTHORIZATION_HEADER, alertId);
    	
    }
    
    @Test
    public void loadTriggersForAlert() throws Exception {
    	// ARRANGE
    	String triggerName = "triggerName";
    	long id = LONG_ID;
    	TriggerObject triggerObj = ImmutableTriggerObject.builder().name(triggerName).id(id).build();
    	Response<List<TriggerObject>> response = Response.success(Collections.singletonList(triggerObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<List<TriggerObject>>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getTriggersForAlert(any(), anyLong());
    	
    	// ASSERT
    	List<TriggerObject>triggerList = argus.loadTriggersForAlert(id);
    	assertThat(triggerList.get(0).name(), is(triggerName));
    	assertThat(triggerList.get(0).id(), is(id));
    	
    }
    
    @Test
    public void createTrigger() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	String triggerName = "triggerName";
    	TriggerObject triggerObj = ImmutableTriggerObject.builder().name(triggerName).alertId(alertId).build();
    	Response<List<TriggerObject>>response = Response.success(Collections.singletonList(triggerObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<List<TriggerObject>>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).createTrigger(any(), anyLong(), any());
    	
    	// ASSERT
    	List<TriggerObject>triggerList = argus.createTrigger(alertId, triggerObj);
    	assertThat(triggerList.get(0).name(), is(triggerName));
    	assertThat(triggerList.get(0).alertId(), is(alertId));
    }
    
    @Test
    public void updateTrigger() throws Exception {
    	// ARRANGE
    	TriggerObject triggerObj = ImmutableTriggerObject.builder().name(TRIGGER_NAME).alertId(LONG_ID).id(LONG_ID).build();
    	Response<TriggerObject>response = Response.success(triggerObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<TriggerObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).updateTrigger(any(), anyLong(), anyLong(), any());

    	// ASSERT
    	TriggerObject triggerList = argus.updateTrigger(LONG_ID, triggerObj);
    	assertThat(triggerList.name(), is(TRIGGER_NAME));
    	assertThat(triggerList.alertId(), is(LONG_ID));
    }
    
    @Test
    public void deleteTrigger() throws Exception {
    	// ARRANGE
    	Response<Void>response = Response.success((new Void[1])[0]);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Void>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).deleteTrigger(any(), anyLong(), anyLong());
    	
    	// ASSERT
    	argus.deleteTrigger(LONG_ID, LONG_ID);
    	verify(svc).deleteTrigger(AUTHORIZATION_HEADER, LONG_ID, LONG_ID);
    }
    
    
    @Test
    public void getNotification() throws Exception {
    	// ARRANGE
    	NotificationObject notificationObj = ImmutableNotificationObject.builder().alertId(LONG_ID).id(LONG_ID).build();
    	Response<NotificationObject> response = Response.success(notificationObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getNotification(any(), anyLong(), anyLong());
    	
    	// ASSERT
    	NotificationObject notificationList = argus.getNotification(LONG_ID, LONG_ID);
    	assertThat(notificationList.alertId(), is(LONG_ID));
    	assertThat(notificationList.id(), is(LONG_ID));
    }
    
    @Test
    public void getNotificationsForAlert() throws Exception {
    	// ARRANGE
    	long alertId = LONG_ID;
    	NotificationObject notificationObj = ImmutableNotificationObject.builder().alertId(alertId).build();
    	Response<List<NotificationObject>> response = Response.success(Collections.singletonList(notificationObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getNotificationsForAlert(any(), anyLong());
    	
    	// ASSERT
    	List<NotificationObject> notificationList = argus.getNotificationsForAlert(alertId);
    	assertThat(notificationList.get(0).alertId(), is(alertId));
    }
    
    @Test
    public void createNotification() throws Exception {
    	// ARRANGE
    	NotificationObject notificationObj = ImmutableNotificationObject.builder().alertId(LONG_ID).name(NAME).build();
    	Response<List<NotificationObject>> response = Response.success(Collections.singletonList(notificationObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).createNotification(any(), anyLong(), any());
    	
    	// ASSERT
    	List<NotificationObject> notificationList = argus.createNotification(LONG_ID, notificationObj);
    	assertThat(notificationList.get(0).alertId(), is(LONG_ID));
    	assertThat(notificationList.get(0).name(), is(NAME));
    }
    
    @Test
    public void updateNotification() throws Exception {
    	// ARRANGE
    	NotificationObject notificationObj = ImmutableNotificationObject.builder().alertId(LONG_ID).id(LONG_ID).name(NAME).build();
    	Response<NotificationObject> response = Response.success(notificationObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).updateNotification(any(), anyLong(), anyLong(), any());
    	
    	// ASSERT
    	NotificationObject notification = argus.updateNotification(LONG_ID, LONG_ID, notificationObj);
    	assertThat(notification.alertId(), is(LONG_ID));
    	assertThat(notification.name(), is(NAME));
    	assertThat(notification.id(), is(LONG_ID));
    }
    
    @Test
    public void deleteNotification() throws Exception {
    	// ARRANGE
    	NotificationObject notificationObj = ImmutableNotificationObject.builder().alertId(LONG_ID).id(LONG_ID).name(NAME).build();
    	Response<NotificationObject> response = Response.success(notificationObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).deleteNotification(any(), anyLong(), anyLong());
    	
    	// ASSERT
    	argus.deleteNotification(LONG_ID, LONG_ID);
    	verify(svc).deleteNotification(AUTHORIZATION_HEADER, LONG_ID, LONG_ID);
    }
    
    @Test
    public void attachTriggerToNotification() throws Exception {
    	// ARRANGE
		TriggerObject triggerObj = ImmutableTriggerObject.builder().alertId(LONG_ID).id(LONG_ID).notificationIds(LIST_OF_LONG_ID).build();
    	Response<TriggerObject> response = Response.success(triggerObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).attachTriggerToNotification(any(), anyLong(), anyLong(), anyLong());
    	
    	// ASSERT
    	TriggerObject notification = argus.attachTriggerToNotification(LONG_ID, Iterables.getOnlyElement(LIST_OF_LONG_ID), LONG_ID);
    	assertThat(notification.alertId(), is(LONG_ID));
    	assertThat(notification.notificationIds(), equalTo(LIST_OF_LONG_ID));
    	assertThat(notification.id(), is(LONG_ID));
    }
    
    @Test
    public void removeTriggerFromNotification() throws Exception {
    	// ARRANGE
    	TriggerObject triggerObj = ImmutableTriggerObject.builder().alertId(LONG_ID).id(LONG_ID).notificationIds(LIST_OF_LONG_ID).build();
    	Response<TriggerObject> response = Response.success(triggerObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).removeTriggerFromNotification(any(), anyLong(), anyLong(), anyLong());
    	
    	// ASSERT
    	argus.removeTriggerFromNotification(LONG_ID, Iterables.getOnlyElement(LIST_OF_LONG_ID), LONG_ID);
    	verify(svc).removeTriggerFromNotification(AUTHORIZATION_HEADER, LONG_ID, Iterables.getOnlyElement(LIST_OF_LONG_ID), LONG_ID);
    }
    
    @Test
    public void getAllDashboards() throws Exception {
    	// ARRANGE
    	DashboardObject dashboardObj = ImmutableDashboardObject.builder().name(NAME).build();
    	Response<List<DashboardObject>> response = Response.success(Collections.singletonList(dashboardObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<NotificationObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getAllDashboards(any());
    	
    	// ASSERT
    	List<DashboardObject> dashboards = argus.getAllDashboards();
    	assertThat(dashboards.get(0).name(), is(NAME));
    }
    
    @Test
    public void getDashboardByName() throws Exception {
    	// ARRANGE
    	DashboardObject dashboardObj = ImmutableDashboardObject.builder().name(NAME).ownerName(OWNER_NAME).build();
    	Response<List<DashboardObject>> response = Response.success(Collections.singletonList(dashboardObj));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<List<DashboardObject>>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getDashboardByName(any(), anyString(), anyString());
    	
    	// ASSERT
    	DashboardObject dashboard = argus.getDashboardByName(OWNER_NAME, NAME);
    	assertThat(dashboard.name(), is(NAME));
    	assertThat(dashboard.ownerName(), is(OWNER_NAME));
    }
    
    @Test
    public void createDashboard() throws Exception {
    	// ARRANGE
    	DashboardObject dashboardObj = ImmutableDashboardObject.builder().name(NAME).ownerName(OWNER_NAME).build();
    	Response<DashboardObject> response = Response.success(dashboardObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<DashboardObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).createDashboard(any(), any());
    	
    	// ASSERT
    	DashboardObject dashboard = argus.createDashboard(dashboardObj);
    	assertThat(dashboard.name(), is(NAME));
    	assertThat(dashboard.ownerName(), is(OWNER_NAME));
    }
    
    @Test
    public void updateDashboard() throws Exception {
    	// ARRANGE
    	DashboardObject dashboardObj = ImmutableDashboardObject.builder().name(NAME).ownerName(OWNER_NAME).id(LONG_ID).build();
    	Response<DashboardObject> response = Response.success(dashboardObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<DashboardObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).updateDashboard(any(), anyLong(), any());
    	
    	// ASSERT
    	DashboardObject dashboard = argus.updateDashboard(LONG_ID, dashboardObj);
    	assertThat(dashboard.name(), is(NAME));
    	assertThat(dashboard.ownerName(), is(OWNER_NAME));
    	assertThat(dashboard.id(), is(LONG_ID));
    }
    
    @Test
    public void deleteDashboard() throws Exception {
    	// ARRANGE
    	long dashboardId = LONG_ID;
    	DashboardObject dashboardObj = ImmutableDashboardObject.builder().name(NAME).ownerName(OWNER_NAME).id(dashboardId).build();
    	Response<DashboardObject> response = Response.success(dashboardObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<DashboardObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).deleteDashboard(any(), anyLong());
    	
    	// ASSERT
    	argus.deleteDashboard(dashboardId);
    	verify(svc).deleteDashboard(AUTHORIZATION_HEADER, dashboardId);
    }
    
    @Test
    public void getDashboardById() throws Exception {
    	// ARRANGE
    	DashboardObject dashboardObj = ImmutableDashboardObject.builder().id(LONG_ID).build();
    	Response<DashboardObject> response = Response.success(dashboardObj);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<DashboardObject>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
		doReturn(request).when(responseCall).request();
		doReturn(responseCall).when(svc).getDashboardById(any(), anyLong());
    	
    	// ASSERT
    	DashboardObject dashboard = argus.getDashboardById(LONG_ID);
    	assertThat(dashboard.id(), is(LONG_ID));
    }
}
