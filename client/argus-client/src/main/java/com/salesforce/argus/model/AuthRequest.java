/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.util.CollectionUtils;
import com.salesforce.pyplyn.util.SensitiveByteArraySerializer;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Auth request model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class AuthRequest {
    @JsonProperty
    private final String username;

    @JsonProperty
    @JsonSerialize(using=SensitiveByteArraySerializer.class)
    private final byte[] password;

    /**
     * Constructs an Auth Request
     *   password is kept as byte to make it harder to extract from a heap dump (in plain text)
     *
     * @param username
     * @param password
     */
    @JsonCreator
    public AuthRequest(@JsonProperty("username") String username,
                       @JsonProperty("password") byte[] password) {
        this.username = username;
        this.password = CollectionUtils.nullableArrayCopy(password);
    }
}
