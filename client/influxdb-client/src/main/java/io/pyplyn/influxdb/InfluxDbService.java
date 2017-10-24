/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package io.pyplyn.influxdb;

import io.pyplyn.influxdb.model.Results;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

/**
 * InfluxDB 1.3 Service class for Retrofit
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public interface InfluxDbService {

    @POST("write")
    Call<Void> writePoints(@Query("db") String db, @Body RequestBody data);

    @GET("query")
    @Headers("Content-Type: application/json")
    Call<Results> query(@Query("db") String db, @Query("q") String query);

}
