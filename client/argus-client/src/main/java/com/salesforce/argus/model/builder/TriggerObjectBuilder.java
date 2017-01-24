/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.TriggerObject;

import static com.salesforce.pyplyn.util.CollectionUtils.nullableArrayCopy;

/**
 * Argus trigger object builder
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 */
public class TriggerObjectBuilder {
    private Long id;
    private Long createdById;
    private Long createdDate;
    private Long modifiedById;
    private Long modifiedDate;
    private String type;
    private String name;
    private Double threshold;
    private Double secondaryThreshold;
    private Long inertia;
    private Long alertId;
    private Long notificationIds[];

    
    /**
     * Default constructor
     */
    public TriggerObjectBuilder() {
    }

    /**
     * Copy constructor
     */
    public TriggerObjectBuilder(TriggerObject triggerObject) {
		this.id = triggerObject.id();
		this.createdById = triggerObject.createdById();
		this.createdDate = triggerObject.createdDate();
		this.modifiedById = triggerObject.modifiedById();
		this.modifiedDate = triggerObject.modifiedDate();
		this.type = triggerObject.type();
		this.name = triggerObject.name();
		this.threshold = triggerObject.threshold();
		this.secondaryThreshold = triggerObject.secondaryThreshold();
		this.inertia = triggerObject.inertia();
		this.alertId = triggerObject.alertId();
		this.notificationIds = nullableArrayCopy(triggerObject.notificationIds());
	}


    /**
     * Builds a new TriggerObject from the updated field list
     */
    public TriggerObject build() {
        return new TriggerObject(id, createdById, createdDate, modifiedById, modifiedDate, type, name, threshold,
                secondaryThreshold, inertia, alertId, notificationIds);
    }


    /* Setters */

    public TriggerObjectBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TriggerObjectBuilder withCreatedById(Long createdById) {
        this.createdById = createdById;
        return this;
    }

    public TriggerObjectBuilder withCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public TriggerObjectBuilder withModifiedById(Long modifiedById) {
        this.modifiedById = modifiedById;
        return this;
    }

    public TriggerObjectBuilder withModifiedDate(Long modifiedDate) {
        this.modifiedDate = modifiedDate;
        return this;
    }

    public TriggerObjectBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public TriggerObjectBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TriggerObjectBuilder withThreshold(Double threshold) {
        this.threshold = threshold;
        return this;
    }

    public TriggerObjectBuilder withSecondaryThreshold(Double secondaryThreshold) {
        this.secondaryThreshold = secondaryThreshold;
        return this;
    }

    public TriggerObjectBuilder withInertia(Long inertia) {
        this.inertia = inertia;
        return this;
    }

    public TriggerObjectBuilder withAlertId(Long alertId) {
        this.alertId = alertId;
        return this;
    }

    public TriggerObjectBuilder withNotificationIds(Long[] notificationIds) {
        this.notificationIds = nullableArrayCopy(notificationIds);
        return this;
    }
}
