/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.util.SensitiveByteArray;

/**
 * Refocus authentication response object
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableAuthResponse.class)
@JsonSerialize(as = ImmutableAuthResponse.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class AuthResponse {
    @JsonProperty("Success")
    public abstract String success();

    @Nullable
    @Value.Redacted
    public abstract String message();

    @Nullable
    @Value.Redacted
    @JsonSerialize(using=SensitiveByteArray.Serializer.class)
    @JsonDeserialize(using=SensitiveByteArray.Deserializer.class)
    public abstract byte[] token();
}
