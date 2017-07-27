/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.cache.Cacheable;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;
import static java.util.Objects.nonNull;

/**
 * Sample model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableSample.class)
@JsonSerialize(as = ImmutableSample.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class Sample implements Cacheable {
    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract String id();

    public abstract String name();

    @Nullable
    public abstract String value();

    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract String updatedAt();

    public abstract List<String> tags();

    public abstract List<Link> relatedLinks();

    @Nullable
    public abstract String messageCode();

    @Nullable
    public abstract String messageBody();

    @Override
    @Value.Derived
    @Value.Auxiliary
    public String cacheKey() {
        return name();
    }

    /**
     * Generates a "sample"=value string
     * <p/>  if value is null, only returns "sample"
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append('"').append(name()).append('"');

        if (nonNull(value())) {
            sb.append('=').append(value());
        }

        return sb.toString();
    }
}
