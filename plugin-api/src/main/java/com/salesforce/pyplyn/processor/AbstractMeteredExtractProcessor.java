/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.processor;

import com.google.inject.Inject;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.status.MeterType;
import com.salesforce.pyplyn.status.SystemStatus;


/**
 * Metered processor base class
 * <p/>
 * Provides helper methods used to meter {@link ExtractProcessor}s and provide runtime information
 *   to an instance of {@link SystemStatus}.
 * <p/>
 * <p/>This class is similar in implementation to {@link AbstractMeteredLoadProcessor}.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public abstract class AbstractMeteredExtractProcessor<T extends Extract> implements ExtractProcessor<T> {
    protected SystemStatus systemStatus;

    /**
     * Override this method and provide a name for the metered class
     *   this name will be used in the AppConfig's alert section to define the required alerting thresholds
     *
     * @return the name used by the implementation of this class
     */
    protected abstract String meterName();

    /**
     * Call this method when the operation has succeeded
     */
    protected void succeeded() {
        systemStatus.meter(meterName(), MeterType.ExtractSuccess).mark();
    }

    /**
     * Call this method when the operation has failed
     */
    protected void failed() {
        systemStatus.meter(meterName(), MeterType.ExtractFailure).mark();
    }

    /**
     * Call this method when the endpoint returns no data
     */
    protected void noData() {
        systemStatus.meter(meterName(), MeterType.ExtractNoDataReturned).mark();
    }

    /**
     * Call this method when attempting to authenticate to the endpoint failed
     */
    protected void authenticationFailure() {
        systemStatus.meter(meterName(), MeterType.AuthenticationFailure).mark();
    }

    /**
     * Allows {@link com.google.inject.Guice} to inject a SystemStatus object
     *
     * @param systemStatus the {@link SystemStatus} singleton used to meter this processor
     */
    @Inject
    public void setSystemStatus(SystemStatus systemStatus) {
        this.systemStatus = systemStatus;
    }
}
