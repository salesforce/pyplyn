/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import com.salesforce.pyplyn.model.ThresholdType;

/**
 * Predefined alert meter types, defined on Metered processors ({@link com.salesforce.pyplyn.processor.AbstractMeteredExtractProcessor}
 *   and {@link com.salesforce.pyplyn.processor.AbstractMeteredLoadProcessor})
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */

public class MeterType {

    private ThresholdType alertType;
    private ProcessStatus processStatus;

    public MeterType(ThresholdType alertType, ProcessStatus processStatus) {
        this.alertType = alertType;
        this.processStatus = processStatus;
    }

    public ThresholdType alertType() {
        return this.alertType;
    }

    public ProcessStatus processStatus(){
        return this.processStatus;
    }
}