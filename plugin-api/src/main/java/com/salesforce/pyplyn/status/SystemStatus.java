/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

/**
 * System status interface
 * <p/>
 * <p/>Extends Runnable, since the implementing class will be passed to a {@link java.util.concurrent.ScheduledExecutorService}
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface SystemStatus extends Runnable {

    /**
     * Implement this method and return a {@link Meter} object used to handle the passed
     *   combination of <b>name</b> and <b>type</b> parameters
     */
    Meter meter(String name, MeterType type);

    /**
     * Implement this method and return a {@link Timer} object used to measure the performance of your app
     */
    Timer timer(String name, String method);
}
