/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.load.refocus;

import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.app.MetricDuct;
import com.salesforce.pyplyn.status.MeterType;
import org.testng.annotations.*;

import static org.mockito.Mockito.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class RefocusLoadProcessorTest {
    AppBootstrapFixtures fixtures;


    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testProcessShouldFailWhenClientIsMissing() throws Exception {
        // ARRANGE
        // bootstrap
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.oneArgusToRefocusConfiguration()
                .returnMockedTransformationResultFromAllExtractProcessors()
                .callRealRefocusLoadProcessor()
                .refocusClientCanNotAuth()
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        // since we had no real client, expecting ArgusExtractProcessor to have logged a failure
        verify(fixtures.systemStatus(), times(1)).meter("Refocus", MeterType.LoadFailure);
    }
}