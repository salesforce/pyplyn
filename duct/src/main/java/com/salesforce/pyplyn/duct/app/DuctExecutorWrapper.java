/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wraps a scheduled executor's functionality, performing the following functions:
 * <p/>- periodically runs a status process
 * <p/>- executes a task and awaits for the its termination
 * <p/>
 * <p/>Using this class makes sense when having a long-running task to execute and that also require a monitoring thread to run
 */
public class DuctExecutorWrapper {
    private static final Logger logger = LoggerFactory.getLogger(DuctExecutorWrapper.class);
    private static final long INITIAL_DELAY_SECONDS = 5;
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
     * Executes the command with an initial 5 second delay
     *   and awaits for all tasks to complete
     * <p/>
     * <p/>The delay is to give the configuration provider a chance to parse all the defined configurations
     *
     * @param command the task to schedule
     */
    public void schedule(Runnable command) {
        // schedule task (usually a long-running process)
        logger.info("Starting main execution in {}s", INITIAL_DELAY_SECONDS);
        executor.schedule(command, INITIAL_DELAY_SECONDS, TimeUnit.SECONDS);

        try {
            // await for all scheduled tasks to complete
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        } catch (InterruptedException e) {
            logger.warn("Interrupted", e);
        }
    }

    /**
     * Registers the executor in the passed shutdown hook
     *   to allow it to be shut down when required
     */
    public void registerForShutdown(ShutdownHook shutdownHook) {
        shutdownHook.registerExecutor(executor);
    }
}
