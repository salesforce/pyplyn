/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import java.util.concurrent.*;

/**
 * Wraps a scheduled executor's functionality, performing the following functions:
 * <p/>- periodically runs a status process
 * <p/>- executes a task and awaits for the its termination
 * <p/>
 * <p/>Using this class makes sense when having a long-running task to execute and that also require a monitoring thread to run
 */
public class DuctExecutorWrapper {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);

    /**
     * Schedules a process that runs periodically and runs any logic pertaining to determining the application's status
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit the time unit of the initialDelay and period parameters
     */
    public void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        executor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    /**
     * Immediately executes a process
     */
    public void execute(Runnable command) {
        executor.execute(command);
    }

    /**
     * Registers the executor in the passed shutdown hook
     *   to allow it to be shut down when required
     */
    public void registerForShutdown(ShutdownHook shutdownHook) {
        shutdownHook.registerExecutor(executor);
    }
}
