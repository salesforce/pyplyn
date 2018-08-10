/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

import java.io.Serializable;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;

/**
 * ETL Configuration model
 * <p/>
 * <p/>
 * This class is the base of all the {@link Extract}/{@link Transform}/{@link Load} definitions and defines the high-level structure
 * of what should be retrieved, how it should be processed, and where it should be delivered to
 * <p/>
 * <p/>
 * This class is {@link Serializable}.
 *
 * @see Extract data sources
 * @see Transform transformation to apply on the extracted data
 * @see Load where to publish the transformation results
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableConfiguration.class)
@JsonSerialize(as = ImmutableConfiguration.class)
public abstract class Configuration implements Serializable {
    private static final long serialVersionUID = 3589803365589594172L;

    /**
     * @return how often this configuration should be processed
     */
    @Value.Auxiliary
    public abstract long repeatIntervalMillis();

    /**
     * @return the data sources to extract from; this result is safe to alter and will not affect currently loaded configurations
     */
    public abstract List<Extract> extract();

    /**
     * @return the transformations that need to be applied; this result is safe to alter and will not affect currently loaded configurations
     */
    public abstract List<Transform> transform();

    /**
     * @return the load destinations where results should be pushed; this result is safe to alter and will not affect currently loaded configurations
     */
    public abstract List<Load> load();

    @Value.Default
    @Value.Auxiliary
    public boolean disabled() {
        return false;
    }
}
