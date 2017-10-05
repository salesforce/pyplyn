/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;

/**
 * Reads all known configurations from disk
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConfigurationLoader {
    protected static final Logger logger = LoggerFactory.getLogger(ConfigurationLoader.class);
    private final AppConfig appConfig;
    private final ConfigurationIntake intake;


    /**
     * Constructs the provider
     *
     * @param appConfig {@link AppConfig} object
     * @param intake Configuration intake that can read all configurations files at the specified path in the config object
     */
    @Inject
    public ConfigurationLoader(AppConfig appConfig, ConfigurationIntake intake) {
        this.appConfig = appConfig;
        this.intake = intake;
    }

    /**
     * @return a list containing all configurations defined in all files
     */
    public Set<Configuration> load() {
        // read all configurations
        try {
            // read all files from config path
            logger.info("Reading configurations from {}", appConfig.global().configurationsPath());
            return intake.parseAll(appConfig.global().configurationsPath());

        } finally {
            // throw an exception if any of the configuration files could not be read successfully
            intake.throwRuntimeExceptionOnErrors();
        }
    }
}
