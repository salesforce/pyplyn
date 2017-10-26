/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.virtualinstruments.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.util.SensitiveByteArray;

/**
 * Virtual Instruments request model
 * <p/>The password is kept as a byte array to avoid string-interning and make it harder to extract
 *   from a heap dump (byte arrays can be zeroed out)
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableAuthRequest.class)
@JsonSerialize(as = ImmutableAuthRequest.class)
@JsonInclude(NON_EMPTY)
public abstract class AuthRequest {
    @Nullable
    public abstract String username();

    @Value.Redacted
    @JsonSerialize(using=SensitiveByteArray.Serializer.class)
    public abstract byte[] password();
}
