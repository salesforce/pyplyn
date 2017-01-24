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

import java.util.List;

import static com.salesforce.pyplyn.util.CollectionUtils.immutableListOrNull;

/**
 * Builder class for Refocus Sample objects
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class SampleBuilder {
    private String id;
    private String name;
    private String value;
    private String updatedAt;
    private List<String> tags;
    private List<Link> relatedLinks;
    private String messageCode;
    private String messageBody;


    /**
     * Default constructor
     */
    public SampleBuilder() {
    }

    /**
     * Copy constructor
     */
    public SampleBuilder(Sample sample) {
        this.id = sample.id();
        this.name = sample.name();
        this.value = sample.value();
        this.updatedAt = sample.updatedAt();
        this.tags = immutableListOrNull(sample.tags());
        this.relatedLinks = immutableListOrNull(sample.relatedLinks());
        this.messageCode = sample.messageCode();
        this.messageBody = sample.messageBody();
    }


    /**
     * Builds a new Sample from the updated field list
     */
    public Sample build() {
        return new Sample(id, name, value, updatedAt, tags, relatedLinks, messageCode, messageBody);
    }


    /* Setters */

    public SampleBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public SampleBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public SampleBuilder withValue(String value) {
        this.value = value;
        return this;
    }

    public SampleBuilder withUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public SampleBuilder withTags(List<String> tags) {
        this.tags = immutableListOrNull(tags);
        return this;
    }

    public SampleBuilder withRelatedLinks(List<Link> relatedLinks) {
        this.relatedLinks = immutableListOrNull(relatedLinks);
        return this;
    }

    public SampleBuilder withMessageCode(String messageCode) {
        this.messageCode = messageCode;
        return this;
    }

    public SampleBuilder withMessageBody(String messageBody) {
        this.messageBody = messageBody;
        return this;
    }
}
