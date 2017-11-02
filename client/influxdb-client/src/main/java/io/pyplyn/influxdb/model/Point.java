/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package io.pyplyn.influxdb.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;

/**
 * InfluxDB Point model, which can be serialized into the Line Protocol format.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutablePoint.class)
@JsonSerialize(as = ImmutablePoint.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class Point<T> {

    /**
     * The part of InfluxDB’s structure that describes the data stored in the associated fields.
     */
    public abstract String measurement();

    /**
     * The collection of tag keys and tag values on a point, used to store metadata. These are also indexed by InfluxDB,
     *   to speed up queries.
     */
    public abstract Map<String, String> tags();

    /**
     * The key part of the key-value pair that makes up a field.
     */
    @Value.Default
    public String key() {
        return "value";
    }

    /**
     * The value part of the key-value pair that makes up a field.
     * <p/> Field values are the actual data; they can be strings, floats, integers, or booleans.
     */
    public abstract T value();

    /**
     * Timestamp in nanos (without nano-precision)
     *
     * @return If not specified, it returns the current time
     */
    @Value.Default
    public Long timestamp() {
        return Instant.now().toEpochMilli() * 1_000_000;
    }

    /**
     * Formats this point into the InfluxDB Line Protocol format, i.e.:
     * <p/>measurement key=value timestamp'
     * <p/>measurement,tag1=tag_value1,tag2=tag_value2 key=value timestamp'
     */
    @Override
    public String toString() {
        String tags = tags().entrySet().stream().map(tag -> "," + tag.getKey() + "=" + tag.getValue()).collect(Collectors.joining());
        return measurement()
            + tags
            + " " + key() + "=" + value()
            + " " + timestamp();
    }
}
