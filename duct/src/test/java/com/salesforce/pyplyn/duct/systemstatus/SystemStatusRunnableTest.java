/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.systemstatus;

import com.salesforce.pyplyn.duct.app.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.app.MetricDuct;
import com.salesforce.pyplyn.status.AlertLevel;
import com.salesforce.pyplyn.status.MeterType;
import com.salesforce.pyplyn.status.StatusMessage;
import com.salesforce.pyplyn.status.SystemStatusConsumer;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class SystemStatusRunnableTest {
    AppBootstrapFixtures fixtures;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    /**
     * We can run an ETL flow with a real SystemStatusRunnable,
     *   which publishes meter and metrics alerts
     */
    @Test
    public void testRun() throws Exception {
        // ARRANGE
        // app-config params
        fixtures.appConfigMocks()
                .enableAlerts()
                .runOnce()

                // we don't want the LoadFailure meter to fail in this test
                .checkMeter("RefocusLoadFailureWARN", 999.0)
                .checkMeter("RefocusLoadFailureERR", 999.0);

        // bootstrap
        fixtures.realSystemStatus()
                .oneArgusToRefocusConfiguration()
                .returnMockedTransformationResultFromAllExtractProcessors()
                .simulateLoadProcessingTime(100)
                .freeze();

        // init app and schedule system status thread
        SystemStatusConsumer systemStatusConsumer = fixtures.statusConsumer();
        MetricDuct app = fixtures.app();


        // ACT
        app.run();

        // process system status checks
        fixtures.systemStatus().run();


        // ASSERT
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StatusMessage>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        // check that meters and timers are initialized
        verify(fixtures.systemStatus(), atLeastOnce()).meter("Refocus", MeterType.LoadFailure);
        verify(fixtures.systemStatus(), atLeastOnce()).timer("Refocus", "upsert-samples-bulk." + AppBootstrapFixtures.MOCK_CONNECTOR_NAME);

        // check that system status was reported to the consumers
        verify(systemStatusConsumer, atLeastOnce()).accept(argumentCaptor.capture());
        List<StatusMessage> messages = argumentCaptor.getValue();

        // check expected output
        assertThat("Expecting three messages, status=OK and the timing message", messages, hasSize(3));
        assertThat("The status should be OK", messages.get(0).level(), is(AlertLevel.OK));

        List<String> messageStrings = messages.stream().map(Object::toString).collect(Collectors.toList());
        assertThat("MetricDuct should report timing data",
                messageStrings, hasItem(containsString(MetricDuct.class.getSimpleName())));
        assertThat("AppBootstrapFixtures should report timing data for the Refocus Load processor",
                messageStrings, hasItem(containsString("Refocus.upsert-samples-bulk." + AppBootstrapFixtures.MOCK_CONNECTOR_NAME)));
    }
}