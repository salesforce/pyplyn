/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.processor;

import com.google.inject.Inject;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.status.*;


/**
 * Metered processor base class
 * <p/>
 * <p/>Provides helper methods used to meter {@link LoadProcessor}s and provide runtime information
 *   to an instance of {@link SystemStatus}.
 * <p/>
 * <p/>This class is similar in implementation to {@link AbstractMeteredExtractProcessor}.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public abstract class AbstractMeteredLoadProcessor<T extends Load> implements LoadProcessor<T> {
    protected SystemStatus systemStatus;

    /**
     * Override this method and provide a name for the metered class
     * this name will be used in the AppConfig's alert section to define the required alerting thresholds
     *
     * @return the name used by the implementation of this class
     */
    protected abstract String meterName();

    /**
     * Call this method when the operation has succeeded
     */
    protected void succeeded() {
        systemStatus.meter(meterName(), new MeterType(null, ProcessStatus.LoadSuccess)).mark();
    }

    /**
     * Call this method when the operation has failed
     */
    protected void failed() {
        systemStatus.meter(meterName(), new MeterType(null, ProcessStatus.LoadFailure)).mark();
    }

    /**
     * Call this method when attempting to authenticate the endpoint failed
     */
    protected void authenticationFailure() {
        systemStatus.meter(meterName(), new MeterType(null, ProcessStatus.AuthenticationFailure)).mark();
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
