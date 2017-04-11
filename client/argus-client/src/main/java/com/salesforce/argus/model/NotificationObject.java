/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;
import static com.salesforce.pyplyn.util.CollectionUtils.nullableArrayCopy;

/**
 * Argus notification object representation.
 * <p/>
 * <p/>A notification is a target (email address, chatter group, GOC++) that is configured to be alerted
 *   when a trigger condition is met. A notification can be tied to multiple triggers.
 *
 * @author thomas.harris
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationObject {
    @JsonProperty(access = WRITE_ONLY)
    private final Long id;

    @JsonProperty(access = WRITE_ONLY)
    private final Long createdById;

    @JsonProperty(access = WRITE_ONLY)
    private final Long createdDate;

    @JsonProperty(access = WRITE_ONLY)
    private final Long modifiedById;

    @JsonProperty(access = WRITE_ONLY)
    private final Long modifiedDate;

    @JsonProperty
    private final String name;

    @JsonProperty
    private final String notifierName;

    @JsonProperty
    private final String[] subscriptions;

    @JsonProperty
    private final String[] metricsToAnnotate;

    @JsonProperty
    private final Long cooldownPeriod;

    @JsonProperty
    private final Long cooldownExpiration;

    @JsonProperty("triggersIds")
    private final Long[] triggerIds;

    @JsonProperty
    private final Long alertId;

    @JsonProperty
    private final String customText;

    @JsonProperty
    private final Integer severityLevel;

    @JsonProperty("sractionable")
    private final Boolean SRactionable;

    public NotificationObject(@JsonProperty("id") Long id,
                              @JsonProperty("createdById") Long createdById,
                              @JsonProperty("createdDate") Long createdDate,
                              @JsonProperty("modifiedById") Long modifiedById,
                              @JsonProperty("modifiedDate") Long modifiedDate,
                              @JsonProperty("name") String name,
                              @JsonProperty("notifierName") String notifierName,
                              @JsonProperty("subscriptions") String[] subscriptions,
                              @JsonProperty("metricsToAnnotate") String[] metricsToAnnotate,
                              @JsonProperty("cooldownPeriod") Long cooldownPeriod,
                              @JsonProperty("cooldownExpiration") Long cooldownExpiration,
                              @JsonProperty("triggersIds") Long[] triggerIds,
                              @JsonProperty("alertId") Long alertId,
                              @JsonProperty("customText") String customText,
                              @JsonProperty("severityLevel") Integer severityLevel,
                              @JsonProperty("sractionable") Boolean SRactionable) {
        this.id = id;
        this.createdById = createdById;
        this.createdDate = createdDate;
        this.modifiedById = modifiedById;
        this.modifiedDate = modifiedDate;
        this.name = name;
        this.notifierName = notifierName;
        this.subscriptions = nullableArrayCopy(subscriptions);
        this.metricsToAnnotate = nullableArrayCopy(metricsToAnnotate);
        this.cooldownPeriod = cooldownPeriod;
        this.cooldownExpiration = cooldownExpiration;
        this.triggerIds = nullableArrayCopy(triggerIds);
        this.alertId = alertId;
        this.customText = customText;
        this.severityLevel = severityLevel;
        this.SRactionable = SRactionable;
    }

    /* Getters */

    public Long id() {
        return id;
    }

    public Long createdById() {
        return createdById;
    }

    public Long createdDate() {
        return createdDate;
    }

    public Long modifiedById() {
        return modifiedById;
    }

    public Long modifiedDate() {
        return modifiedDate;
    }

    public String name() {
        return name;
    }

    public String notifierName() {
        return notifierName;
    }


    public String[] subscriptions() {
        return nullableArrayCopy(subscriptions);
    }

    public String[] metricsToAnnotate() {
        return nullableArrayCopy(metricsToAnnotate);
    }

    public Long cooldownPeriod() {
        return cooldownPeriod;
    }

    public Long cooldownExpiration() {
        return cooldownExpiration;
    }

    public Long[] triggerIds() {
        return nullableArrayCopy(triggerIds);
    }


    public Long alertId() {
        return alertId;
    }

    public String customText() {
        return customText;
    }

    public Integer severityLevel() {
        return severityLevel;
    }

    public Boolean isSRactionable() {
        return SRactionable;
    }
}
