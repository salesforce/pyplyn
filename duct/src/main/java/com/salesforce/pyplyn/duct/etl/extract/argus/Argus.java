/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.argus;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.Extract;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Argus datasource model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableArgus.class)
@JsonSerialize(as = ImmutableArgus.class)
public abstract class Argus implements Extract {
    private static final long serialVersionUID = 8271965988988568032L;

    /**
     * Endpoint where the expression should be executed on
     */
    public abstract String endpoint();

    /**
     * Expression to load metrics for
     */
    public abstract String expression();

    /**
     * Name of expression
     */
    public abstract String name();

    /**
     * How long to cache this expression's results
     */
    @Value.Default
    @Value.Auxiliary
    public int cacheMillis() {
        return 0;
    }

    /**
     * If no results are returned from the endpoint,
     *   having this parameter specified causes the processor to generate one datapoint
     *   with this value and the current time (at the time of execution)
     */
    @Nullable
    @Value.Auxiliary
    public abstract Double defaultValue();

    /**
     * @return the cache key for this object
     */
    @Value.Auxiliary
    public final String cacheKey() {
        return name();
    }
}
