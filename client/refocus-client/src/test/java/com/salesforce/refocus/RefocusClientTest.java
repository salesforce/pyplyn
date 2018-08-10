/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.refocus.model.*;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Refocus outlet test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public class RefocusClientTest {
    private static final String AUTHORIZATION_HEADER = "Authorization: fake";
    private static final Link LINK = ImmutableLink.of("name", "url");
    private static final List<String> fields = Arrays.asList("id", "name", "isPublished");

    private Sample sample;
    private Request request;
    private Subject subject;

    @Mock
    private RefocusService svc;

    @Mock
    private Connector connector;

    private RefocusClient refocus;
    private AuthRequest authRequest;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        doReturn("http://localhost/").when(connector).endpoint();
        doReturn("password".getBytes(Charset.defaultCharset())).when(connector).password();

        refocus = spy(new RefocusClient(connector));
        doReturn(svc).when(refocus).svc();

        // init a subject
        subject = ImmutableSubject.builder()
                .id("id")
                .parentId("parentId")
                .name("name")
                .description("description")
                .isPublished(Boolean.TRUE)
                .relatedLinks(Collections.singletonList(LINK))
                .build();

        // init a sample
        sample = ImmutableSample.builder().id("id").name("name").value("value").build();

        // build a dummy request object
        Request.Builder builder = new Request.Builder();
        builder.url("http://tests/");
        builder.method("MOCK", null);
        request = builder.build();

        // mock auth
        doReturn(AUTHORIZATION_HEADER).when(refocus).authorizationHeader();
        authRequest = ImmutableAuthRequest.of("username", "password".getBytes());
    }

    @Test
    public void authWithToken() throws Exception {
        // ARRANGE
        doReturn("password".getBytes()).when(connector).password();

        // ACT
        refocus.authenticate();

        // ASSERT
        assertThat(refocus.isAuthenticated(), equalTo(true));
        verify(refocus).auth();
    }

    @Test
    public void authWithCredentials() throws Exception {
        // ARRANGE
        AuthResponse authResponse = mock(AuthResponse.class);
        doReturn("token".getBytes(Charset.defaultCharset())).when(authResponse).token();
        Response<AuthResponse> response = Response.success(authResponse);

        @SuppressWarnings("unchecked")
        Call<AuthResponse> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).authenticate(any());

        doReturn("username").when(connector).username();
        doReturn("password".getBytes()).when(connector).password();

        // ACT
        refocus.auth();

        // ASSERT
        assertThat(refocus.isAuthenticated(), equalTo(true));
        verify(svc).authenticate(authRequest);
        verify(refocus).auth();
    }

    @Test(expectedExceptions = UnauthorizedException.class)
    public void failedAuthWithTokenThrowsException() throws Exception {
        // ARRANGE
        ResponseBody fail = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<AuthResponse> failedResponse = Response.error(401, fail);

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = mock(Call.class);
        doReturn(failedResponse).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).authenticate(any());

        doReturn("username").when(connector).username();
        doReturn("password".getBytes()).when(connector).password();

        // ACT
        try {
            refocus.authenticate();

        // ASSERT
        } finally {
            assertThat(refocus.isAuthenticated(), equalTo(false));
            verify(svc, times(1)).authenticate(any());
            verify(refocus, times(1)).auth();
        }
    }

    @Test
    public void reauthenticateIfAnOperationFailsWithAuthException() throws Exception {
        // ARRANGE
        // credentials
        doReturn("username").when(connector).username();
        doReturn("password".getBytes()).when(connector).password();

        // token response
        AuthResponse authResponse = mock(AuthResponse.class);
        doReturn("token".getBytes(Charset.defaultCharset())).when(authResponse).token();
        Response<AuthResponse> responseAuth = Response.success(authResponse);

        // auth response
        @SuppressWarnings("unchecked")
        Call<AuthResponse> authResponseCall = mock(Call.class);
        doReturn(responseAuth).when(authResponseCall).execute();
        doReturn(request).when(authResponseCall).request();
        doReturn(authResponseCall).when(svc).authenticate(any());

        // subject
        List<Subject> responseData = Collections.singletonList(subject);
        Response<List<Subject>> response = Response.success(responseData);

        // authentication failure
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "error");
        Response<List<Subject>> authFailedResponse = Response.error(401, errorBody);

        // subjects response
        @SuppressWarnings("unchecked")
        Call<List<Subject>> responseCall = mock(Call.class);
        doReturn(responseCall).when(responseCall).clone();
        doReturn(authFailedResponse, response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getSubjects(any(), any());


        // ACT
        List<Subject> subjects = refocus.getSubjects(fields);


        // ASSERT
        verify(svc).authenticate(authRequest);
        verify(refocus).resetAuth();
        verify(refocus).auth();
        assertThat(refocus.isAuthenticated(), equalTo(true));

        assertThat(subjects.size(), equalTo(1));
        assertThat(subjects.get(0).name(), equalTo("name"));
        assertThat(subjects.get(0).isPublished(), equalTo(true));
    }



    @Test
    public void upsertSamplesBulk() throws Exception {
        // ARRANGE
        Response<UpsertResponse> response = Response.success(ImmutableUpsertResponse.of("OK", 0));

        @SuppressWarnings("unchecked")
        Call<UpsertResponse> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).upsertSamplesBulk(any(), any());

        // ACT
        UpsertResponse result = refocus.upsertSamplesBulk(Collections.singletonList(sample));

        // ASSERT
        assertThat(result, notNullValue());
        assertThat(result.status(), equalTo("OK"));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void upsertSamplesFailNPESample() throws Exception {
        // AA
        upsertSamplesFailWithSample(null);
    }

    @Test
    public void upsertSamplesFailWithSamples() throws Exception {
        // AA
        UpsertResponse result = upsertSamplesFailWithSample(Collections.singletonList(sample));

        // ASSERT
        assertThat(result, nullValue());
    }

    // Helper that prepares the upsert test
    private UpsertResponse upsertSamplesFailWithSample(List<Sample> samples) throws IOException, UnauthorizedException {
        // ARRANGE
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<UpsertResponse> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<UpsertResponse> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).upsertSamplesBulk(any(), any());

        // ACT
        return refocus.upsertSamplesBulk(samples);
    }

    @Test
    public void retrieveSubjects() throws Exception {
        // ARRANGE
        List<Subject> responseData = Collections.singletonList(subject);

        Response<List<Subject>> response = Response.success(responseData);

        @SuppressWarnings("unchecked")
        Call<List<Subject>> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getSubjects(any(), any());

        // ACT
        List<Subject> subjects = refocus.getSubjects(fields);

        // ASSERT
        assertThat(subjects.size(), equalTo(1));
        assertThat(subjects.get(0).name(), equalTo("name"));
        assertThat(subjects.get(0).isPublished(), equalTo(true));
    }

    @Test
    public void retrieveSubjectsFailure() throws Exception {
        // ARRANGE
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<List<Subject>> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<List<Subject>> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getSubjects(any(), any());

        // ACT
        List<Subject> subjects = refocus.getSubjects(fields);

        // ASSERT
        assertThat(subjects.size(), equalTo(0));
    }

    @Test
    public void getSample() throws Exception {
        // ARRANGE
        Response<Sample> response = Response.success(sample);

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getSample(any(), any(), anyList());

        // ACT
        Sample result = refocus.getSample("name", fields);

        // ASSERT
        assertThat(result.name(), is("name"));
        assertThat(result.value(), is("value"));
        assertThat(result.id(), is("id"));
    }

    @Test
    public void getSamples() throws Exception {
        // ARRANGE
        Response<List<Sample>> response = Response.success(Collections.singletonList(sample));

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getSample(any(), anyString());

        // ACT
        List<Sample> results = refocus.getSamples("name");

        // ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0).name(), is("name"));
        assertThat(results.get(0).value(), is("value"));
        assertThat(results.get(0).id(), is("id"));
    }

    @Test
    public void deleteSample() throws Exception {
    	// ARRANGE
        Response<Sample> response = Response.success(sample);

        @SuppressWarnings("unchecked")
        Call<Sample> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).deleteSample(any(), anyString());

        // ACT
        refocus.deleteSample("id");
        
        // ASSERT
        verify(svc).deleteSample(AUTHORIZATION_HEADER, "id");
    }
    
    @Test
    public void getSubjects() throws Exception {
    	// ARRANGE
        Response<List<Subject>> response = Response.success(Collections.singletonList(subject));

        @SuppressWarnings("unchecked")
        Call<List<Subject>> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getSubjects(any(), any());

        // ACT
        List<Subject> subjects = refocus.getSubjects(fields);

        // ASSERT
        assertThat(subjects.size(), equalTo(1));
        assertThat(subjects.get(0).name(), is("name"));
        assertThat(subjects.get(0).isPublished(), is(true));
    }

    @Test
    public void getSubject() throws Exception {
        // ARRANGE
        Subject subject = ImmutableSubject.builder()
                .from(this.subject)
                .isPublished(Boolean.FALSE)
                .build();

        Response<Subject> response = Response.success(subject);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getSubject(any(), anyString(), anyList());

        // ACT
        Subject result = refocus.getSubject(subject.id(), Collections.emptyList());

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("name"));
        assertThat(result.absolutePath(), is(nullValue()));
    }
    
    @Test
    public void getSubjectHierarchy() throws Exception {
        // ARRANGE
        Response<Subject> response = Response.success(subject);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getSubjectHierarchy(any(), anyString(), anyString());

        // ACT
        Subject result = refocus.getSubjectHierarchy("name", "OK");

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("name"));
        assertThat(result.id(), is("id"));
    }

    @Test
    public void getSubjectFailResponse() throws Exception {
        // ARRANGE
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<Subject> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getSubject(any(), anyString(), anyList());

        // ACT
        Subject result = refocus.getSubject("", Collections.emptyList());

        // ASSERT
        assertThat(result, is(nullValue()));
    }

    @Test
    public void postSubject() throws Exception {
        // ARRANGE
        Response<Subject> response = Response.success(subject);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).postSubject(any(), any());

        // ACT
        Subject result = refocus.postSubject(subject);

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("name"));
        assertThat(result.isPublished(), equalTo(true));
    }

    @Test
    public void postSubjectFail() throws Exception {
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<Subject> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).postSubject(any(), any());

        // ACT
        Subject result = refocus.postSubject(subject);

        // ASSERT
        assertThat(result, is(nullValue()));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void postSubjectFailNPE() throws Exception {
        // ACT
        refocus.postSubject(null);
    }

    @Test
    public void putSubject() throws Exception {
        // ARRANGE
        Response<Subject> response = Response.success(subject);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).putSubject(any(), any(), any());

        // ACT
        Subject result = refocus.putSubject(subject);

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("name"));
        assertThat(result.isPublished(), equalTo(true));
    }

    @Test
    public void putSubjectFail() throws Exception {
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<Subject> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).putSubject(any(), any(), any());

        // ACT
        Subject result = refocus.putSubject(subject);

        // ASSERT
        assertThat(result, is(nullValue()));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void putSubjectFailNPE() throws Exception {
        // ACT
        refocus.putSubject(null);
    }

    @Test
    public void deleteSubject() throws Exception {
        // ARRANGE
        Response<Subject> response = Response.success(subject);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).deleteSubject(any(), any());

        // ACT
        Subject result = refocus.deleteSubject("key");

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("name"));
        assertThat(result.isPublished(), equalTo(true));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void deleteSubjectFailNPE() throws Exception {
        // ACT
        refocus.deleteSubject(null);
    }

    @Test
    public void patchSubject() throws Exception {
        // ARRANGE
        Response<Subject> response = Response.success(subject);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).patchSubject(any(), anyString(), any());

        // ACT
        Subject result = refocus.patchSubject(subject);

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("name"));
    }

    @Test
    public void patchSubjectFail() throws Exception {
        // ARRANGE
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<Subject> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).patchSubject(any(), any(), any());

        // ACT
        Subject result = refocus.patchSubject(subject);

        // ASSERT
        assertThat(result, is(nullValue()));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void patchSubjectFailNPE() throws Exception {
        // ACT
        refocus.patchSubject(null);
    }
    
    @Test
    public void getAspects() throws Exception {
    	// ARRANGE
    	Aspect aspect = ImmutableAspect.builder().name("name").build();
    	Response<List<Aspect>>response = Response.success(Collections.singletonList(aspect));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Aspect>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getAspects(any(), any());
    	
    	List<Aspect>results = refocus.getAspects(fields);
    	
    	// ASSERT
    	assertThat(results.get(0).name(), is("name"));
    }
    
    @Test
    public void getAspect() throws Exception {
    	// ARRANGE
    	Aspect aspect = ImmutableAspect.builder().name("name").build();
    	Response<Aspect>response = Response.success(aspect);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Aspect>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).getAspect(any(), anyString(), anyList());
    	
    	Aspect result = refocus.getAspect("name", fields);
    	
    	// ASSERT
    	assertThat(result.name(), is("name"));
    }
    
    @Test
    public void postAspect() throws Exception {
    	// ARRANGE
    	Aspect aspect = ImmutableAspect.builder().name("name").build();
    	Response<Aspect>response = Response.success(aspect);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Aspect>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).postAspect(any(), any());
    	
    	Aspect result = refocus.postAspect(aspect);
    	
    	// ASSERT
    	assertThat(result.name(), is("name"));
    }
    
    @Test
    public void patchAspect() throws Exception {
    	Aspect aspect = ImmutableAspect.builder().name("name").build();
    	Response<Aspect>response = Response.success(aspect);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Aspect>responseCall = mock(Call.class);
    	doReturn(response).when(responseCall).execute();
        doReturn(request).when(responseCall).request();
        doReturn(responseCall).when(svc).patchAspect(any(), anyString(), any());
    	
    	Aspect result = refocus.patchAspect(aspect);
    	
    	// ASSERT
    	assertThat(result.name(), is("name"));
    }
}