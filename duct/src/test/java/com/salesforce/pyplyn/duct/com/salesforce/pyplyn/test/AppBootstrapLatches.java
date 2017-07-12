/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Latches used to simulate race conditions within the app, during testing
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class AppBootstrapLatches {
    // required to synchronize concurrent access to the latches from multiple tests
    private static ReentrantLock LOCK = new ReentrantLock();

    private static CountDownLatch beforeExtractProcessorStarts;
    private static CountDownLatch beforeLoadProcessorStarts;
    private static CountDownLatch appHasShutdown;

    private static CountDownLatch holdOffBeforeExtractProcessorStarts;
    private static CountDownLatch holdOffUntilExtractProcessorFinishes;

    private static CountDownLatch holdOffBeforeLoadProcessorStarts;
    private static CountDownLatch holdOffUntilLoadProcessorFinishes;


    /**
     * Initializes all known latches to default values; this is called automatically in {@link AppBootstrapFixtures#initializeFixtures()}
     * <p/>NOTE: it is best to not call this manually, as it might yield unexpected results (since it's not synchronized)
     */
    static void init(boolean withLatches) {
        // if latches are not enabled, set to zero so all latches are ignored
        int initialCount = withLatches?1:0;

        // lock if using latches
        if (initialCount > 0) {
            LOCK.lock();
        }

        beforeExtractProcessorStarts = new CountDownLatch(initialCount);
        beforeLoadProcessorStarts = new CountDownLatch(initialCount);
        appHasShutdown = new CountDownLatch(initialCount);

        holdOffBeforeExtractProcessorStarts = new CountDownLatch(initialCount);
        holdOffUntilExtractProcessorFinishes= new CountDownLatch(initialCount);

        holdOffBeforeLoadProcessorStarts = new CountDownLatch(initialCount);
        holdOffUntilLoadProcessorFinishes = new CountDownLatch(initialCount);
    }

    /**
     * Release latch lock
     */
    public static void release() {
        LOCK.unlock();
    }

    /**
     * @return latch used to signal we are about to start {@link com.salesforce.pyplyn.processor.ExtractProcessor#process(List)}
     */
    public static CountDownLatch beforeExtractProcessorStarts() {
        return beforeExtractProcessorStarts;
    }

    /**
     * @return latch used to signal we are about to start {@link com.salesforce.pyplyn.processor.LoadProcessor#process(List, List)}
     */
    public static CountDownLatch beforeLoadProcessorStarts() {
        return beforeLoadProcessorStarts;
    }

    /**
     * @return latch that signals when {@link com.salesforce.pyplyn.duct.etl.configuration.ConfigurationUpdateManager#run()} has completed
     */
    public static CountDownLatch appHasShutdown() {
        return appHasShutdown;
    }

    /**
     * @return latch used to block any processing in {@link com.salesforce.pyplyn.processor.ExtractProcessor}
     */
    public static CountDownLatch holdOffBeforeExtractProcessorStarts() {
        return holdOffBeforeExtractProcessorStarts;
    }

    /**
     * @return latch used to sync until the {@link com.salesforce.pyplyn.processor.ExtractProcessor} has completed
     */
    public static CountDownLatch holdOffUntilExtractProcessorFinishes() {
        return holdOffUntilExtractProcessorFinishes;
    }

    /**
     * @return latch used to block any processing in {@link com.salesforce.pyplyn.processor.LoadProcessor}
     */
    public static CountDownLatch holdOffBeforeLoadProcessorStarts() {
        return holdOffBeforeLoadProcessorStarts;
    }

    /**
     * @return latch used to sync until the {@link com.salesforce.pyplyn.processor.LoadProcessor} has completed
     */
    public static CountDownLatch holdOffUntilLoadProcessorFinishes() {
        return holdOffUntilLoadProcessorFinishes;
    }
}
