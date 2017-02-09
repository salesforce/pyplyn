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

import static com.salesforce.pyplyn.duct.app.AppBootstrapFixtures.LOAD_PROCESSOR_TIMER_NAME;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since ${project.version}
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
        // config params
        fixtures.appConfigMocks()
                .enableAlerts()
                .runOnce()

                // we don't want the LoadSuccess meter to fail, since the rate would be too low for only one invocation
                .checkMeter(LOAD_PROCESSOR_TIMER_NAME + "LoadSuccessWARN", -1.0)
                .checkMeter(LOAD_PROCESSOR_TIMER_NAME + "LoadSuccessERR", -1.0);

        // bootstrap
        fixtures
                .realSystemStatus()
                .oneConfiguration()
                .allProcessorsReturnAnExtractResult()
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
        verify(fixtures.systemStatus(), atLeastOnce()).meter(LOAD_PROCESSOR_TIMER_NAME, MeterType.LoadSuccess);
        verify(fixtures.systemStatus(), atLeastOnce()).timer(LOAD_PROCESSOR_TIMER_NAME, LOAD_PROCESSOR_TIMER_NAME);

        // check that system status was reported to the consumers
        verify(systemStatusConsumer, atLeastOnce()).accept(argumentCaptor.capture());
        List<StatusMessage> messages = argumentCaptor.getValue();

        // check expected output
        assertThat("Expecting three messages, status=OK and the timing message", messages, hasSize(3));
        assertThat("The status should be OK", messages.get(0).level(), is(AlertLevel.OK));

        List<String> messageStrings = messages.stream().map(Object::toString).collect(Collectors.toList());
        assertThat("MetricDuct should report timing data",
                messageStrings, hasItem(containsString(MetricDuct.class.getSimpleName())));
        assertThat("AppBootstrapFixtures should report timing data for its mocked Load processor",
                messageStrings, hasItem(containsString(LOAD_PROCESSOR_TIMER_NAME)));
    }
}