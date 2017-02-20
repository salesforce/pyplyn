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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.util.SensitiveByteArraySerializer;

import java.util.Arrays;

import static com.salesforce.pyplyn.util.CollectionUtils.nullableArrayCopy;

/**
 * Auth request model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AuthRequest {
    @JsonProperty
    private final String username;

    @JsonProperty
    @JsonSerialize(using=SensitiveByteArraySerializer.class)
    private final byte[] password;

    /**
     * Constructs an Auth Request
     * <p/>
     * <p/>The password is kept as a byte array to make it harder to extract from a heap dump (byte arrays can be nulled-out)
     */
    @JsonCreator
    public AuthRequest(@JsonProperty("username") String username,
                       @JsonProperty("password") byte[] password) {
        this.username = username;
        this.password = nullableArrayCopy(password);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthRequest that = (AuthRequest) o;

        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return Arrays.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(password);
        return result;
    }
}
