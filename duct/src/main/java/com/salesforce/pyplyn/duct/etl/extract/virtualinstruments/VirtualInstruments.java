/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.virtualinstruments;

import com.salesforce.pyplyn.util.AbsoluteOrRelativeTime;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.duct.etl.extract.influxdb.ImmutableInfluxDB;
import com.salesforce.pyplyn.model.Extract;

/**
 * Virtual Instruments data source model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.2.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableVirtualInstruments.class)
@JsonSerialize(as = ImmutableVirtualInstruments.class)
@JsonTypeName("VirtualInstruments")
public abstract class VirtualInstruments implements Extract {
    private static final long serialVersionUID = 2273965988988568032L;

    /**
     * Endpoint where the query should be executed on
     */
    public abstract String endpoint();

    /**
     * @return time from which to start loading metrics (expressed in milliseconds from epoch or
     *   relative formats described in {@link AbsoluteOrRelativeTime})
     */
    @JsonDeserialize(using = AbsoluteOrRelativeTime.Deserializer.class)
    public abstract Long startTime();

    /**
     * @return time until which metrics are loaded (expressed in milliseconds from epoch or
     *   relative formats described in {@link AbsoluteOrRelativeTime})
     */
    @JsonDeserialize(using = AbsoluteOrRelativeTime.Deserializer.class)
    public abstract Long endTime();

    /**
     * @return entity type name
     */
    public abstract String entityType();

    /**
     * @return metric to load data for
     */
    public abstract String metricName();

    /**
     * @return the delay between polling requests
     */
    @Value.Default
    public Long pollingIntervalMillis() {
        return 500L;
    }

    /**
     * @return the number of milliseconds after which to abort attempting to poll for results
     */
    @Value.Default
    public Long pollingTimeoutMillis() {
        return 5_000L;
    }

}
