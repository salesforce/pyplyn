/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import com.google.inject.Inject;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.BootstrapException;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;

import java.io.IOException;
import java.util.Set;

/**
 * Reads all known configurations from disk
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConfigurationLoader {
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
            try {
                // read all files from config path
                return intake.parseAll(intake.getAllConfigurationsFromDisk(appConfig.global().configurationsPath()));

            } catch (IOException e) {
                // being unable to read the configuration files is an unrecoverable exception
                //   we will rethrow this as a BootstrapException, since the process
                //   would be unable to load any configurations to process
                throw new BootstrapException("Could not read configuration", e);
            }

        } finally {
            // throw an exception if any of the configuration files could not be read successfully
            intake.throwRuntimeExceptionOnErrors();
        }
    }
}
