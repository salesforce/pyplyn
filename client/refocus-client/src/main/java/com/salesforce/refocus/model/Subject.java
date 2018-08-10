/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.util.RawJsonCollectionDeserializer;

/**
 * Subject model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableSubject.class)
@JsonSerialize(as = ImmutableSubject.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class Subject {
    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract String id();

    @Nullable
    public abstract String parentId();

    public abstract String name();

    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract String absolutePath();

    @Nullable
    @JsonProperty(access = WRITE_ONLY)
    public abstract String parentAbsolutePath();

    @Nullable
    public abstract String description();

    @Nullable
    public abstract String helpEmail();

    @Nullable
    public abstract String helpUrl();

    public abstract boolean isPublished();

    @JsonProperty(access = WRITE_ONLY)
    public abstract List<Subject> children();

    @JsonDeserialize(using = RawJsonCollectionDeserializer.class)
    public abstract List<String> tags();

    @JsonProperty(access = WRITE_ONLY)
    public abstract List<Sample> samples();

    @JsonDeserialize(using = RawJsonCollectionDeserializer.class)
    public abstract List<Link> relatedLinks();

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Subject{")
                .append(Objects.toString(absolutePath(), name()))
                .append(", parent=")
                .append(Objects.toString(parentAbsolutePath(), "N/A"))
                .append(", parentId=")
                .append(Objects.toString(parentId(), "N/A"))
                .append('}')
                .toString();
    }
}
