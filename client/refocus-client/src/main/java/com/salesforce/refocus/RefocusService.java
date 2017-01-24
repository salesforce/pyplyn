/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus;

import com.salesforce.refocus.model.*;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.*;

import java.util.List;

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

    /* Authentication */

    @POST("authenticate")
    @Headers("Content-Type: application/json")
    Call<AuthResponse> authenticate(@Body AuthRequest body);

    /* Samples operations */

    @GET("samples/{key}")
    @Headers("Content-Type: application/json")
    Call<Sample> getSample(@Header("Authorization") String authorization, @Path("key") String key, @Query("fields") List<String> fields);

    @POST("samples/upsert/bulk")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> upsertSamplesBulk(@Header("Authorization") String authorization, @Body List<Sample> samples);

    @DELETE("samples/{key}")
    @Headers("Content-Type: application/json")
    Call<Sample> deleteSample(@Header("Authorization") String authorization, @Path("key") String key);


    /* Subjects operations */

    @GET("subjects")
    @Headers("Content-Type: application/json")
    Call<List<Subject>> getSubjects(@Header("Authorization") String authorization, @Query("fields") List<String> fields);

    @GET("subjects/{key}")
    @Headers("Content-Type: application/json")
    Call<Subject> getSubject(@Header("Authorization") String authorization, @Path("key") String key, @Query("fields") List<String> fields);

    @GET("subjects/{key}/hierarchy")
    @Headers("Content-Type: application/json")
    Call<Subject> getSubjectHierarchy(@Header("Authorization") String authorization, @Path("key") String key, @Query("status") String status);

    @POST("subjects")
    @Headers("Content-Type: application/json")
    Call<Subject> postSubject(@Header("Authorization") String authorization, @Body Subject subject);

    @PATCH("subjects/{key}")
    @Headers("Content-Type: application/json")
    Call<Subject> patchSubject(@Header("Authorization") String authorization, @Path("key") String key, @Body Subject subject);


    /* Aspect operations */

    @GET("aspects")
    @Headers("Content-Type: application/json")
    Call<List<Aspect>> getAspects(@Header("Authorization") String authorization, @Query("fields") List<String> fields);

    @GET("aspects/{key}")
    @Headers("Content-Type: application/json")
    Call<Aspect> getAspect(@Header("Authorization") String authorization, @Path("key") String key, @Query("fields") List<String> fields);

    @POST("aspects")
    @Headers("Content-Type: application/json")
    Call<Aspect> postAspect(@Header("Authorization") String authorization, @Body Aspect subject);

    @PATCH("aspects/{key}")
    @Headers("Content-Type: application/json")
    Call<Aspect> patchAspect(@Header("Authorization") String authorization, @Path("key") String key, @Body Aspect subject);
}
