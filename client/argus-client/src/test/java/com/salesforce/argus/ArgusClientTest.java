/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Iterables;
import com.salesforce.argus.model.AlertObject;
import com.salesforce.argus.model.AuthToken;
import com.salesforce.argus.model.DashboardObject;
import com.salesforce.argus.model.ImmutableAlertObject;
import com.salesforce.argus.model.ImmutableAuthToken;
import com.salesforce.argus.model.ImmutableDashboardObject;
import com.salesforce.argus.model.ImmutableMetricResponse;
import com.salesforce.argus.model.ImmutableNotificationObject;
import com.salesforce.argus.model.ImmutableTriggerObject;
import com.salesforce.argus.model.MetricResponse;
import com.salesforce.argus.model.NotificationObject;
import com.salesforce.argus.model.TriggerObject;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.EndpointConnector;

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
    public void testRefresh() throws Exception{

        // ARRANGE

        doReturn("username").when(connector).username(); //need a username to avoid token based auth

        AuthToken token = ImmutableAuthToken.of("access".getBytes(Charset.defaultCharset()), "refresh".getBytes(Charset.defaultCharset()));
        Response<AuthToken> authResponseToken = Response.success(token);

        // the new access token is returned after a refresh
        AuthToken newToken = ImmutableAuthToken.of("new_access".getBytes(Charset.defaultCharset()), "refresh".getBytes(Charset.defaultCharset()));
        Response<AuthToken> newAuthResponseToken = Response.success(newToken);

        @SuppressWarnings("unchecked")
        Call<AuthToken> authResponseCall = mock(Call.class);
        doReturn(authResponseToken).when(authResponseCall).execute();
        doReturn(request).when(authResponseCall).request();
        doReturn(authResponseCall).when(svc).login(any());

        // simulate a timeout of the access token by throwing an UnauthorizedException when calling getMetrics()
        @SuppressWarnings("unchecked")
        Call<List<MetricResponse>> failedMetricResponseCall = mock(Call.class);
        doThrow(UnauthorizedException.class).when(failedMetricResponseCall).execute();
        doReturn(request).when(failedMetricResponseCall).request();
        doReturn(failedMetricResponseCall).when(svc).getMetrics(any(), any());

        // return a different token when refreshed as opposed to login
        @SuppressWarnings("unchecked")
        Call<AuthToken> newAuthResponseCall = mock(Call.class);
        doReturn(newAuthResponseToken).when(newAuthResponseCall).execute();
        doReturn(request).when(newAuthResponseCall).request();
        doReturn(newAuthResponseCall).when(svc).refresh(any());

        // ACT
        argus.auth();
        byte[] loginAccessToken = argus.getAccessToken();

        try {
            List<String> notNullExpressions = new ArrayList<>();
            notNullExpressions.add("expression1"); // getMetrics requires a non-empty list
            argus.getMetrics(notNullExpressions);
        } catch(NullPointerException e) {
            //getMetrics returns null when it fails, so this is expected
        } finally {
            byte[] refreshAccessToken = argus.getAccessToken();

            // ASSERT

            verify(argus, times(2)).resetAuth(); // the access token was nullified, runs twice as the retry count is 2.
            verify(argus, times(3)).auth();      // only ask for auth when access token is null
            // This gets called twice, since on retry it calls auth which internally calls refresh method. The third attempt it fails as the call object itself is null.
            verify(svc, times(2)).refresh(any());
            verify(svc, times(1)).login(any());  // login only once.

            assertThat("Refreshing should change the access token", loginAccessToken, is(not(refreshAccessToken)));
        }

    }

    @Test
    public void getNewRefreshTokenWithUserAndPass() throws Exception {

        // ARRANGE

        doReturn("username").when(connector).username(); //need a username to avoid token based auth

        AuthToken token = ImmutableAuthToken.of("access".getBytes(Charset.defaultCharset()), "refresh".getBytes(Charset.defaultCharset()));
        Response<AuthToken> authResponseToken = Response.success(token);

        AuthToken newRefreshToken = ImmutableAuthToken.of("new_access".getBytes(Charset.defaultCharset()), "new_refresh".getBytes(Charset.defaultCharset()));
        Response<AuthToken> refreshAuthResponseToken = Response.success(newRefreshToken);

        Call<AuthToken> authResponseCall = mock(Call.class);
        doReturn(authResponseToken).when(authResponseCall).execute();
        doReturn(request).when(authResponseCall).request();

        // simulate a timeout of the access token by throwing an UnauthorizedException when calling getMetrics()
        Call<List<MetricResponse>> failedMetricResponseCall = mock(Call.class);
        doThrow(UnauthorizedException.class).when(failedMetricResponseCall).execute();
        doReturn(request).when(failedMetricResponseCall).request();
        doReturn(failedMetricResponseCall).when(svc).getMetrics(any(), any());

        // return a different token when refreshed as opposed to login
        @SuppressWarnings("unchecked")
        Call<AuthToken> refreshAuthResponseCall = mock(Call.class);
        doReturn(refreshAuthResponseToken).when(refreshAuthResponseCall).execute();
        doReturn(request).when(refreshAuthResponseCall).request();

        //return a different set of tokens the second time we login to simulate a refresh token timing out
        doReturn(authResponseCall).doReturn(refreshAuthResponseCall).when(svc).login(any());

        // refreshing should fail with an expired access token
        doThrow(UnauthorizedException.class).when(svc).refresh(any());

        // ACT
        argus.auth();
        byte[] loginRefreshToken = argus.getRefreshToken();

        try {
            List<String> notNullExpressions = new java.util.ArrayList<String>();
            notNullExpressions.add("expression1"); // getMetrics requires a non-empty list
            argus.getMetrics(notNullExpressions);
        } catch(NullPointerException e) {
            // getMetrics returns null when it fails, so this is expected
        } finally {
            byte[] refreshRefreshToken = argus.getRefreshToken();

            // ASSERT

            verify(argus, times(2)).resetAuth(); // the access token was nullified; retry count = 2
            verify(argus, times(5)).auth();      // once for first token; 2 x 2 = 4 times for retrying
            verify(svc, times(2)).refresh(any());// refresh twice
            verify(svc, times(3)).login(any());  // once for first token; twice for retrying

            assertThat("The refresh token should change once denied", loginRefreshToken, is(not(refreshRefreshToken)));
        }

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
        Call<List<AlertObject>> responseCall = mock(Call.class);
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
        Call<List<AlertObject>> responseCall = mock(Call.class);
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
    public void getAllDashboardMetadata() throws Exception {
        // ARRANGE
        DashboardObject dashboardObj = ImmutableDashboardObject.builder().name(NAME).build();
        Response<List<DashboardObject>> response = Response.success(Collections.singletonList(dashboardObj));

        // ACT
        @SuppressWarnings("unchecked")
        Call<NotificationObject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getAllDashboardMetadata(any());

        // ASSERT
        List<DashboardObject> dashboards = argus.getAllDashboardMetadata();
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

    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteAndRetrieveBodyWithRetry() throws Exception {
        // ARRANGE

        Call originalCall = mock(Call.class);
        Call cloneOne = mock(Call.class);
        Call cloneTwo = mock(Call.class);
        Call cloneThree = mock(Call.class);

        MockAbstractRemoteClient argusRemoteClient = spy(new MockAbstractRemoteClient(connector));

        RuntimeException excp = new RuntimeException("This is an expected exception.");
        doThrow(excp).when(argusRemoteClient).executeCallInternalRetryIfUnauthorized(any());

        // Mock clones.
        doReturn(cloneOne).when(originalCall).clone();
        doReturn(cloneTwo).when(cloneOne).clone();
        doReturn(cloneThree).when(cloneTwo).clone();

        // This validates that the third clone isn't cloned and we don't do unnecessary work after the last retry.
        doThrow(new RuntimeException("Should not be cloned!")).when(cloneThree).clone();

        // ACT
        Object result = argusRemoteClient.executeAndRetrieveBody(originalCall, "retry", 3);

        // ASSERT

        assertThat(result, is("retry"));

        // Validate the retries.
        // This will invoke the clone twice, initially -> originalCall, clone first time -> cloneOne, clone second time -> cloneTwo (total 3 attempts).
        verify(argusRemoteClient, times(2)).executeCallInternalRetryIfUnauthorized(originalCall);
        verify(argusRemoteClient, times(1)).executeCallInternalRetryIfUnauthorized(cloneOne);
        verify(argusRemoteClient, times(1)).executeCallInternalRetryIfUnauthorized(cloneTwo);
        // This will never be called since the retry count 3. Order -> OriginalCall, cloneOne, cloneTwo.
        verify(argusRemoteClient, times(0)).executeCallInternalRetryIfUnauthorized(cloneThree);
    }

    /**
     * Abstract class for testing the protected methods in AbstractRemoteClient.java
     * @author akarande
     *
     */
    class MockAbstractRemoteClient extends ArgusClient {
        public MockAbstractRemoteClient(EndpointConnector connector) {
            super(connector);
        }

        @Override
        public <T> T executeAndRetrieveBody(Call<T> call, T defaultFailResponse, int retryCount) throws UnauthorizedException {
            return super.executeAndRetrieveBody(call, defaultFailResponse, retryCount);
        }

        @Override
        public <T> Response<T> executeCallInternalRetryIfUnauthorized(Call<T> call) throws UnauthorizedException {
            return super.executeCallInternalRetryIfUnauthorized(call);
        }
    }
}
