/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
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
 * Argus notification object representation.
 * <p/>
 * <p/>A notification is a target (email address, chatter group, GOC++) that is configured to be alerted
 *   when a trigger condition is met. A notification can be tied to multiple triggers.
 *
 * @author thomas.harris
 * @author Mihai Bojin &lt();mbojin@salesforce.com&gt();
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableNotificationObject.class)
@JsonSerialize(as = ImmutableNotificationObject.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class NotificationObject {
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
    public abstract String notifierName();

    public abstract List<String> subscriptions();

    @JsonInclude(ALWAYS)
    public abstract List<String> metricsToAnnotate();

    @Nullable
    public abstract Long cooldownPeriod();

    @Nullable
    public abstract Long cooldownExpiration();

    @JsonProperty("triggersIds")
    public abstract List<Long> triggerIds();

    @Nullable
    public abstract Long alertId();

    @Nullable
    public abstract String customText();

    @Nullable
    public abstract Integer severityLevel();

    @Nullable
    @JsonProperty("sractionable")
    public abstract Boolean isSRactionable();
}
