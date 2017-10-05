/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;

/**
 * This class is a model that is used to hold extract data while all {@link Transform}s are applied and until
 *   the result is loaded onto a destination.
 * <p/>
 * <p/>This object is immutable.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@Value.Enclosing
@PyplynImmutableStyle
public abstract class Transmutation {
    /**
     * @return the time that this result represents
     */
    public abstract ZonedDateTime time();

    /**
     * @return the name of this result, as returned by the {@link Extract} stage
     */
    public abstract String name();

    /**
     * @return the value returned from the remote endpoint during the {@link Extract} stage
     */
    public abstract Number value();

    /**
     * @return the original value returned by the remote endpoint; this will ensure the original value is always available,
     *         even after the value changed by applying {@link Transform}s
     */
    public abstract Number originalValue();


    /**
     * @return metadata specific to this result; different {@link Transform}s may add details that could be used by a
     *         {@link com.salesforce.pyplyn.processor.LoadProcessor} and passed to the destination endpoint
     */
    public abstract Metadata metadata();

    @Value.Immutable
    @PyplynImmutableStyle
    public static abstract class Metadata {
        /**
         * @return message code passed
         */
        @Nullable
        public abstract String messageCode();

        /**
         * @return all defined messages
         */
        public abstract List<String> messages();

        /**
         * @return all defined tags
         */
        public abstract Map<String, String> tags();

        /**
         * @return the source data used to generate this datapoint
         */
        @Nullable
        public abstract Object source();
    }
}
