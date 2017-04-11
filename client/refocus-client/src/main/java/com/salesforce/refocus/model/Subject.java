/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import com.fasterxml.jackson.annotation.*;

import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;
import static com.salesforce.pyplyn.util.CollectionUtils.immutableListOrNull;
import static com.salesforce.pyplyn.util.CollectionUtils.immutableOrEmptyList;

/**
 * Subject model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class Subject {
    @JsonProperty(access = WRITE_ONLY)
    private final String id;

    @JsonProperty
    private final String parentId;

    @JsonProperty
    private final String name;

    @JsonProperty(access = WRITE_ONLY)
    private final String absolutePath;

    @JsonProperty(access = WRITE_ONLY)
    private final String parentAbsolutePath;

    @JsonProperty
    private final String description;

    @JsonProperty
    private final Boolean isPublished;

    @JsonProperty(access = WRITE_ONLY)
    private final List<Subject> children;

    @JsonProperty
    private final List<String> tags;

    @JsonProperty(access = WRITE_ONLY)
    private final List<Sample> samples;

    @JsonProperty
    private final List<Link> relatedLinks;

    @JsonCreator
    public Subject(@JsonProperty("id") String id,
                   @JsonProperty("parentId") String parentId,
                   @JsonProperty("name") String name,
                   @JsonProperty("absolutePath") String absolutePath,
                   @JsonProperty("parentAbsolutePath") String parentAbsolutePath,
                   @JsonProperty("description") String description,
                   @JsonProperty("isPublished") boolean isPublished,
                   @JsonProperty("children") List<Subject> children,
                   @JsonProperty("tags") List<String> tags,
                   @JsonProperty("samples") List<Sample> samples,
                   @JsonProperty("relatedLinks") List<Link> relatedLinks) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.absolutePath = absolutePath;
        this.parentAbsolutePath = parentAbsolutePath;
        this.description = description;
        this.isPublished = isPublished;
        this.children = immutableListOrNull(children);
        this.tags = immutableListOrNull(tags);
        this.samples = immutableListOrNull(samples);
        this.relatedLinks = immutableListOrNull(relatedLinks);
    }

    public String id() {
        return id;
    }

    public String parentId() {
        return parentId;
    }

    public String name() {
        return name;
    }

    public String absolutePath() {
        return absolutePath;
    }

    public String parentAbsolutePath() {
        return parentAbsolutePath;
    }

    public List<Subject> children() {
        return immutableOrEmptyList(children);
    }

    public List<String> tags() {
        return immutableOrEmptyList(tags);
    }

    public String description() {
        return description;
    }

    @JsonIgnore
    public boolean isPublished() {
        return isPublished;
    }

    public List<Sample> samples() {
        return immutableOrEmptyList(samples);
    }

    public List<Link> relatedLinks() {
        return immutableOrEmptyList(relatedLinks);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Subject{")
                .append(Objects.toString(absolutePath, name))
                .append(", parent=")
                .append(Objects.toString(parentAbsolutePath, "null"))
                .append(", parentId=")
                .append(Objects.toString(parentId, "null"))
                .append('}')
                .toString();
    }
}
