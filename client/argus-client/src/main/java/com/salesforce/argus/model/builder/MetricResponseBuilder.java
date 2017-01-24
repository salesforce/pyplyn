/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.MetricResponse;

import java.util.Map;
import java.util.SortedMap;

import static com.salesforce.pyplyn.util.CollectionUtils.immutableMapOrNull;
import static com.salesforce.pyplyn.util.CollectionUtils.immutableSortedMapOrNull;

/**
 * Builder class for Argus MetricResponse objects
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class MetricResponseBuilder {
    private String scope;
    private String metric;
    private Map<String,String> tags;
    private String namespace;
    private String displayName;
    private String units;
    private SortedMap<String,String> datapoints;


    /**
     * Default constructor
     */
    public MetricResponseBuilder() {
    }

    /**
     * Copy constructor
     */
    public MetricResponseBuilder(MetricResponse metricResponse) {
        this.scope = metricResponse.scope();
        this.metric = metricResponse.metric();
        this.tags = metricResponse.tags();
        this.namespace = metricResponse.namespace();
        this.displayName = metricResponse.displayName();
        this.units = metricResponse.units();
        this.datapoints = metricResponse.datapoints();
    }


    /**
     * Builds a new MetricResponse from the updated field list
     */
    public MetricResponse build() {
        return new MetricResponse(scope, metric, tags, namespace, displayName, units, datapoints);
    }


    /* Setters */

    public MetricResponseBuilder withScope(String scope) {
        this.scope = scope;
        return this;
    }

    public MetricResponseBuilder withMetric(String metric) {
        this.metric = metric;
        return this;
    }

    public MetricResponseBuilder withTags(Map<String, String> tags) {
        this.tags = immutableMapOrNull(tags);
        return this;
    }

    public MetricResponseBuilder withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public MetricResponseBuilder withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public MetricResponseBuilder withUnits(String units) {
        this.units = units;
        return this;
    }

    public MetricResponseBuilder withDatapoints(SortedMap<String, String> datapoints) {
        this.datapoints = immutableSortedMapOrNull(datapoints);
        return this;
    }
}
