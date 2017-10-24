/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.influxdb;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.Extract;
import org.immutables.value.Value;

/**
 * InfluxDB datasource model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.1.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableInfluxDB.class)
@JsonSerialize(as = ImmutableInfluxDB.class)
@JsonTypeName("InfluxDB")
public abstract class InfluxDB implements Extract {
    private static final long serialVersionUID = 8271965988988568032L;

    /**
     * Endpoint where the query should be executed on
     */
    public abstract String endpoint();

    /**
     * Query to load results for
     */
    public abstract String query();

    /**
     * Unique name to identify the current query
     */
    public abstract String name();
}
