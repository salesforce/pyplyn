/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;

/**
 * Argus dashboard object model. This represents both metadata and the dashboard content.
 *
 * @author thomas.harris
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableDashboardObject.class)
@JsonSerialize(as = ImmutableDashboardObject.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class DashboardObject {
    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract Long id();

    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract Long createdById();

    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract Long createdDate();

    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract Long modifiedById();

    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract Long modifiedDate();

    @Nullable
    public abstract String name();

    @Nullable
    public abstract String content();

    @Nullable
    public abstract String ownerName();

    @Nullable
    public abstract Boolean shared();

    @Nullable
    public abstract String description();
}
