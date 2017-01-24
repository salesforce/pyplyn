/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model;

import com.salesforce.pyplyn.model.builder.ETLMetadataBuilder;

import java.time.ZonedDateTime;

/**
 * This class is a model that is used to hold extract data while all {@link Transform}s are applied and until
 *   the result is loaded onto a destination.
 * <p/>
 * <p/>This object is immutable.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class TransformationResult {
    private final ZonedDateTime time;
    private final String name;
    private final Number value;
    private final Number originalValue;
    private final ETLMetadata metadata;

    /**
     * Default constructor
     *   used when no metadata exists (defaults to empty {@link ETLMetadata} object
     */
    public TransformationResult(ZonedDateTime time, String name, Number value, Number originalValue) {
        this(time, name, value, originalValue, new ETLMetadataBuilder().build());
    }

    /**
     * Class constructor used to specify all parameters, including {@link ETLMetadata}, which should not be null
     */
    public TransformationResult(ZonedDateTime time, String name, Number value, Number originalValue, ETLMetadata metadata) {
        this.time = time;
        this.name = name;
        this.value = value;
        this.originalValue = originalValue;
        this.metadata = metadata;
    }

    /**
     * @return the time that this result represents
     */
    public ZonedDateTime time() {
        return time;
    }

    /**
     * @return the name of this result, as returned by the {@link Extract} stage
     */
    public String name() {
        return name;
    }

    /**
     * @return the value returned from the remote endpoint during the {@link Extract} stage
     */
    public Number value() {
        return value;
    }

    /**
     * @return the original value returned by the remote endpoint; this will ensure the original value is always available,
     *         even after the value changed by applying {@link Transform}s
     */
    public Number originalValue() {
        return originalValue;
    }

    /**
     * @return metadata specific to this result; different {@link Transform}s may add details that could be used by a
     *         {@link com.salesforce.pyplyn.processor.LoadProcessor} and passed to the destination endpoint
     */
    public ETLMetadata metadata() {
        return metadata;
    }
}
