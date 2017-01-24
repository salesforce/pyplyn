/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.appconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;


/**
 * Holds application configuration parameters
 * <p/>
 * <p/> This object is the starting point for all settings and directories passed to Pyplyn. Only the <b>global</b>
 *   section is required, the others can be skipped if not requiring the specific functionality they define.
 * <p/>
 * <p/>The <i>ignoreUnknown</i> Jackson parameter is specified because we want to let extending plugins to
 *   piggyback on the same configuration file and define extra properties without causing this object to fail
 *   deserialization due to unknown properties.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
    @JsonProperty(required = true)
    private Global global;

    @JsonProperty
    private Hazelcast hazelcast;

    @JsonProperty
    private Alert alert;

    public Global global() {
        return global;
    }

    public Hazelcast hazelcast() {
        return hazelcast;
    }

    public Alert alert() {
        return alert;
    }


    public static class Global {
        @JsonProperty(required = true)
        private String configurationsPath;

        @JsonProperty(required = true)
        private String connectorsPath;

        @JsonProperty(required = true)
        private Long minRepeatIntervalMillis;

        @JsonProperty(required = true)
        private Long updateConfigurationIntervalMillis;

        public String configurationsPath() {
            return configurationsPath;
        }

        public String connectorsPath() {
            return connectorsPath;
        }

        public long minRepeatIntervalMillis() {
            return minRepeatIntervalMillis;
        }

        public long updateConfigurationIntervalMillis() {
            return updateConfigurationIntervalMillis;
        }
    }

    public static class Hazelcast {
        @JsonProperty(defaultValue = "false")
        private Boolean enabled;

        @JsonProperty(required = true)
        private String config;

        public boolean isEnabled() {
            return Optional.ofNullable(enabled).orElse(Boolean.FALSE);
        }

        public String config() {
            return config;
        }
    }

    public static class Alert {
        @JsonProperty(defaultValue = "false")
        private boolean enabled;

        @JsonProperty(defaultValue = "300000")
        private Long checkIntervalMillis;

        @JsonProperty(required = true)
        private Map<String, Double> thresholds;

        public boolean isEnabled() {
            return Optional.ofNullable(enabled).orElse(Boolean.FALSE);
        }

        public long checkIntervalMillis() {
            return Optional.ofNullable(checkIntervalMillis).orElse(300000L);
        }

        public Map<String, Double> thresholds() {
            return Collections.unmodifiableMap(thresholds);
        }
    }
}
