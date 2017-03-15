/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus;

import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.refocus.model.*;
import com.salesforce.refocus.model.builder.AspectBuilder;
import com.salesforce.refocus.model.builder.SampleBuilder;
import com.salesforce.refocus.model.builder.SubjectBuilder;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Refocus outlet test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public class RefocusClientTest {
    List<Sample> samples = Collections.emptyList();

    @Mock
    private RefocusService svc;

    @Mock
    private AbstractConnector connector;

    private RefocusClient refocus;

    private Subject subject;

    private Sample sample;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        doReturn("http://localhost/").when(connector).endpoint();

        refocus = spy(new RefocusClient(connector));
        doReturn(svc).when(refocus).svc(); // FindBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT - IGNORE
        
        subject = new SubjectBuilder()
                .withId("id")
                .withParentId("parentId")
                .withName("subject")
                .withDescription("description")
                .withPublished(Boolean.TRUE)
                .withSamples(samples)
                .withRelatedLinks(Collections.singletonList(new Link("name", "url")))
                .build();

        sample = new SampleBuilder().withId("id").withName("name").withValue("value").build();
    }

    @Test
    public void authWithToken() throws Exception {
        // ARRANGE
        doReturn("password".getBytes()).when(connector).password();

        // ACT
        refocus.auth();

        // ASSERT
        assertThat(refocus.isAuthenticated(), equalTo(true));
        verify(refocus).generateAuthorizationHeader(new String("password".getBytes(), Charset.defaultCharset()));
    }

    @Test
    public void authWithCredentials() throws Exception {
        // ARRANGE
        AuthResponse authResponse = mock(AuthResponse.class);
        doReturn("token").when(authResponse).token();
        Response<AuthResponse> response = Response.success(authResponse);

        @SuppressWarnings("unchecked")
        Call<AuthResponse> responseCall = (Call<AuthResponse>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).authenticate(any());

        doReturn("username").when(connector).username();
        doReturn("password".getBytes()).when(connector).password();

        // ACT
        refocus.auth();

        // ASSERT
        assertThat(refocus.isAuthenticated(), equalTo(true));
        verify(svc).authenticate(new AuthRequest("username", "password".getBytes()));
        verify(refocus).generateAuthorizationHeader(new String("token".getBytes(), Charset.defaultCharset()));
    }

    @Test
    public void failedAuth() throws Exception {
        // ARRANGE
        ResponseBody fail = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<AuthResponse> failedResponse = Response.error(400, fail);

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = (Call<ResponseBody>)mock(Call.class);
        doReturn(failedResponse).when(responseCall).execute();
        doReturn(responseCall).when(svc).authenticate(any());

        doReturn("username").when(connector).username();
        doReturn("password".getBytes()).when(connector).password();

        // ACT
        boolean authenticationResult = refocus.auth();

        // ASSERT
        assertThat(authenticationResult, equalTo(false));
        assertThat(refocus.isAuthenticated(), equalTo(false));
        verify(svc, times(1)).authenticate(any());
        verify(refocus, times(0)).generateAuthorizationHeader(any());
    }

    @Test
    public void upsertSamples() throws Exception {
        // ARRANGE
        ResponseBody okBody = ResponseBody.create(MediaType.parse(""), "OK");
        Response<ResponseBody> response = Response.success(okBody);

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = (Call<ResponseBody>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).upsertSamplesBulk(any(), any());

        // ACT
        boolean result = refocus.upsertSamplesBulk(Collections.singletonList(sample));

        // ASSERT
        assertThat(result, equalTo(true));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void upsertSamplesFailNPESample() throws Exception {
        // AA
        upsertSamplesFailWithSample(null); // FindBugs/NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS
    }

    @Test
    public void upsertSamplesFailWithSamples() throws Exception {
        // AA
        boolean result = upsertSamplesFailWithSample(Collections.singletonList(sample));

        // ASSERT
        assertThat(result, equalTo(false));
    }

    // Helper that prepares the upsert test
    private boolean upsertSamplesFailWithSample(List<Sample> samples) throws IOException, UnauthorizedException {
        // ARRANGE
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<ResponseBody> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = (Call<ResponseBody>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).upsertSamplesBulk(any(), any());

        // ACT
        return refocus.upsertSamplesBulk(samples);
    }

    @Test
    public void retrieveSubjects() throws Exception {
        // ARRANGE
        List<String> fields = Arrays.asList("id", "name", "isPublished");

        List<Subject> responseData = Collections.singletonList(subject);

        Response<List<Subject>> response = Response.success(responseData);

        @SuppressWarnings("unchecked")
        Call<List<Subject>> responseCall = (Call<List<Subject>>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).getSubjects(any(), any());

        // ACT
        List<Subject> subjects = refocus.getSubjects(fields);

        // ASSERT
        assertThat(subjects.size(), equalTo(1));
        assertThat(subjects.get(0).name(), equalTo("subject"));
        assertThat(subjects.get(0).isPublished(), equalTo(true));
    }

    @Test
    public void retrieveSubjectsFailure() throws Exception {
        // ARRANGE
        List<String> fields = Arrays.asList("id", "name", "isPublished");

        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<List<Subject>> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<List<Subject>> responseCall = (Call<List<Subject>>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).getSubjects(any(), any());

        // ACT
        List<Subject> subjects = refocus.getSubjects(fields);

        // ASSERT
        assertThat(subjects.size(), equalTo(0));
    }

    @Test
    public void getSample() throws Exception {
        // ARRANGE
        String name = "cceDevils";
        String value = "sampleValue";
        String id = "123456";
        List<String> fields = Arrays.asList("id", "name", "isPublished");
        Sample sample = new SampleBuilder().withName(name).withValue(value).withId(id).build();
        Response<Sample> response = Response.success(sample);

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = (Call<ResponseBody>) mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).getSample(any(), any(), anyList());

        // ACT
        Sample result = refocus.getSample(name, fields);

        // ASSERT
        assertThat(result.name(), is(name));
        assertThat(result.value(), is(value));
        assertThat(result.id(), is(id));
    }

    @Test
    public void getSamples() throws Exception {
        // ARRANGE
        String name = "cceDevils";
        String value = "sampleValue";
        String id = "123456";
        Sample sample = new SampleBuilder().withName(name).withValue(value).withId(id).build();
        Response<List<Sample>> response = Response.success(Collections.singletonList(sample));

        @SuppressWarnings("unchecked")
        Call<ResponseBody> responseCall = (Call<ResponseBody>) mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).getSample(any(), anyString());

        // ACT
        List<Sample> results = refocus.getSamples(name);

        // ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0).name(), is(name));
        assertThat(results.get(0).value(), is(value));
        assertThat(results.get(0).id(), is(id));
    }

    @Test
    public void upsertSamplesBulk() throws Exception {
    	// ARRANGE
    	String name = "cceDevils";
    	String value = "sampleValue";
    	String id = "123456";
    	List<Sample>sampleList = new ArrayList<Sample>();
        Sample sample = new SampleBuilder().withName(name).withValue(value).withId(id).build();
        sampleList.add(sample);
        Response<List<Sample>> response = Response.success(Collections.singletonList(sample));

        @SuppressWarnings("unchecked")
        Call<List<Sample>> responseCall = (Call<List<Sample>>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).upsertSamplesBulk(any(), any());

        // ACT
        boolean result = refocus.upsertSamplesBulk(sampleList);

        // ASSERT
        assertThat(result, is(true));
    }
    
    @Test
    public void deleteSample() throws Exception {
    	// ARRANGE
    	String key = "cceDevils";
    	String name = "cceDevilsSample";
    	String id = "123456";
        Sample sample = new SampleBuilder().withName(name).withId(id).build();
        Response<Sample> response = Response.success(sample);

        @SuppressWarnings("unchecked")
        Call<Sample> responseCall = (Call<Sample>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).deleteSample(any(), anyString());

        // ACT
        refocus.deleteSample(key);
        
        // ASSERT
        verify(svc).deleteSample(any(), anyString());
    }
    
    @Test
    public void getSubjects() throws Exception {
    	// ARRANGE
        List<String> fields = Arrays.asList("id", "subject", "isPublished");
        Response<List<Subject>> response = Response.success(Collections.singletonList(subject));

        @SuppressWarnings("unchecked")
        Call<List<Subject>> responseCall = (Call<List<Subject>>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).getSubjects(any(), any());

        // ACT
        List<Subject> subjects = refocus.getSubjects(fields);

        // ASSERT
        assertThat(subjects.size(), equalTo(1));
        assertThat(subjects.get(0).name(), is(fields.get(1)));
        assertThat(subjects.get(0).isPublished(), is(true));
    }


    @Test
    public void getSubject() throws Exception {
        // ARRANGE
        Subject subject = new SubjectBuilder(this.subject).withPublished(Boolean.FALSE).build();

        Response<Subject> response = Response.success(subject);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = (Call<Subject>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).getSubject(any(), anyString(), anyList());

        // ACT
        Subject result = refocus.getSubject(subject.id(), Collections.emptyList());

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("subject"));
        assertThat(result.absolutePath(), is(nullValue()));
    }
    
    @Test
    public void getSubjectHierarchy() throws Exception {
        // ARRANGE
        Response<Subject> response = Response.success(subject);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = (Call<Subject>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).getSubjectHierarchy(any(), anyString(), anyString());

        // ACT
        Subject result = refocus.getSubjectHierarchy("subject", "OK");

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("subject"));
        assertThat(result.id(), is("id"));
    }

    @Test
    public void getSubjectFailResponse() throws Exception {
        // ARRANGE
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<Subject> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = (Call<Subject>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
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
        Call<Subject> responseCall = (Call<Subject>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).postSubject(any(), any());

        // ACT
        Subject result = refocus.postSubject(subject);

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("subject"));
        assertThat(result.isPublished(), equalTo(true));
    }

    @Test
    public void postSubjectFail() throws Exception {
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<Subject> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = (Call<Subject>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).postSubject(any(), any());

        // ACT
        Subject result = refocus.postSubject(subject);

        // ASSERT
        assertThat(result, is(nullValue()));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void postSubjectFailNPE() throws Exception {
        // ACT
        refocus.postSubject(null); // FindBugs/NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS
    }


    @Test
    public void patchSubject() throws Exception {
        // ARRANGE
        Response<Subject> response = Response.success(subject);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = (Call<Subject>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).patchSubject(any(), anyString(), any());

        // ACT
        Subject result = refocus.patchSubject(subject);

        // ASSERT
        assertThat(result, is(not(nullValue())));
        assertThat(result.name(), equalTo("subject"));
    }


    @Test
    public void patchSubjectFail() throws Exception {
        // ARRANGE
        ResponseBody errorBody = ResponseBody.create(MediaType.parse(""), "FAIL");
        Response<Subject> response = Response.error(400, errorBody);

        @SuppressWarnings("unchecked")
        Call<Subject> responseCall = (Call<Subject>)mock(Call.class);
        doReturn(response).when(responseCall).execute();
        doReturn(responseCall).when(svc).patchSubject(any(), any(), any());

        // ACT
        Subject result = refocus.patchSubject(subject);

        // ASSERT
        assertThat(result, is(nullValue()));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void patchSubjectFailNPE() throws Exception {
        // ACT
        refocus.patchSubject(null); // FindBugs/NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS
    }
    
    @Test
    public void getAspects() throws Exception {
    	// ARRANGE
    	String name = "cceDevils";
    	List<String> fields = new ArrayList<String>();
    	Aspect aspect = new AspectBuilder().withName(name).build();
    	Response<List<Aspect>>response = Response.success(Collections.singletonList(aspect));
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Aspect>responseCall = (Call<Aspect>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
    	doReturn(responseCall).when(svc).getAspects(any(), any());
    	
    	List<Aspect>results = refocus.getAspects(fields);
    	
    	// ASSERT
    	assertThat(results.get(0).name(), is(name));
    }
    
    @Test
    public void getAspect() throws Exception {
    	// ARRANGE
    	String name = "cceDevils";
    	List<String> fields = new ArrayList<String>();
    	Aspect aspect = new AspectBuilder().withName(name).build();
    	Response<Aspect>response = Response.success(aspect);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Aspect>responseCall = (Call<Aspect>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
    	doReturn(responseCall).when(svc).getAspect(any(), anyString(), anyList());
    	
    	Aspect result = refocus.getAspect(name, fields);
    	
    	// ASSERT
    	assertThat(result.name(), is(name));
    }
    
    @Test
    public void postAspect() throws Exception {
    	// ARRANGE
    	String name = "cceDevils";
    	Aspect aspect = new AspectBuilder().withName(name).build();
    	Response<Aspect>response = Response.success(aspect);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Aspect>responseCall = (Call<Aspect>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
    	doReturn(responseCall).when(svc).postAspect(any(), any());
    	
    	Aspect result = refocus.postAspect(aspect);
    	
    	// ASSERT
    	assertThat(result.name(), is(name));
    }
    
    @Test
    public void patchAspect() throws Exception {
    	String name = "cceDevils";
    	Aspect aspect = new AspectBuilder().withName(name).build();
    	Response<Aspect>response = Response.success(aspect);
    	
    	// ACT
    	@SuppressWarnings("unchecked")
		Call<Aspect>responseCall = (Call<Aspect>)mock(Call.class);
    	doReturn(response).when(responseCall).execute();
    	doReturn(responseCall).when(svc).patchAspect(any(), anyString(), any());
    	
    	Aspect result = refocus.patchAspect(aspect);
    	
    	// ASSERT
    	assertThat(result.name(), is(name));
    }
}