/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.appconfig;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.salesforce.pyplyn.util.SerializationHelper;

/**
 * Provides an app config object
 * <p/>
 * <p/>Deserializes the specified configuration file and stores the generated config object
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AppConfigProvider implements Provider<AppConfig> {
    private final AppConfig appConfig;

    @Inject
    public AppConfigProvider(@Named("config") String configFile, ObjectMapper mapper) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        // TODO: simplify this / move to modules
        appConfig = mapper.readValue(SerializationHelper.loadResourceInsecure(configFile), AppConfig.class);
    }

    @Override
    public AppConfig get() {
        return appConfig;
    }
}
