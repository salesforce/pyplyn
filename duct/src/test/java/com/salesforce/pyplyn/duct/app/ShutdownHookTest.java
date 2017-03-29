/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapLatches;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.ExecutorTestHelper;
import com.salesforce.pyplyn.status.MeterType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class ShutdownHookTest {
    AppBootstrapFixtures fixtures;
    ExecutorService executor;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
        executor = ExecutorTestHelper.initSingleThreadExecutor();
    }

    /**
     * Checks that triggering the ShutdownHook will stop an ongoing ETL cycle
     * <p/> and that RefocusLoadProcessor does not post to Refocus endpoint on Shutdown
     */
    @Test
    public void testAppNotPostingResultsToRefocusAfterShuttingDown() throws Exception {
        // ARRANGE
        // bootstrap
        fixtures.realShutdownHook()
                .enableLatches()
                .oneArgusToRefocusConfiguration()
                .returnMockedTransformationResultFromAllExtractProcessors()
                .simulateRefocusLoadProcessingDelay(100)
                .freeze();

        // init app and register executor for shutdown
        ShutdownHook shutdownHook = registerExecutorWithShutdownHook();
        MetricDuct app = fixtures.app();

        // ACT
        executor.submit(app);

        // wait until we are ready to load, shutdown and allow extract processor to run
        AppBootstrapLatches.beforeLoadProcessorStarts().await();
        shutdownHook.shutdown();
        AppBootstrapLatches.holdOffBeforeLoadProcessorStarts().countDown();

        // wait for shutdown
        AppBootstrapLatches.appHasShutdown().await();

        // ASSERT
        // since the load was interrupted, expecting the RefocusLoadProcessor to log a failure
        verify(fixtures.systemStatus()).meter("Refocus", MeterType.LoadFailure);
    }

    @Test(timeOut = 5000L)
    public void testArgusNotQueriedIfShuttingDown() throws Exception {
        // ARRANGE
        fixtures.realShutdownHook()
                .enableLatches()
                .oneArgusToRefocusConfiguration()
                .callRealArgusExtractProcessor()
                .freeze();

        // register executor with ShutdownHook
        ShutdownHook shutdownHook = registerExecutorWithShutdownHook();
        MetricDuct app = fixtures.app();

        // ACT

        // start app
        executor.submit(app);

        // wait until we're ready to extract, then shutdown
        AppBootstrapLatches.beforeExtractProcessorStarts().await();
        shutdownHook.shutdown();
        AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().countDown();

        // wait for shutdown
        AppBootstrapLatches.appHasShutdown().await();

        // ASSERT
        // expecting no failures or successes to be logged when the app was shutdown
        verify(fixtures.systemStatus(), times(0)).meter("Argus", MeterType.ExtractFailure);
        verify(fixtures.systemStatus(), times(0)).meter("Argus", MeterType.ExtractSuccess);
    }


    /**
     * RefocusExtractProcessor does not query the remote Argus endpoint if the app is shutting down
     */
    @Test(timeOut = 5000L)
    public void testRefocusNotQueriedIfShuttingDown() throws Exception {
        // ARRANGE
        // bootstrap
        fixtures.realShutdownHook()
                .enableLatches()
                .oneRefocusToRefocusConfiguration()
                .callRealRefocusExtractProcessor()
                .freeze();

        // init app and register executor for shutdown
        ShutdownHook shutdownHook = registerExecutorWithShutdownHook();
        MetricDuct app = fixtures.app();

        // ACT
        executor.submit(app);

        // wait until we are ready to start, shutdown and allow extract processor to run
        AppBootstrapLatches.beforeExtractProcessorStarts().await();
        shutdownHook.shutdown();
        AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().countDown();

        // wait for shutdown
        AppBootstrapLatches.appHasShutdown().await();

        // ASSERT
        // since the extract was interrupted, expecting the timer surrounding RefocusClient#getSamples(String) to not be executed
        verify(fixtures.systemStatus(), times(0)).timer("Refocus", "get-sample." + AppBootstrapFixtures.MOCK_CONNECTOR_NAME);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // if an executor was started, shut it down
        ExecutorTestHelper.shutdown(executor);
    }

    /**
     * Registers the test executor for shutdown (similar to what DuctExecutorWrapper does
     */
    private ShutdownHook registerExecutorWithShutdownHook() {
        fixtures.shutdownHook().registerExecutor(executor);
        return fixtures.shutdownHook();
    }
}