package com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test;

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
        Extract[] extracts = new Extract[]{
                new Argus(argusEndpoint, argusExpression, argusName, 1, 2d),
                new Refocus(refocusEndpoint, refocusSubject, refocusAspect, 1, 2d)
        };

        Transform[] transform = new Transform[]{
                new HighestValue(),
                new InfoStatus(),
                new LastDatapoint(),
                new SaveMetricMetadata(),
                new Threshold()
        };

        Load[] load = new Load[]{
                new com.salesforce.pyplyn.duct.etl.load.refocus.Refocus(refocusEndpoint, refocusSubject, refocusAspect,
                        "defaultMessageCode", "defaultMessageBody", null)
        };

        return new Configuration(repeatIntervalMillis, extracts, transform, load, disabled);
    }
}
