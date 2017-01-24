/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model.builder;

import com.salesforce.refocus.model.Aspect;
import com.salesforce.refocus.model.Link;

import java.util.List;

import static com.salesforce.pyplyn.util.CollectionUtils.mutableListCopyOrNull;
import static com.salesforce.pyplyn.util.CollectionUtils.nullableArrayCopy;

/**
 * Builder class for Refocus Aspect objects
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AspectBuilder {
    private String id;
    private String name;
    private String description;
    private String helpEmail;
    private Boolean isPublished;
    private double[] criticalRange;
    private double[] warningRange;
    private double[] infoRange;
    private double[] okRange;
    private String timeout;
    private String valueType;
    private List<String> tags;
    private List<Link> relatedLinks;

    /**
     * Default constructor
     */
    public AspectBuilder() {
    }

    /**
     * Copy constructor
     */
    public AspectBuilder(Aspect aspect) {
        this.id = aspect.id();
        this.name = aspect.name();
        this.description = aspect.description();
        this.helpEmail = aspect.helpEmail();
        this.isPublished = aspect.isPublished();
        this.criticalRange = nullableArrayCopy(aspect.criticalRange());
        this.warningRange = nullableArrayCopy(aspect.warningRange());
        this.infoRange = nullableArrayCopy(aspect.infoRange());
        this.okRange = nullableArrayCopy(aspect.okRange());
        this.timeout = aspect.timeout();
        this.valueType = aspect.valueType();
        this.tags = mutableListCopyOrNull(aspect.tags());
        this.relatedLinks = mutableListCopyOrNull(aspect.relatedLinks());
    }


    /**
     * Builds a new Aspect from the updated field list
     */
    public Aspect build() {
        return new Aspect(id, name, description, helpEmail, isPublished, criticalRange,
                warningRange, infoRange, okRange, timeout, valueType, tags, relatedLinks);
    }


    /* Setters */

    public AspectBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public AspectBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public AspectBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public AspectBuilder withHelpEmail(String helpEmail) {
        this.helpEmail = helpEmail;
        return this;
    }

    public AspectBuilder withPublished(Boolean published) {
        isPublished = published;
        return this;
    }

    public AspectBuilder withCriticalRange(double[] criticalRange) {
        this.criticalRange = nullableArrayCopy(criticalRange);
        return this;
    }

    public AspectBuilder withWarningRange(double[] warningRange) {
        this.warningRange = nullableArrayCopy(warningRange);
        return this;
    }

    public AspectBuilder withInfoRange(double[] infoRange) {
        this.infoRange = nullableArrayCopy(infoRange);
        return this;
    }

    public AspectBuilder withOkRange(double[] okRange) {
        this.okRange = nullableArrayCopy(okRange);
        return this;
    }

    public AspectBuilder withTimeout(String timeout) {
        this.timeout = timeout;
        return this;
    }

    public AspectBuilder withValueType(String valueType) {
        this.valueType = valueType;
        return this;
    }

    public void withTags(List<String> tags) {
        this.tags = tags;
    }

    public void withRelatedLinks(List<Link> relatedLinks) {
        this.relatedLinks = relatedLinks;
    }
}
