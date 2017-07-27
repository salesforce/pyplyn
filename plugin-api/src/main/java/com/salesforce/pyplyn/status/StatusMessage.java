/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import com.salesforce.pyplyn.model.StatusCode;

import java.util.List;

/**
 * Represents a system status message
 */
public class StatusMessage {
    /**
     * Alert level
     */
    private final StatusCode level;

    /**
     * Alert message
     */
    private final String message;

    /**
     * Simplified message constructor used when a rate is not required
     */
    public StatusMessage(StatusCode level, String message) {
        this.level = level;
        this.message = message;
    }

    /**
     * @return the alert's level
     */
    public StatusCode level() {
        return level;
    }

    /**
     * Called by {@link SystemStatusConsumer#accept(List)} to report this message
     */
    @Override
    public String toString() {
        return message;
    }
}
