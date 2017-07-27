/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.appconfig;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;


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
@Value.Immutable
@Value.Enclosing
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableAppConfig.class)
@JsonSerialize(as = ImmutableAppConfig.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AppConfig {
    public abstract Global global();

    @Nullable
    public abstract Hazelcast hazelcast();

    @Nullable
    public abstract Alert alert();


    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableAppConfig.Global.class)
    @JsonSerialize(as = ImmutableAppConfig.Global.class)
    public static abstract class Global {
        public abstract String configurationsPath();

        public abstract String connectorsPath();

        /**
         * This parameter will be removed in future versions
         *
         * @see {@link Global#runOnce}
         * @deprecated
         */
        @Deprecated
        @Value.Default
        public long minRepeatIntervalMillis() {
            return 60_000L;
        }

        @Value.Default
        public boolean runOnce() {
            return minRepeatIntervalMillis() <= 0;
        }

        public abstract Long updateConfigurationIntervalMillis();

        @Value.Default
        public int ioPoolsThreadSize() {
            return 200;
        }
    }

    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableAppConfig.Hazelcast.class)
    @JsonSerialize(as = ImmutableAppConfig.Hazelcast.class)
    public static abstract class Hazelcast {
        @Value.Default
        @JsonProperty("enabled")
        public boolean isEnabled() {
            return false;
        }

        public abstract String config();
    }

    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableAppConfig.Alert.class)
    @JsonSerialize(as = ImmutableAppConfig.Alert.class)
    public static abstract class Alert {
        @Value.Default
        @JsonProperty("enabled")
        public boolean isEnabled() {
            return false;
        }

        @JsonProperty(defaultValue = "300000")
        @Value.Default
        public long checkIntervalMillis() {
            return 300_000L;
        }

        public abstract Map<String, Double> thresholds();
    }
}
