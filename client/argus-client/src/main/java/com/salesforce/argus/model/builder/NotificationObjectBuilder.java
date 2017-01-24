/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.NotificationObject;

import static com.salesforce.pyplyn.util.CollectionUtils.nullableArrayCopy;

/**
 * Argus notification object builder
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 */
public class NotificationObjectBuilder {
    private Long id;
    private Long createdById;
    private Long createdDate;
    private Long modifiedById;
    private Long modifiedDate;
    private String name;
    private String notifierName;
    private String[] subscriptions;
    private String[] metricsToAnnotate;
    private Long cooldownPeriod;
    private Long cooldownExpiration;
    private Long[] triggerIds;
    private Long alertId;
    private String customText;
    private Boolean SRactionable;


    /**
     * Default constructor
     */
    public NotificationObjectBuilder() {
    }

    /**
     * Copy constructor
     */
    public NotificationObjectBuilder(NotificationObject notificationObject) {
        this.id = notificationObject.id();
        this.createdById = notificationObject.createdById();
        this.createdDate = notificationObject.createdDate();
        this.modifiedById = notificationObject.modifiedById();
        this.modifiedDate = notificationObject.modifiedDate();
        this.name = notificationObject.name();
        this.notifierName = notificationObject.notifierName();
        this.subscriptions = nullableArrayCopy(notificationObject.subscriptions());
        this.metricsToAnnotate = nullableArrayCopy(notificationObject.metricsToAnnotate());
        this.cooldownPeriod = notificationObject.cooldownPeriod();
        this.cooldownExpiration = notificationObject.cooldownExpiration();
        this.triggerIds = nullableArrayCopy(notificationObject.triggerIds());
        this.alertId = notificationObject.alertId();
        this.customText = notificationObject.customText();
        this.SRactionable = notificationObject.isSRactionable();
    }


    /**
     * Builds a new NotificationObject from the updated field list
     */
    public NotificationObject build() {
        return new NotificationObject(id, createdById, createdDate, modifiedById, modifiedDate, name, notifierName,
                subscriptions, metricsToAnnotate, cooldownPeriod, cooldownExpiration, triggerIds, alertId, customText,
                SRactionable);
    }


    /* Setters */

    public NotificationObjectBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public NotificationObjectBuilder withCreatedById(Long createdById) {
        this.createdById = createdById;
        return this;
    }

    public NotificationObjectBuilder withCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public NotificationObjectBuilder withModifiedById(Long modifiedById) {
        this.modifiedById = modifiedById;
        return this;
    }

    public NotificationObjectBuilder withModifiedDate(Long modifiedDate) {
        this.modifiedDate = modifiedDate;
        return this;
    }

    public NotificationObjectBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public NotificationObjectBuilder withNotifierName(String notifierName) {
        this.notifierName = notifierName;
        return this;
    }

    public NotificationObjectBuilder withSubscriptions(String[] subscriptions) {
        this.subscriptions = nullableArrayCopy(subscriptions);
        return this;
    }

    public NotificationObjectBuilder withMetricsToAnnotate(String[] metricsToAnnotate) {
        this.metricsToAnnotate = nullableArrayCopy(metricsToAnnotate);
        return this;
    }

    public NotificationObjectBuilder withCooldownPeriod(Long cooldownPeriod) {
        this.cooldownPeriod = cooldownPeriod;
        return this;
    }

    public NotificationObjectBuilder withCooldownExpiration(Long cooldownExpiration) {
        this.cooldownExpiration = cooldownExpiration;
        return this;
    }

    public NotificationObjectBuilder withTriggerIds(Long[] triggerIds) {
        this.triggerIds = nullableArrayCopy(triggerIds);
        return this;
    }

    public NotificationObjectBuilder withAlertId(Long alertId) {
        this.alertId = alertId;
        return this;
    }

    public NotificationObjectBuilder withCustomText(String customText) {
        this.customText = customText;
        return this;
    }

    public NotificationObjectBuilder withSRactionable(Boolean SRactionable) {
        this.SRactionable = SRactionable;
        return this;
    }
}
