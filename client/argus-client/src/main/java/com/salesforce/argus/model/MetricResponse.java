/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.pyplyn.cache.Cacheable;

import java.util.Map;
import java.util.SortedMap;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.salesforce.pyplyn.util.CollectionUtils.*;

/**
 * Metric response model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class MetricResponse implements Cacheable {
    @JsonProperty
    private String scope;

    @JsonProperty
    private String metric;

    @JsonProperty
    private Map<String,String> tags;

    @JsonProperty
    private String namespace;

    @JsonProperty
    private String displayName;

    @JsonProperty
    private String units;

    @JsonProperty
    private SortedMap<String,String> datapoints;

    @JsonCreator
    public MetricResponse(@JsonProperty("scope") String scope,
                          @JsonProperty("metric") String metric,
                          @JsonProperty("tags") Map<String, String> tags,
                          @JsonProperty("namespace") String namespace,
                          @JsonProperty("displayName") String displayName,
                          @JsonProperty("units") String units,
                          @JsonProperty("datapoints") SortedMap<String, String> datapoints) {
        this.scope = scope;
        this.metric = metric;
        this.tags = immutableMapOrNull(tags);
        this.namespace = namespace;
        this.displayName = displayName;
        this.units = units;
        this.datapoints = immutableSortedMapOrNull(datapoints);
    }

    public String scope() {
        return scope;
    }

    public String metric() {
        return metric;
    }

    public Map<String, String> tags() {
        return immutableOrEmptyMap(tags);
    }

    public String namespace() {
        return namespace;
    }

    public String displayName() {
        return displayName;
    }

    public String units() {
        return units;
    }

    public SortedMap<String, String> datapoints() {
        return immutableSortedOrEmptyMap(datapoints);
    }

    @Override
    public String cacheKey() {
        return metric;
    }
}
