/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationWrapper;
import com.salesforce.pyplyn.model.TransformationResult;

import java.util.List;

/**
 * Represents an execution stage ({@link com.salesforce.pyplyn.model.Extract}[], {@link com.salesforce.pyplyn.model.Transform}[],
 *   or {@link com.salesforce.pyplyn.model.Load}[])
 * <p/>
 * <p/>Used in {@link MetricDuct} to pass data between the ETL stages for the current {@link ConfigurationWrapper} object
 * <p/>
 * <p/> Immutable object.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
class Stage {
    private final List<List<TransformationResult>> data;
    private final ConfigurationWrapper configuration;

    /**
     * Constructs a new stage from the passed results and configuration
     */
    Stage(List<List<TransformationResult>> data, ConfigurationWrapper configuration) {
        this.data = data;
        this.configuration = configuration;
    }

    /**
     * @return processed data
     */
    public List<List<TransformationResult>> data() {
        return data;
    }

    /**
     * @return the corresponding {@link ConfigurationWrapper}
     */
    public ConfigurationWrapper wrapper() {
        return configuration;
    }
}
