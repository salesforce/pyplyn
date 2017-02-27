/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

/**
 * Test helper code for initializing and shutting down executors
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class ExecutorTestHelper {

    /**
     * Initializes a single thread executor, used to run a process under test
     * @return a {@link ThreadPoolExecutor} as we want to be able to query the number of active threads
     */
    public static ThreadPoolExecutor initSingleThreadExecutor() {
        return (ThreadPoolExecutor)Executors.newFixedThreadPool(1);
    }

    /**
     * Shuts down a running executor and allow for a short grace period before terminating
     * @throws InterruptedException if interrupted white waiting for the executor to shut down
     */
    public static void shutdown(ExecutorService executor) throws InterruptedException {
        if (nonNull(executor) && !executor.isShutdown()) {
            executor.shutdownNow();
            executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        }
    }
}
