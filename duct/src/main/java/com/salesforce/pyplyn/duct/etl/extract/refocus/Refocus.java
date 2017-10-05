/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.refocus;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.Extract;

/**
 * Refocus datasource model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableRefocus.class)
@JsonSerialize(as = ImmutableRefocus.class)
@JsonTypeName("Refocus")
public abstract class Refocus implements Extract {
    private static final long serialVersionUID = -2303603367792896790L;

    /**
     * Endpoint where the expression should be executed on
     */
    public abstract String endpoint();

    /**
     * Subject to load data for; can include wildcards
     */
    public abstract String subject();

    /**
     * Used to filter samples returned from the Refocus endpoint by the name specified in this field;
     *   useful when wanting to cache multiple samples that match a wildcard, but only return one
     *   <p/>For example, given: subject=Subject.Root.*, actualSubject=Subject.Root.Name, aspect=ASPECT, cacheMillis=60000
     *   <p/>All returned samples that match "Subject.Root.*|ASPECT" will be cached for 60s but only "Subject.Root.Name|ASPECT"
     *   will be returned by {@link RefocusExtractProcessor}
     *
     * @return {@link Refocus#actualSubject} if specified, or otherwise {@link Refocus#subject}, since in that case they are the same
     */
    @Nullable
    @Value.Default
    public String actualSubject() {
        return subject();
    }

    /**
     * Aspect to load
     */
    public abstract String aspect();

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
     * @return the Refocus standardized sample name (i.e.: SUBJECT.PATH|ASPECT)
     */
    @Value.Auxiliary
    public final String name() {
        return String.format("%s|%s", subject(), aspect());
    }

    /**
     * @return the Refocus filtered sample name (i.e.: SUBJECT.PATH.*|ASPECT)
     */
    @Value.Auxiliary
    public final String filteredName() {
        return String.format("%s|%s", actualSubject(), aspect());
    }

    /**
     * @return the cache key for this object
     */
    @Value.Auxiliary
    public final String cacheKey() {
        return filteredName();
    }
}
