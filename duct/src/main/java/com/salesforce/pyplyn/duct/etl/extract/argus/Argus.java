/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.argus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.pyplyn.model.Extract;

import java.io.Serializable;
import java.util.Optional;

/**
 * Argus datasource model
 * <p/> immutable
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class Argus implements Extract, Serializable {
    private static final long serialVersionUID = 8271965988988568032L;

    @JsonProperty(required = true)
    private String endpoint;

    @JsonProperty(required = true)
    private String expression;

    @JsonProperty(required = true)
    private String name;

    @JsonProperty(defaultValue = "0")
    private Integer cacheMillis;

    @JsonProperty
    private Double defaultValue;


    /**
     * Endpoint where the expression should be executed on
     */
    String endpoint() {
        return endpoint;
    }

    /**
     * Expression to load metrics for
     */
    String expression() {
        return expression;
    }

    /**
     * Name of expression
     */
    String name() {
        return name;
    }

    /**
     * How long to cache this expression's results
     */
    int cacheMillis() {
        return Optional.ofNullable(cacheMillis).orElse(0);
    }

    /**
     * If no results are returned from the endpoint,
     *   having this parameter specified causes the processor to generate one datapoint
     *   with this value and the current time (at the time of execution)
     */
    Double defaultValue() {
        return defaultValue;
    }

    /**
     * Returns the cache key for this object
     *
     * @return
     */
    final String cacheKey() {
        return cacheKeyFor(endpoint(), name());
    }

    /**
     * Standardizes key names used to cache and retrieve Argus results
     */
    static String cacheKeyFor(String endpoint, String name) {
        return endpoint + ":" + name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Argus argus = (Argus) o;

        if (!endpoint.equals(argus.endpoint)) {
            return false;
        }
        if (!expression.equals(argus.expression)) {
            return false;
        }

        return name.equals(argus.name);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * endpoint.hashCode() + expression.hashCode()) + name.hashCode();
    }
}
