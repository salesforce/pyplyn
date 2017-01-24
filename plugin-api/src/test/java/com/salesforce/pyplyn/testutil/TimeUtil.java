/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.testutil;

/**
 * Wait utility
 *   used in tests to wait for specified durations
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class TimeUtil {
    /**
     * Functional-style factory method
     */

    public static TimeUtil await() {
        return new TimeUtil();
    }

    /**
     * Sleep for at least this many milliseconds
     * @param delay
     */
    public void atLeastMs(long delay) {
        // make note of current time and compute time at which we should stop waiting
        long sleepUntil = System.currentTimeMillis() + delay;

        // if the target time has not been reached
        while (System.currentTimeMillis() < sleepUntil) {
            // sleep for remaining duration
            sleepFor(Math.max(0, sleepUntil - System.currentTimeMillis()));
        }
    }

    private void sleepFor(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // nothing to do, we don't care about interrupted exceptions
        }
    }
}
