/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * App shutdown hook
 * <p/>
 * <p/>Annotated as Singleton as there should only be one instance of this class in operation.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class ShutdownHook extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);

    /**
     * Used to signal the program is shutting down and all listeners should stop doing expensive operations
     *   and gracefully stop their executions
     */
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Registered executors that should be sent the shutdown signal
     */
    private final Set<ExecutorService> executors = new HashSet<>();

    /**
     * Registered threads that should be run on shutdown
     */
    private final Set<Runnable> operations = new HashSet<>();

    /**
     * Sets the shutdown flag to true, allowing processes that are aware of this hook to stop gracefully
     */
    @Override
    public void run() {
        logger.warn("Shutting down...");
        isShutdown.set(true);
    }

    /**
     * Executes all shutdown operations and shuts down all executors
     */
    public void shutdown() {
        // shutdown all executors
        executors.forEach(ExecutorService::shutdown);

        // run shutdown ops
        operations.forEach(Runnable::run);
    }

    /**
     * Registers an executor, which will be shutdown when this thread runs
     *
     * @param executor
     */
    public void registerExecutor(ExecutorService executor) {
        executors.add(executor);
    }

    /**
     * Registers a thread to be run when the system is shutting down
     * <p/>
     * Caution: these should be small operations that signal a shutdown; avoid doing large amounts of work,
     *  as they might delay the app's shut down; also, there is no guarantee that these will actually finish executing
     */
    public void registerOperation(Runnable runnable) {
        operations.add(runnable);
    }

    /**
     * Allows running threads to check if the app is shutting down
     * <p/>This method is used to stop any new work from being undertaken and
     *   allowing the process to stop gracefully.
     *
     * @return true if app is shutting down
     */
    public boolean isShutdown() {
        return isShutdown.get();
    }

    /**
     * This implementation always responds with app is running
     * <p/>
     * <p/>It is a default implementation to avoid having to check for nulls in classes that expect a ShutdownHook object
     */
    public static class NoOp extends ShutdownHook {
        @Override
        public boolean isShutdown() {
            return false;
        }
    }
}
