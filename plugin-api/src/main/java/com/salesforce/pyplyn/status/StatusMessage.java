/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

/**
 * Represents a system status message
 */
public class StatusMessage {

    /**
     * Default "all ok" status message
     */
    static final String MESSAGE_NAME_OK = "All services";

    /**
     * Alert level
     */
    private final AlertLevel level;

    /**
     * Alert name
     */
    private final String name;

    /**
     * Alert message
     */
    private final String message;

    /**
     * Simplified message constructor used when a rate is not required ({@link Ok} messages)
     */
    public StatusMessage(AlertLevel level, String name) {
        this.level = level;
        this.name = name;
        this.message = String.format("%s %s", name, level);
    }

    /**
     * Constructs a status message, passing in its level, name, and rate
     */
    public StatusMessage(AlertLevel level, String name, double rate) {
        this.level = level;
        this.name = name;
        this.message = String.format("rate=%f/5m", rate);
    }

    /**
     * @return the alert's level
     */
    public AlertLevel level() {
        return level;
    }

    /**
     * Shorthand method to standardize how status messages are reported to {@link SystemStatusConsumer}s
     */
    @Override
    public String toString() {
        return String.format("%s %s (%s)", name, level, message);
    }


    /**
     * Represents the "ALL OK" status message
     * <p/>
     * <p/>Shorthand for reporting no errors.
     */
    public static class Ok extends StatusMessage {

        /**
         * Default constructor that calls its super constructor with "all ok"
         */
        public Ok() {
            super(AlertLevel.OK, MESSAGE_NAME_OK);
        }
    }
}
