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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriggerObject {
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
    private final String type;

    @JsonProperty
    private final String name;

    @JsonProperty
    private final Double threshold;

    @JsonProperty
    private final Double secondaryThreshold;

    @JsonProperty
    private final Long inertia;

    @JsonProperty
    private final Long alertId;

    @JsonProperty
    private final Long notificationIds[];

	public TriggerObject(@JsonProperty("id") Long id,
						 @JsonProperty("createdById") Long createdById,
						 @JsonProperty("createdDate") Long createdDate,
						 @JsonProperty("modifiedById") Long modifiedById,
						 @JsonProperty("modifiedDate") Long modifiedDate,
						 @JsonProperty("type") String type,
						 @JsonProperty("name") String name,
						 @JsonProperty("threshold") Double threshold,
						 @JsonProperty("secondaryThreshold") Double secondaryThreshold,
						 @JsonProperty("inertia") Long inertia,
						 @JsonProperty("alertId") Long alertId,
						 @JsonProperty("notificationIds") Long[] notificationIds) {
		this.id = id;
		this.createdById = createdById;
		this.createdDate = createdDate;
		this.modifiedById = modifiedById;
		this.modifiedDate = modifiedDate;
		this.type = type;
		this.name = name;
		this.threshold = threshold;
		this.secondaryThreshold = secondaryThreshold;
		this.inertia = inertia;
		this.alertId = alertId;
		this.notificationIds = nullableArrayCopy(notificationIds);
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

	public String type() {
		return type;
	}

	public String name() {
		return name;
	}

	public Double threshold() {
		return threshold;
	}

	public Double secondaryThreshold() {
		return secondaryThreshold;
	}

	public Long inertia() {
		return inertia;
	}

	public Long alertId() {
		return alertId;
	}

	public Long[] notificationIds() {
		return nullableArrayCopy(notificationIds);
	}
}
