/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.virtualinstruments;

import com.virtualinstruments.model.AuthRequest;
import com.virtualinstruments.model.ReportPayload;
import com.virtualinstruments.model.ReportResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

/**
 * VirtualInstruments service
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.2.0
 */
public interface VirtualInstrumentsService {

    @POST("sec/login")
    @Headers("Content-Type: application/json")
    Call<Map<String, Object>> login(@Body AuthRequest request);

    @PUT("analytics/reportBatch")
    @Headers("Accept: application/json")
    Call<Void> reportBatch(@Body ReportPayload payload);

    @GET("analytics/poll/reportPoll")
    @Headers("Accept: application/json")
    Call<ReportResponse> reportPoll(@Query("uuid") String reportUuid);

}
