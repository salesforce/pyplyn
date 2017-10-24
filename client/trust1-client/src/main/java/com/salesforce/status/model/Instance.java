/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.status.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.cache.Cacheable;
import com.salesforce.pyplyn.model.StatusCode;
import org.immutables.value.Value;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * Refocus API implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
@Value.Immutable
@Value.Enclosing
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableInstance.class)
@JsonSerialize(as = ImmutableInstance.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class Instance implements Cacheable {
    @Nullable
    public abstract Integer id();

    public abstract String key();

    public abstract String location();

    public abstract String environment();

    public abstract String releaseVersion();

    public abstract String releaseNumber();

    public abstract Status status();

    public abstract boolean isActive();

    @JsonProperty("Incidents")
    public abstract List<Map<String, Object>> incidents();

    @JsonProperty("Maintenances")
    public abstract List<Map<String, Object>> maintenances();

    @Override
    @Value.Derived
    @Value.Auxiliary
    public String cacheKey() {
        return key();
    }

    /**
     * Represents statuses returned by the Trust1 status API,
     *   mapped to a Pyplyn {@link StatusCode}
     */
    public enum Status {
        OK(StatusCode.OK),
        MAJOR_INCIDENT_CORE(StatusCode.CRIT),
        MINOR_INCIDENT_CORE(StatusCode.WARN),
        MAINTENANCE_CORE(StatusCode.WARN),
        INFORMATIONAL_CORE(StatusCode.INFO),
        MAJOR_INCIDENT_NONCORE(StatusCode.CRIT),
        MINOR_INCIDENT_NONCORE(StatusCode.WARN),
        MAINTENANCE_NONCORE(StatusCode.WARN),
        INFORMATIONAL_NONCORE(StatusCode.INFO);

        private final StatusCode code;

        /**
         * Enum constructor
         */
        Status(StatusCode code) {
            this.code = code;
        }

        /**
         * @return the corresponding {@link StatusCode}
         */
        public StatusCode code() {
            return code;
        }

        /**
         * @return the corresponding {@link StatusCode#value()}
         */
        public Double codeVal() {
            return code.value();
        }
    }
}
