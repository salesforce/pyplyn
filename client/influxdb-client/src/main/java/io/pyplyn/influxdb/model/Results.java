/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package io.pyplyn.influxdb.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;

/**
 * InfluxDB query results model class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
@Value.Immutable
@Value.Enclosing
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableResults.class)
@JsonSerialize(as = ImmutableResults.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class Results {

    public abstract List<Wrapper> results();

    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableResults.Statement.class)
    @JsonSerialize(as = ImmutableResults.Statement.class)
    public static abstract class Statement {

        @Nullable
        public abstract String name();

        public abstract List<String> columns();

        public abstract List<List<Object>> values();
    }

    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableResults.Wrapper.class)
    @JsonSerialize(as = ImmutableResults.Wrapper.class)
    public static abstract class Wrapper {
        @JsonProperty("statement_id")
        public abstract Integer statementId();

        @JsonProperty("series")
        public abstract List<Statement> series();
    }

}
