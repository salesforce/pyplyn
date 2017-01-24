/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.salesforce.pyplyn.configuration.Configuration;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Augments {@link Configuration} by providing last run information
 * <p/>used to schedule the next time a Configuration should be processed
 * <p/>
 * <p/>This class is {@link Serializable}.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConfigurationWrapper implements Serializable {
    private static final long serialVersionUID = 5317732626880370818L;

    /**
     * Configuration object
     */
    private Configuration configuration;

    /**
     * Last time this configuration was run
     */
    private Long lastRun;


    /**
     * Class constructor
     *
     * @param configuration {@link Configuration} object to wrap
     */
    public ConfigurationWrapper(Configuration configuration, Long lastRun) {
        this.configuration = configuration;
        this.lastRun = lastRun;
    }

    /**
     * Returns the configuration represented by this wrapper
     */
    public Configuration configuration() {
        return configuration;
    }

    /**
     * Delegate for {@link Configuration#isEnabled()}
     */
    public boolean isEnabled() {
        return configuration.isEnabled();
    }

    /**
     * Determine if configuration should be run
     *
     * @param offsetMillis If specified (larger than zero), it will allow configurations to be processed
     *                     earlier, and will decrease the chance they are delayed until the full list is processed
     *                     If left equal to 0, it might cause configurations to be picked up after a full list is
     *                     finished.
     * @return true if configuration should be processed in the current cycle
     */
    public boolean shouldRun(long offsetMillis) {
        // negative offsets are not supported and hence not considered
        if (offsetMillis < 0) {
            return nextRun() <= System.currentTimeMillis();

            // determine if the config should run
        } else {
            return nextRun() - offsetMillis <= System.currentTimeMillis();
        }
    }

    /**
     * @return the timestamp in milliseconds at which this configuration should be run
     */
    public long nextRun() {
        // if this configuration was never run, execute it straight away
        if (isNull(lastRun)) {
            return System.currentTimeMillis();
        }

        // run after the interval has passed
        return lastRun + configuration.repeatIntervalMillis();
    }

    /**
     * Update configuration object represented by this wrapper
     */
    public void update(Configuration replaceConfiguration) {
        synchronized (this) {
            this.configuration = replaceConfiguration;
        }
    }

    /**
     * Mark time at which configuration was last run
     *
     * @return this object (fluent interface implementation)
     */
    public ConfigurationWrapper ran() {
        synchronized (this) {
            lastRun = System.currentTimeMillis();
        }
        return this;
    }

    /**
     * @return the number of milliseconds this configuration should be run after; if negative, returns 0 (run right now)
     */
    public long nextRunInMilliseconds() {
        return Math.max(nextRun() - System.currentTimeMillis(), 0);
    }

    /**
     * Delegates equality to {@link Configuration#equals(Object)}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigurationWrapper that = (ConfigurationWrapper) o;

        return configuration.equals(that.configuration);
    }

    @Override
    public int hashCode() {
        return configuration.hashCode();
    }

    /**
     * Allows sorting configurations in their next run proximity order
     */
    public static class NextRunComparator implements Comparator<ConfigurationWrapper>, Serializable {
        @Override
        public int compare(ConfigurationWrapper o1, ConfigurationWrapper o2) {
            return Long.compare(o1.nextRun(), o2.nextRun());
        }
    }
}
