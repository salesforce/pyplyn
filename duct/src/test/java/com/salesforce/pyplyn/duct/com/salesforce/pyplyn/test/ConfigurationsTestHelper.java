/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test;

import static com.salesforce.pyplyn.model.ThresholdType.GREATER_THAN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;

import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.configuration.ImmutableConfiguration;
import com.salesforce.pyplyn.duct.etl.extract.argus.ImmutableArgus;
import com.salesforce.pyplyn.duct.etl.extract.refocus.ImmutableRefocus;
import com.salesforce.pyplyn.duct.etl.transform.standard.*;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;

/**
 * Test helper that creates a full {@link com.salesforce.pyplyn.configuration.Configuration} object
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class ConfigurationsTestHelper {
    /**
     * Creates a full {@link Configuration} object and allows specifying the repeat interval and disabled status
     */
    public static Configuration createFullConfiguration(long repeatIntervalMillis, boolean disabled) {
        return createCustomConfiguration("endpoint", "endpoint",
                "expression", "name",
                "subject", "aspect",
                repeatIntervalMillis, disabled);
    }

    /**
     * Create a customized {@link Configuration} object and allows control over argus/refocus parameters
     */
    public static Configuration createCustomConfiguration(String argusEndpoint, String refocusEndpoint,
                                                          String argusExpression, String argusName,
                                                          String refocusSubject, String refocusAspect,
                                                          long repeatIntervalMillis, boolean disabled) {
        List<Extract> extracts = asList(
                ImmutableArgus.of(argusEndpoint, argusExpression, argusName, 1, 2d),
                ImmutableRefocus.builder().endpoint(refocusEndpoint).subject(refocusSubject).aspect(refocusAspect).cacheMillis(1).defaultValue(2d).build()
        );

        List<Transform> transform = createThresholdTransforms("metric");

        List<Load> load = singletonList(
                com.salesforce.pyplyn.duct.etl.load.refocus.ImmutableRefocus.of(refocusEndpoint, refocusSubject, refocusAspect,
                        "defaultMessageCode", "defaultMessageBody", emptyList())
        );

        return ImmutableConfiguration.of(repeatIntervalMillis, extracts, transform, load, disabled);
    }

    /**
     * Creates an array of {@link Transform}'s, based on {@link Threshold}
     */
    public static List<Transform> createThresholdTransforms(String metricName) {
        LastDatapoint lastDatapoint = ImmutableLastDatapoint.builder().build();
        SaveMetricMetadata saveMetricMetadata = ImmutableSaveMetricMetadata.builder().build();
        Threshold threshold = ImmutableThreshold.of(metricName, 1000d, 100d, 10d, GREATER_THAN);
        InfoStatus infoStatus = ImmutableInfoStatus.builder().build();
        HighestValue highestValue = ImmutableHighestValue.of(HighestValue.Display.ORIGINAL_VALUE, HighestValue.Display.ORIGINAL_TIMESTAMP);
        return asList(lastDatapoint, saveMetricMetadata, threshold, infoStatus, highestValue);
    }
}
