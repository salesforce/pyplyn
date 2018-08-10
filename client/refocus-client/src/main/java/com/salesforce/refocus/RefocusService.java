/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus;

import java.util.List;

import com.salesforce.refocus.model.*;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.*;

/**
 * Refocus REST service class
 * <p/>
 * Note: operations are not commented in this interface, since this is a {@link Retrofit} service.
 *   See {@link RefocusClient} for an explanation of what each method does.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public interface RefocusService {
    /* Authorization header */
    String AUTHORIZATION = "Authorization";

    /* Authentication */

    @POST("authenticate")
    @Headers("Content-Type: application/json")
    Call<AuthResponse> authenticate(@Body AuthRequest body);

    /* Samples operations */

    @GET("samples/{key}")
    @Headers("Content-Type: application/json")
    Call<Sample> getSample(@Header(AUTHORIZATION) String authorization, @Path("key") String key, @Query("fields") List<String> fields);

    @GET("samples")
    @Headers("Content-Type: application/json")
    Call<List<Sample>> getSample(@Header(AUTHORIZATION) String authorization, @Query("name") String name);

    @POST("samples/upsert/bulk")
    @Headers("Content-Type: application/json")
    Call<UpsertResponse> upsertSamplesBulk(@Header(AUTHORIZATION) String authorization, @Body List<Sample> samples);

    @DELETE("samples/{key}")
    @Headers("Content-Type: application/json")
    Call<Sample> deleteSample(@Header(AUTHORIZATION) String authorization, @Path("key") String key);


    /* Subjects operations */

    @GET("subjects")
    @Headers("Content-Type: application/json")
    Call<List<Subject>> getSubjects(@Header(AUTHORIZATION) String authorization, @Query("fields") List<String> fields);

    @GET("subjects/{key}")
    @Headers("Content-Type: application/json")
    Call<Subject> getSubject(@Header(AUTHORIZATION) String authorization, @Path("key") String key, @Query("fields") List<String> fields);

    @GET("subjects/{key}/hierarchy")
    @Headers("Content-Type: application/json")
    Call<Subject> getSubjectHierarchy(@Header(AUTHORIZATION) String authorization, @Path("key") String key, @Query("status") String status);

    @POST("subjects")
    @Headers("Content-Type: application/json")
    Call<Subject> postSubject(@Header(AUTHORIZATION) String authorization, @Body Subject subject);

    @PATCH("subjects/{key}")
    @Headers("Content-Type: application/json")
    Call<Subject> patchSubject(@Header(AUTHORIZATION) String authorization, @Path("key") String key, @Body Subject subject);

    @PUT("subjects/{key}")
    @Headers("Content-Type: application/json")
    Call<Subject> putSubject(@Header(AUTHORIZATION) String authorization, @Path("key") String key, @Body Subject subject);

    @DELETE("subjects/{key}")
    @Headers("Content-Type: application/json")
    Call<Subject> deleteSubject(@Header(AUTHORIZATION) String authorization, @Path("key") String key);


    /* Aspect operations */

    @GET("aspects")
    @Headers("Content-Type: application/json")
    Call<List<Aspect>> getAspects(@Header(AUTHORIZATION) String authorization, @Query("fields") List<String> fields);

    @GET("aspects/{key}")
    @Headers("Content-Type: application/json")
    Call<Aspect> getAspect(@Header(AUTHORIZATION) String authorization, @Path("key") String key, @Query("fields") List<String> fields);

    @POST("aspects")
    @Headers("Content-Type: application/json")
    Call<Aspect> postAspect(@Header(AUTHORIZATION) String authorization, @Body Aspect subject);

    @PATCH("aspects/{key}")
    @Headers("Content-Type: application/json")
    Call<Aspect> patchAspect(@Header(AUTHORIZATION) String authorization, @Path("key") String key, @Body Aspect subject);
}
