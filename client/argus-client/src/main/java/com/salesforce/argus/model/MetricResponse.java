/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.util.Map;
import java.util.SortedMap;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Iterables;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.cache.Cacheable;

/**
 * Metric response model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableMetricResponse.class)
@JsonSerialize(as = ImmutableMetricResponse.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
@Serial.Structural
public abstract class MetricResponse implements Cacheable {
    @Nullable
    public abstract String scope();

    public abstract String metric();

    public abstract Map<String, String> tags();

    @Nullable
    public abstract String namespace();

    @Nullable
    public abstract String displayName();

    @Nullable
    public abstract String units();

    @Value.NaturalOrder
    public abstract SortedMap<String, String> datapoints();

    @Override
    @Value.Derived
    @Value.Auxiliary
    @JsonIgnore
    public String cacheKey() {
        return metric();
    }

    /**
     * Generates a "name"=value string; the total number of retrieved datapoints will be printed
     * <p/>  if a datapoint map is defined, and it is not empty, the last value will also be printed
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append('"').append(metric()).append('"');

        // print information about the datapoints (if data is available)
        if (!datapoints().isEmpty()) {
            String lastValue = Iterables.getLast(datapoints().values());
            sb.append('=').append(lastValue);
        }

        sb.append(" (").append(datapoints().size()).append(" data points)");

        return sb.toString();
    }
}
