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

import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;

/**
 * Argus alert object model
 * <p/>
 * <p/>An Alert consists of a metric expression and a list of associated Notification IDs. In order to notify
 *   the recipients associated with the Notification, a Triggers must be created and associated with the
 *   Alert that fires when the expression reaches a particular threshold.
 *
 * @author thomas.harris
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableAlertObject.class)
@JsonSerialize(as = ImmutableAlertObject.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class AlertObject {
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
    public abstract String expression();

    @Nullable
    public abstract String cronEntry();

    @Nullable
    public abstract Boolean enabled();

    @Nullable
    public abstract Boolean missingDataNotificationEnabled();

    public abstract List<Long> notificationsIds();

    public abstract List<Long> triggersIds();

    @Nullable
    public abstract String ownerName();

    @Nullable
    public abstract Boolean shared();
}
