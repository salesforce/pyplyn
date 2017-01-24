/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.DashboardObject;

/**
 * Argus dashboard object builder
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 */
public class DashboardObjectBuilder {
    private Long id;
    private Long createdById;
    private Long createdDate;
    private Long modifiedById;
    private Long modifiedDate;
    private String name;
    private String content;
    private String ownerName;
    private Boolean shared;
    private String description;

    /**
     * Default constructor
     */
    public DashboardObjectBuilder() {
    }

    /**
     * Copy constructor
     */
    public DashboardObjectBuilder(DashboardObject dashboardObject) {
        this.id = dashboardObject.id();
        this.createdById = dashboardObject.createdById();
        this.createdDate = dashboardObject.createdDate();
        this.modifiedById = dashboardObject.modifiedById();
        this.modifiedDate = dashboardObject.modifiedDate();
        this.name = dashboardObject.name();
        this.content = dashboardObject.content();
        this.ownerName = dashboardObject.ownerName();
        this.shared = dashboardObject.isShared();
        this.description = dashboardObject.description();
    }


    /**
     * Builds a new DashboardObject from the updated field list
     */
    public DashboardObject build() {
        return new DashboardObject(id, createdById, createdDate, modifiedById, modifiedDate, name, content, ownerName,
                shared, description);
    }


    /* Setters */

    public DashboardObjectBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public DashboardObjectBuilder withCreatedById(Long createdById) {
        this.createdById = createdById;
        return this;
    }

    public DashboardObjectBuilder withCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public DashboardObjectBuilder withModifiedById(Long modifiedById) {
        this.modifiedById = modifiedById;
        return this;
    }

    public DashboardObjectBuilder withModifiedDate(Long modifiedDate) {
        this.modifiedDate = modifiedDate;
        return this;
    }

    public DashboardObjectBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public DashboardObjectBuilder withContent(String content) {
        this.content = content;
        return this;
    }

    public DashboardObjectBuilder withOwnerName(String ownerName) {
        this.ownerName = ownerName;
        return this;
    }

    public DashboardObjectBuilder withShared(Boolean shared) {
        this.shared = shared;
        return this;
    }

    public DashboardObjectBuilder withDescription(String description) {
        this.description = description;
        return this;
    }
}
