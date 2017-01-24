/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Refocus authentication response object
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AuthResponse {

    @JsonProperty("Success")
    private final String success;

    @JsonProperty
    private final String message;

    @JsonProperty
    private final String token;

    @JsonCreator
    public AuthResponse(@JsonProperty("success") String success,
                        @JsonProperty("message") String message,
                        @JsonProperty("token") String token) {
        this.success = success;
        this.message = message;
        this.token = token;
    }

    /* Getters */

    public String success() {
        return success;
    }

    public String message() {
        return message;
    }

    public String token() {
        return token;
    }
}
