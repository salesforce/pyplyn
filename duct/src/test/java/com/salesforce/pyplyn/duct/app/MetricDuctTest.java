/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.ConfigurationsTestHelper.createThresholdTransforms;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 6.0
 */
public class MetricDuctTest {
    private AppBootstrapFixtures fixtures;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
    }

    @Test
    public void testRun() throws Exception {
        // ARRANGE
        // init transformation results
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<TransformationResult> series1 = Arrays.asList(
                new TransformationResult(now.minusMinutes(2), "metric-ok", 1d, 1d),
                new TransformationResult(now.minusMinutes(1), "metric-ok", 2d, 2d),
                new TransformationResult(now, "metric-ok", 3d, 3d)
        );
        List<TransformationResult> series2 = Arrays.asList(
                new TransformationResult(now.minusMinutes(1), "metric-warn", 100d, 100d),
                new TransformationResult(now, "metric-warn", 100d, 100d)
        );

        // init transforms
        Transform[] transforms = createThresholdTransforms(null);

        // only run once
        fixtures.appConfigMocks()
                .runOnce();

        fixtures.returnTransformationResultFromAllExtractProcessors(Arrays.asList(series1, series2))
                .argusToRefocusConfigurationWithTransforms(transforms)
                .freeze();

        // init app and register executor for shutdown
        MetricDuct app = fixtures.app();

        // ACT
        app.run();

        // ASSERT
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TransformationResult>> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixtures.refocusLoadProcessor()).execute(dataCaptor.capture(), any());

        List<TransformationResult> data = dataCaptor.getValue();
        assertThat(data, hasSize(1));

        TransformationResult result = data.get(0);
        assertThat(result.name(), equalTo("metric-warn"));
        assertThat(result.value(), equalTo(2d));
        assertThat(result.originalValue(), equalTo(100d));
        assertThat(result.metadata().messageCode(), equalTo("100"));
        assertThat(result.metadata().messages(), hasItems("metric-ok=3.00", "metric-warn=100.00"));
        assertThat(result.metadata().messages(), hasItem(containsString("WARN threshold hit by metric-warn")));
        assertThat(result.metadata().messages(), hasItem(containsString("value=100.00 GREATER_THAN 100.00")));
    }
}