/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import static com.salesforce.pyplyn.model.ThresholdType.GREATER_THAN;
import static com.salesforce.pyplyn.model.ThresholdType.LESS_THAN;

import com.salesforce.pyplyn.model.ThresholdType;

/**
 * Predefined alert meter types, defined on Metered processors ({@link com.salesforce.pyplyn.processor.AbstractMeteredExtractProcessor}
 *   and {@link com.salesforce.pyplyn.processor.AbstractMeteredLoadProcessor})
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public enum MeterType {
    ExtractSuccess(LESS_THAN),
    ExtractFailure(GREATER_THAN),
    ExtractNoDataReturned(GREATER_THAN),
    LoadSuccess(LESS_THAN),
    LoadFailure(GREATER_THAN),
    AuthenticationFailure(GREATER_THAN),
    ConfigurationUpdateFailure(GREATER_THAN);

    private final ThresholdType alertType;

    /**
     * Enum constructor
     */
    MeterType(ThresholdType alertType) {
        this.alertType = alertType;
    }

    /**
     * @return the alert's type
     * @see ThresholdType
     */
    public ThresholdType alertType() {
        return alertType;
    }
}
