/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.virtualinstruments;

import com.virtualinstruments.model.ReportPayload;
import com.virtualinstruments.model.ReportResponse;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

/**
 * VirtualInstruments service
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.2.0
 */
public interface VirtualInstrumentsService {

    @POST("sec/login")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    Call<ResponseBody> login(@Body RequestBody credentials);

    @PUT("analytics/reportBatch")
    @Headers("Content-Type: application/json")
    Call<Void> reportBatch(@Header("Cookie") String sessionId, @Body ReportPayload payload);

    @GET("analytics/poll/reportPoll")
    @Headers("Accept: application/json")
    Call<ReportResponse> reportPoll(@Header("Cookie") String sessionId, @Query("uuid") String reportUuid);

}
