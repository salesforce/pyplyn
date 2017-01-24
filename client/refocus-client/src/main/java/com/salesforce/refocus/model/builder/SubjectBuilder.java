/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model.builder;

import com.salesforce.refocus.model.Link;
import com.salesforce.refocus.model.Sample;
import com.salesforce.refocus.model.Subject;

import java.util.List;

import static com.salesforce.pyplyn.util.CollectionUtils.immutableListOrNull;
import static com.salesforce.pyplyn.util.CollectionUtils.mutableListCopyOrNull;

/**
 * Builder class for Refocus Subjects
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class SubjectBuilder {
    private String id;
    private String parentId;
    private String name;
    private String absolutePath;
    private String parentAbsolutePath;
    private String description;
    private Boolean isPublished;
    private List<Subject> children;
    private List<String> tags;
    private List<Sample> samples;
    private List<Link> relatedLinks;


    /**
     * Default constructor
     */
    public SubjectBuilder() {
    }

    /**
     * Copy constructor
     */
    public SubjectBuilder(Subject subject) {
        id = subject.id();
        parentId = subject.parentId();
        name = subject.name();
        absolutePath = subject.absolutePath();
        parentAbsolutePath = subject.parentAbsolutePath();
        description = subject.description();
        isPublished = subject.isPublished();
        children = mutableListCopyOrNull(subject.children());
        tags = mutableListCopyOrNull(subject.tags());
        samples = mutableListCopyOrNull(subject.samples());
        relatedLinks = mutableListCopyOrNull(subject.relatedLinks());
    }


    /**
     * Builds a new Subject from the updated field list
     */
    public Subject build() {
        return new Subject(id, parentId, name, absolutePath, parentAbsolutePath, description, isPublished, children, tags, samples, relatedLinks);
    }


    /* Setters */

    public SubjectBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public SubjectBuilder withParentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    public SubjectBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public SubjectBuilder withAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
        return this;
    }

    public SubjectBuilder withParentAbsolutePath(String parentAbsolutePath) {
        this.parentAbsolutePath = parentAbsolutePath;
        return this;
    }

    public SubjectBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public SubjectBuilder withPublished(Boolean published) {
        isPublished = published;
        return this;
    }

    public SubjectBuilder withChildren(List<Subject> children) {
        this.children = immutableListOrNull(children);
        return this;
    }

    public SubjectBuilder withTags(List<String> tags) {
        this.tags = immutableListOrNull(tags);
        return this;
    }

    public SubjectBuilder withSamples(List<Sample> samples) {
        this.samples = immutableListOrNull(samples);
        return this;
    }

    public SubjectBuilder withRelatedLinks(List<Link> relatedLinks) {
        this.relatedLinks = immutableListOrNull(relatedLinks);
        return this;
    }
}
