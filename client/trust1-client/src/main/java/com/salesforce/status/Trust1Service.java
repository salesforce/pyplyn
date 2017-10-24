/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.status;

import com.salesforce.status.model.Instance;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

import java.util.List;

public interface Trust1Service {

    @GET("instances/status/preview")
    @Headers("Accept: application/json")
    Call<List<Instance>> instancePreview(@Query("productId") String productId, @Query("childProducts") boolean childProducts, @Query("locale") String locale);

}
