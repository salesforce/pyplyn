/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.AlertObject;

import static com.salesforce.pyplyn.util.CollectionUtils.nullableArrayCopy;

/**
 * Argus alert object builder
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 */
public class AlertObjectBuilder {
    private Long id;
    private Long createdById;
    private Long createdDate;
    private Long modifiedById;
    private Long modifiedDate;
    private String name;
    private String expression;
    private String cronEntry;
    private Boolean enabled;
    private Boolean missingDataNotificationEnabled;
    private Long[] notificationsIds;
    private Long[] triggersIds;
    private String ownerName;
    private Boolean shared;

    /**
     * Default constructor
     */
    public AlertObjectBuilder() {
    }

    /**
     * Copy constructor
     */
    public AlertObjectBuilder(AlertObject alertObject) {
        this.id = alertObject.id();
        this.createdById = alertObject.createdById();
        this.createdDate = alertObject.createdDate();
        this.modifiedById = alertObject.modifiedById();
        this.modifiedDate = alertObject.modifiedDate();
        this.name = alertObject.name();
        this.expression = alertObject.expression();
        this.cronEntry = alertObject.cronEntry();
        this.enabled = alertObject.isEnabled();
        this.missingDataNotificationEnabled = alertObject.isMissingDataNotificationEnabled();
        this.notificationsIds = nullableArrayCopy(alertObject.notificationsIds());
        this.triggersIds = nullableArrayCopy(alertObject.triggersIds());
        this.ownerName = alertObject.ownerName();
        this.shared = alertObject.isShared();
    }


    /**
     * Builds a new {@link AlertObject} from the updated field list
     */
    public AlertObject build() {
        return new AlertObject(id, createdById, createdDate, modifiedById, modifiedDate, name, expression, cronEntry,
                enabled, missingDataNotificationEnabled, notificationsIds, triggersIds, ownerName, shared);
    }


    /* Setters */

    public AlertObjectBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public AlertObjectBuilder withCreatedById(Long createdById) {
        this.createdById = createdById;
        return this;
    }

    public AlertObjectBuilder withCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public AlertObjectBuilder withModifiedById(Long modifiedById) {
        this.modifiedById = modifiedById;
        return this;
    }

    public AlertObjectBuilder withModifiedDate(Long modifiedDate) {
        this.modifiedDate = modifiedDate;
        return this;
    }

    public AlertObjectBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public AlertObjectBuilder withExpression(String expression) {
        this.expression = expression;
        return this;
    }

    public AlertObjectBuilder withCronEntry(String cronEntry) {
        this.cronEntry = cronEntry;
        return this;
    }

    public AlertObjectBuilder withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AlertObjectBuilder withMissingDataNotificationEnabled(Boolean missingDataNotificationEnabled) {
        this.missingDataNotificationEnabled = missingDataNotificationEnabled;
        return this;
    }

    public AlertObjectBuilder withNotificationsIds(Long[] notificationsIds) {
        this.notificationsIds = nullableArrayCopy(notificationsIds);
        return this;
    }

    public AlertObjectBuilder withTriggersIds(Long[] triggersIds) {
        this.triggersIds = nullableArrayCopy(triggersIds);
        return this;
    }

    public AlertObjectBuilder withOwnerName(String ownerName) {
        this.ownerName = ownerName;
        return this;
    }

    public AlertObjectBuilder withShared(Boolean shared) {
        this.shared = shared;
        return this;
    }
}
