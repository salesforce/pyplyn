/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.etl.extract.argus.Argus;
import com.salesforce.pyplyn.duct.etl.extract.refocus.Refocus;
import com.salesforce.pyplyn.duct.etl.transform.highestvalue.HighestValue;
import com.salesforce.pyplyn.duct.etl.transform.infostatus.InfoStatus;
import com.salesforce.pyplyn.duct.etl.transform.lastdatapoint.LastDatapoint;
import com.salesforce.pyplyn.duct.etl.transform.savemetricmetadata.SaveMetricMetadata;
import com.salesforce.pyplyn.duct.etl.transform.threshold.Threshold;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class DuctConfigurationTest {
    @Test
    public void testAllKnownETLObjects() throws Exception {
        // ARRANGE
        Configuration configuration1 = createFullConfiguration(100L, false);
        Configuration configuration2 = createFullConfiguration(200L, false);

        // ASSERT
        assertThat("Configurations should be equal based on their E/T/L parameters, but not other params", configuration1, equalTo(configuration2));
    }


    @Test
    public void testConfigurationWrapperDelegatesEquality() throws Exception {
        // ARRANGE
        ConfigurationWrapper configuration1 =
                new ConfigurationWrapper(createFullConfiguration(100L, false), null);
        ConfigurationWrapper configuration2 =
                new ConfigurationWrapper(createFullConfiguration(200L, false), null);

        // ASSERT
        assertThat("Configuration wrappers should delegate equality to their Configuration objects", configuration1, equalTo(configuration2));
    }

    /**
     * Creates a full {@link Configuration} object and allows specifying the repeat interval and disabled status
     */
    public static Configuration createFullConfiguration(long repeatIntervalMillis, boolean disabled) {
        Extract[] extracts = new Extract[]{
                new Argus("endpoint", "expression", "name", 1, 2d),
                new Refocus("endpoint", "subject", "aspect", 1, 2d)
        };

        Transform[] transform = new Transform[]{
            new HighestValue(),
            new InfoStatus(),
            new LastDatapoint(),
            new SaveMetricMetadata(),
            new Threshold()
        };

        Load[] load = new Load[]{
            new com.salesforce.pyplyn.duct.etl.load.refocus.Refocus("endpoint", "subject", "aspect",
                    "defaultMessageCode", "defaultMessageBody", null)
        };

        return new Configuration(repeatIntervalMillis, extracts, transform, load, disabled);
    }
}
