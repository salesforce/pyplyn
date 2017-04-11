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
import com.salesforce.pyplyn.cache.Cacheable;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;
import static com.salesforce.pyplyn.util.CollectionUtils.immutableListOrNull;
import static com.salesforce.pyplyn.util.CollectionUtils.immutableOrEmptyList;
import static java.util.Objects.nonNull;

/**
 * Sample model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class Sample implements Cacheable {
    @JsonProperty(access = WRITE_ONLY)
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty
    private String value;

    @JsonProperty(access = WRITE_ONLY)
    private String updatedAt;

    @JsonProperty
    private final List<String> tags;

    @JsonProperty
    private List<Link> relatedLinks;

    @JsonProperty
    private String messageCode;

    @JsonProperty
    private String messageBody;

    @JsonCreator
    public Sample(@JsonProperty("id") String id,
                  @JsonProperty("name") String name,
                  @JsonProperty("value") String value,
                  @JsonProperty("updatedAt") String updatedAt,
                  @JsonProperty("tags") List<String> tags,
                  @JsonProperty("relatedLinks") List<Link> relatedLinks,
                  @JsonProperty("messageCode") String messageCode,
                  @JsonProperty("messageBody") String messageBody) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.updatedAt = updatedAt;
        this.tags = immutableListOrNull(tags);
        this.relatedLinks = immutableListOrNull(relatedLinks);
        this.messageCode = messageCode;
        this.messageBody = messageBody;
    }

    /* Getters */

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public String updatedAt() {
        return updatedAt;
    }

    public List<String> tags() {
        return immutableOrEmptyList(tags);
    }

    public List<Link> relatedLinks() {
        return immutableOrEmptyList(relatedLinks);
    }

    public String messageCode() {
        return messageCode;
    }

    public String messageBody() {
        return messageBody;
    }

    /**
     * Generates a "sample"=value string
     *   if value is null, only returns "sample"
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append('"').append(name).append('"');

        if (nonNull(value)) {
            sb.append('=').append(value);
        }

        return sb.toString();
    }

    @Override
    public String cacheKey() {
        return name;
    }
}
