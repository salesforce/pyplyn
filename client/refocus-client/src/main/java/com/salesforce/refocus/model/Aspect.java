/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;
import static com.salesforce.pyplyn.util.CollectionUtils.immutableOrEmptyList;
import static com.salesforce.pyplyn.util.CollectionUtils.nullableArrayCopy;

/**
 * Aspect model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class Aspect {
    @JsonProperty(access = WRITE_ONLY)
    private final String id;

    @JsonProperty
    private final String name;

    @JsonProperty
    private final String description;

    @JsonProperty
    private final String helpEmail;

    @JsonProperty
    private final Boolean isPublished;

    @JsonProperty
    private final double[] criticalRange;

    @JsonProperty
    private final double[] warningRange;

    @JsonProperty
    private final double[] infoRange;

    @JsonProperty
    private final double[] okRange;

    @JsonProperty
    private final String timeout;

    @JsonProperty
    private final String valueType;

    @JsonProperty
    private final List<String> tags;

    @JsonProperty
    private final List<Link> relatedLinks;


    @JsonCreator
    public Aspect(@JsonProperty("id") String id,
                  @JsonProperty("name") String name,
                  @JsonProperty("description") String description,
                  @JsonProperty("helpEmail") String helpEmail,
                  @JsonProperty("isPublished") Boolean isPublished,
                  @JsonProperty("criticalRange") double[] criticalRange,
                  @JsonProperty("warningRange") double[] warningRange,
                  @JsonProperty("infoRange") double[] infoRange,
                  @JsonProperty("okRange") double[] okRange,
                  @JsonProperty("timeout") String timeout,
                  @JsonProperty("valueType") String valueType,
                  @JsonProperty("tags") List<String> tags,
                  @JsonProperty("relatedLinks") List<Link> relatedLinks) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.helpEmail = helpEmail;
        this.isPublished = isPublished;
        this.criticalRange = nullableArrayCopy(criticalRange);
        this.warningRange = nullableArrayCopy(warningRange);
        this.infoRange = nullableArrayCopy(infoRange);
        this.okRange = nullableArrayCopy(okRange);
        this.timeout = timeout;
        this.valueType = valueType;
        this.tags = tags;
        this.relatedLinks = relatedLinks;
    }


    /* Getters */

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String helpEmail() {
        return helpEmail;
    }

    public Boolean isPublished() {
        return isPublished;
    }

    public double[] criticalRange() {
        return nullableArrayCopy(criticalRange);
    }

    public double[] warningRange() {
        return nullableArrayCopy(warningRange);
    }

    public double[] infoRange() {
        return nullableArrayCopy(infoRange);
    }

    public double[] okRange() {
        return nullableArrayCopy(okRange);
    }

    public String timeout() {
        return timeout;
    }

    public String valueType() {
        return valueType;
    }

    public List<String> tags() {
        return immutableOrEmptyList(tags);
    }

    public List<Link> relatedLinks() {
        return immutableOrEmptyList(relatedLinks);
    }

    @Override
    public String toString() {
        return "Aspect{" + name + '}';
    }
}
