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
 * Argus trigger object representation.
 * <p/>
 * <p/>A trigger represents a threshold at which the metric expression defined in the associated Alert will notify
 *   the specified set of recipients. For example, email a particular user when the value of the expression drops
 *   below 100.
 * <p/>
 * Use multiple trigger objects for an alert to support different warn/error thresholds and different recipients
 *   based on the severity of a problem.
 *
 * @author thomas.harris
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableTriggerObject.class)
@JsonSerialize(as = ImmutableTriggerObject.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class TriggerObject {
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
    public abstract String type();

    @Nullable
    public abstract String name();

    @Nullable
    public abstract Double threshold();

    @Nullable
    public abstract Double secondaryThreshold();

    @Nullable
    public abstract Long inertia();

    @Nullable
    public abstract Long alertId();

    public abstract List<Long> notificationIds();
}
