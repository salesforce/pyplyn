/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test;

import com.salesforce.pyplyn.duct.app.MetricDuct;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Latches used to simulate race conditions within the app, during testing
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class AppBootstrapLatches {
    private static CountDownLatch isProcessingExtractDatasources;
    private static CountDownLatch beforeExtractProcessorStarts;
    private static CountDownLatch beforeLoadProcessorStarts;
    private static CountDownLatch appHasShutdown;

    /**
     * Initializes all known latches to default values; this is called automatically in {@link AppBootstrapFixtures#freeze()}
     * <p/>NOTE: it is best to not call this manually, as it might yield unexpected results (since it's not synchronized)
     */
    static void init() {
        isProcessingExtractDatasources = new CountDownLatch(1);
        beforeExtractProcessorStarts = new CountDownLatch(1);
        beforeLoadProcessorStarts = new CountDownLatch(1);
        appHasShutdown = new CountDownLatch(1);
    }

    /**
     * @return latch used to signal we are processing {@link com.salesforce.pyplyn.model.Extract} datasources
     */
    public static CountDownLatch isProcessingExtractDatasources() {
        return isProcessingExtractDatasources;
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
     * @return latch that signals when {@link MetricDuct#run()} has completed
     */
    public static CountDownLatch appHasShutdown() {
        return appHasShutdown;
    }
}
