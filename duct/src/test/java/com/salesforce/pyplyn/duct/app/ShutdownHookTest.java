/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapLatches;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.ExecutorTestHelper;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationUpdateManager;
import com.salesforce.pyplyn.status.MeterType;


/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class ShutdownHookTest {
    AppBootstrapFixtures fixtures;
    ExecutorService executor;
    private ArgumentCaptor<MeterType> meterTypeArgumentCaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
        executor = ExecutorTestHelper.initSingleThreadExecutor();
        fixtures.shutdownHook().registerExecutor(executor);
        meterTypeArgumentCaptor = ArgumentCaptor.forClass(MeterType.class);
    }

    /**
     * Checks that triggering the ShutdownHook will stop an ongoing ETL cycle
     * <p/> and that RefocusLoadProcessor does not post to Refocus endpoint on Shutdown
     */
    @Test(timeOut = 5_000L)
    public void testAppNotPostingResultsToRefocusAfterShuttingDown() throws Exception {
        try {
            // ARRANGE
            fixtures.enableLatches()
                    .oneArgusToRefocusConfigurationWithRepeatInterval(1_000L)
                    .callRealRefocusLoadProcessor()
                    .initializeFixtures()
                    .returnMockedTransformationResultFromAllExtractProcessors();


            // init app and register executor for shutdown
            ConfigurationUpdateManager manager = fixtures.configurationManager();

            // ACT
            executor.submit(manager);

            // wait until we are ready to load, shutdown and allow extract processor to run
            AppBootstrapLatches.beforeLoadProcessorStarts().await();
            fixtures.shutdownHook().shutdown();
            AppBootstrapLatches.holdOffBeforeLoadProcessorStarts().countDown();

            // wait for shutdown
            AppBootstrapLatches.appHasShutdown().await();

            // ASSERT
            // since the load scheduler was interrupted, expecting no interactions
            verify(fixtures.systemStatus(), times(1)).meter(eq("Refocus"), meterTypeArgumentCaptor.capture());
            assert(meterTypeArgumentCaptor.getValue().processStatus().name().equals("LoadFailure"));

        } finally {
            AppBootstrapLatches.release();
        }
    }

    @Test(timeOut = 5_000L)
    public void testArgusNotQueriedIfShuttingDown() throws Exception {
        try {
            // ARRANGE
            fixtures.enableLatches()
                    .oneArgusToRefocusConfigurationWithRepeatInterval(1_000L)
                    .callRealArgusExtractProcessor()
                    .initializeFixtures();

            // register executor with ShutdownHook
            ConfigurationUpdateManager manager = fixtures.configurationManager();

            // ACT
            executor.submit(manager);

            // wait until we're ready to extract, then shutdown
            AppBootstrapLatches.beforeExtractProcessorStarts().await();
            fixtures.shutdownHook().shutdown();
            AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().countDown();

            // wait for shutdown
            AppBootstrapLatches.appHasShutdown().await();

            // ASSERT
            // expecting no failures or successes to be logged when the app was shutdown
            verify(fixtures.systemStatus(), times(0)).meter(eq("Argus"), any());

        } finally {
            AppBootstrapLatches.release();
        }
    }


    /**
     * RefocusExtractProcessor does not query the remote Argus endpoint if the app is shutting down
     */
    @Test(timeOut = 5_000L)
    public void testRefocusNotQueriedIfShuttingDown() throws Exception {
        try {
            // ARRANGE
            // bootstrap
            fixtures.enableLatches()
                    .oneRefocusToRefocusConfigurationWithRepeatInterval(1_000L)
                    .callRealRefocusExtractProcessor()
                    .initializeFixtures();

            // init app and register executor for shutdown
            ConfigurationUpdateManager manager = fixtures.configurationManager();

            // ACT
            executor.submit(manager);

            // wait until we are ready to start, shutdown and allow extract processor to run
            AppBootstrapLatches.beforeExtractProcessorStarts().await();
            fixtures.shutdownHook().shutdown();
            AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().countDown();

            // wait for shutdown
            AppBootstrapLatches.appHasShutdown().await();

            // ASSERT
            // since the extract was interrupted, expecting the timer surrounding RefocusClient#getSamples(String) to not be executed
            verify(fixtures.systemStatus(), times(0)).timer("Refocus", "get-sample." + AppBootstrapFixtures.MOCK_CONNECTOR_NAME);

        } finally {
            AppBootstrapLatches.release();
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // if an executor was started, shut it down
        ExecutorTestHelper.shutdown(executor);
    }
}