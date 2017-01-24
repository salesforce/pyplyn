/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;
import static com.salesforce.pyplyn.util.CollectionUtils.nullableArrayCopy;

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
public class AlertObject {
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
    private final String expression;

    @JsonProperty
    private final String cronEntry;

    @JsonProperty
    private final Boolean enabled;

    @JsonProperty
    private final Boolean missingDataNotificationEnabled;

    @JsonProperty
    private final Long[] notificationsIds;

    @JsonProperty
    private final Long[] triggersIds;

    @JsonProperty
    private final String ownerName;

    @JsonProperty
    private final Boolean shared;

    @JsonCreator
    public AlertObject(@JsonProperty("id") Long id,
                       @JsonProperty("createdById") Long createdById,
                       @JsonProperty("createdDate") Long createdDate,
                       @JsonProperty("modifiedById") Long modifiedById,
                       @JsonProperty("modifiedDate") Long modifiedDate,
                       @JsonProperty("name") String name,
                       @JsonProperty("expression") String expression,
                       @JsonProperty("cronEntry") String cronEntry,
                       @JsonProperty("enabled") Boolean enabled,
                       @JsonProperty("missingDataNotificationEnabled") Boolean missingDataNotificationEnabled,
                       @JsonProperty("notificationsIds") Long[] notificationsIds,
                       @JsonProperty("triggersIds") Long[] triggersIds,
                       @JsonProperty("ownerName") String ownerName,
                       @JsonProperty("shared") Boolean shared) {
        this.id = id;
        this.createdById = createdById;
        this.createdDate = createdDate;
        this.modifiedById = modifiedById;
        this.modifiedDate = modifiedDate;
        this.name = name;
        this.expression = expression;
        this.cronEntry = cronEntry;
        this.enabled = enabled;
        this.missingDataNotificationEnabled = missingDataNotificationEnabled;
        this.notificationsIds = nullableArrayCopy(notificationsIds);
        this.triggersIds = nullableArrayCopy(triggersIds);
        this.ownerName = ownerName;
        this.shared = shared;
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

    public String expression() {
        return expression;
    }

    public String cronEntry() {
        return cronEntry;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Boolean isMissingDataNotificationEnabled() {
        return missingDataNotificationEnabled;
    }

    public Long[] notificationsIds() {
        return nullableArrayCopy(notificationsIds);
    }

    public Long[] triggersIds() {
        return nullableArrayCopy(triggersIds);
    }

    public String ownerName() {
        return ownerName;
    }

    public Boolean isShared() {
        return shared;
    }
}
