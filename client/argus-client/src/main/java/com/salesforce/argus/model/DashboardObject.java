/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

/**
 * Argus dashboard object model. This represents both metadata and the dashboard content.
 *
 * @author thomas.harris
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardObject {
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
    private final String content;

    @JsonProperty
    private final String ownerName;

    @JsonProperty
    private final Boolean shared;

    @JsonProperty
    private final String description;

    @JsonCreator
    public DashboardObject(@JsonProperty("id") Long id,
                           @JsonProperty("createdById") Long createdById,
                           @JsonProperty("createdDate") Long createdDate,
                           @JsonProperty("modifiedById") Long modifiedById,
                           @JsonProperty("modifiedDate") Long modifiedDate,
                           @JsonProperty("name") String name,
                           @JsonProperty("content") String content,
                           @JsonProperty("ownerName") String ownerName,
                           @JsonProperty("shared") Boolean shared,
                           @JsonProperty("description") String description) {
        this.id = id;
        this.createdById = createdById;
        this.createdDate = createdDate;
        this.modifiedById = modifiedById;
        this.modifiedDate = modifiedDate;
        this.name = name;
        this.content = content;
        this.ownerName = ownerName;
        this.shared = shared;
        this.description = description;
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

    public String content() {
        return content;
    }

    public String ownerName() {
        return ownerName;
    }

    public Boolean isShared() {
        return shared;
    }

    public String description() {
        return description;
    }
}
