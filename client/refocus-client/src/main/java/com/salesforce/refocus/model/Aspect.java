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
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

/**
 * Aspect model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableAspect.class)
@JsonSerialize(as = ImmutableAspect.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class Aspect {
    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract String id();

    @Nullable
    public abstract String name();

    @Nullable
    public abstract String description();

    @Nullable
    public abstract String helpEmail();

    @Nullable
    public abstract Boolean isPublished();

    public abstract List<Double> criticalRange();

    public abstract List<Double> warningRange();

    public abstract List<Double> infoRange();

    public abstract List<Double> okRange();

    @Nullable
    public abstract String timeout();

    @Nullable
    public abstract String valueType();

    public abstract List<String> tags();

    public abstract List<Link> relatedLinks();
}
