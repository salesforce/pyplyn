/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;

import java.io.Serializable;
import java.util.Arrays;

import static com.salesforce.pyplyn.util.CollectionUtils.nullableArrayCopy;

/**
 * ETL Configuration model
 * <p/>
 * <p/>This class is the base of all the {@link Extract}/{@link Transform}/{@link Load} definitions and defines the high-level structure
 *   of what should be retrieved, how it should be processed, and where it should be delivered to
 * <p/>
 * <p/>This class is {@link Serializable}.
 *
 * @see Extract data sources
 * @see Transform transformation to apply on the extracted data
 * @see Load where to publish the transformation results
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class Configuration implements Serializable {
    private static final long serialVersionUID = 3589803365589594172L;

    @JsonProperty(required = true)
    private long repeatIntervalMillis;

    @JsonProperty(required = true)
    private Extract[] extract;

    @JsonProperty(required = true)
    private Transform[] transform;

    @JsonProperty(required = true)
    private Load[] load;

    @JsonProperty
    private boolean disabled;

    @JsonCreator
    public Configuration(@JsonProperty("repeatIntervalMillis") long repeatIntervalMillis,
                         @JsonProperty("extract") Extract[] extract,
                         @JsonProperty("transform") Transform[] transform,
                         @JsonProperty("load") Load[] load,
                         @JsonProperty("disabled") boolean disabled) {
        this.repeatIntervalMillis = repeatIntervalMillis;
        this.extract = nullableArrayCopy(extract);
        this.transform = nullableArrayCopy(transform);
        this.load = nullableArrayCopy(load);
        this.disabled = disabled;
    }

    /**
     * @return how often this configuration should be processed
     */
    public long repeatIntervalMillis() {
        return repeatIntervalMillis;
    }

    /**
     * @return the data sources to extract from; this result is safe to alter and will not affect currently loaded configurations
     */
    public Extract[] extract() {
        return Arrays.copyOf(extract, extract.length);
    }

    /**
     * @return the transformations that need to be applied; this result is safe to alter and will not affect currently loaded configurations
     */
    public Transform[] transform() {
        return Arrays.copyOf(transform, transform.length);
    }

    /**
     * @return the load destinations where results should be pushed; this result is safe to alter and will not affect currently loaded configurations
     */
    public Load[] load() {
        return Arrays.copyOf(load, load.length);
    }

    /**
     * Checks if this configuration is enabled for processing
     * <p/>
     * <p/>This allows configurations to be quickly disabled in case they are causing issues
     *   but when it is not desired to remove them completely.
     *
     * @return true by default, or otherwise the specified value
     */
    public boolean isEnabled() {
        return !disabled;
    }

    /**
     * Determines if configurations are alike.
     * <p/>
     * <p/>This relies on all child classes of Extract, Transform, Load having implemented equals()
     *
     * @return True if all ETL parameters represent the same settings
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Configuration that = (Configuration) o;

        if (!Arrays.equals(extract, that.extract)) {
            return false;
        }
        if (!Arrays.equals(transform, that.transform)) {
            return false;
        }

        return Arrays.equals(load, that.load);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Arrays.hashCode(extract) + Arrays.hashCode(transform)) + Arrays.hashCode(load);
    }
}
