/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.refocus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.pyplyn.model.Extract;

import java.io.Serializable;
import java.util.Optional;

/**
 * Refocus datasource model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class Refocus implements Extract, Serializable {
    private static final long serialVersionUID = -2303603367792896790L;

    @JsonProperty(required = true)
    private String endpoint;

    @JsonProperty(required = true)
    private String subject;

    @JsonProperty
    private String actualSubject;

    @JsonProperty(required = true)
    private String aspect;

    @JsonProperty(defaultValue = "0")
    private Integer cacheMillis;

    @JsonProperty
    private Double defaultValue;


    /**
     * Default constructor
     */
    @JsonCreator
    public Refocus(@JsonProperty("endpoint") String endpoint,
                   @JsonProperty("subject") String subject,
                   @JsonProperty("actualSubject") String actualSubject,
                   @JsonProperty("aspect") String aspect,
                   @JsonProperty("cacheMillis") Integer cacheMillis,
                   @JsonProperty("defaultValue") Double defaultValue) {
        this.endpoint = endpoint;
        this.subject = subject;
        this.actualSubject = actualSubject;
        this.aspect = aspect;
        this.cacheMillis = cacheMillis;
        this.defaultValue = defaultValue;
    }

    /**
     * Endpoint where the expression should be executed on
     */
    public String endpoint() {
        return endpoint;
    }

    /**
     * Subject to load data for; can include wildcards
     */
    public String subject() {
        return subject;
    }

    /**
     * Used to filter samples returned from the Refocus endpoint by the name specified in this field;
     *   useful when wanting to cache multiple samples that match a wildcard, but only return one
     *   <p/>For example, given: subject=Subject.Root.*, actualSubject=Subject.Root.Name, aspect=ASPECT, cacheMillis=60000
     *   <p/>All returned samples that match "Subject.Root.*|ASPECT" will be cached for 60s but only "Subject.Root.Name|ASPECT"
     *   will be returned by {@link RefocusExtractProcessor}
     *
     * @return {@link Refocus#actualSubject} if specified, or otherwise {@link Refocus#subject}, since in that case they are the same
     */
    public String actualSubject() {
        return Optional.ofNullable(actualSubject).orElse(subject);
    }

    /**
     * Aspect to load
     */
    public String aspect() {
        return aspect;
    }

    /**
     * How long to cache this expression's results
     */
    public int cacheMillis() {
        return Optional.ofNullable(cacheMillis).orElse(0);
    }

    /**
     * If no results are returned from the endpoint,
     *   having this parameter specified causes the processor to generate one datapoint
     *   with this value and the current time (at the time of execution)
     */
    public Double defaultValue() {
        return defaultValue;
    }


    /**
     * @return the Refocus standardized sample name (i.e.: SUBJECT.PATH|ASPECT)
     */
    public final String name() {
        return String.format("%s|%s", subject(), aspect);
    }

    /**
     * @return the Refocus filtered sample name (i.e.: SUBJECT.PATH.*|ASPECT)
     */
    public final String filteredName() {
        return String.format("%s|%s", actualSubject(), aspect);
    }

    /**
     * @return the cache key for this object
     */
    public final String cacheKey() {
        return filteredName();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Refocus refocus = (Refocus) o;

        if (!endpoint.equals(refocus.endpoint)) {
            return false;
        }
        if (!subject.equals(refocus.subject)) {
            return false;
        }

        return aspect.equals(refocus.aspect);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * endpoint.hashCode() + subject.hashCode()) + aspect.hashCode();
    }
}
