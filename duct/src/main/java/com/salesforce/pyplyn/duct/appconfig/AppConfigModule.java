/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.appconfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Defines bindings for the {@link AppConfigProvider} and the config file path
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AppConfigModule extends AbstractModule {
    final String configFile;

    public AppConfigModule(String config) {
        this.configFile = config;
    }

    @Override
    protected void configure() {
        // app configuration
        bindConstant().annotatedWith(Names.named("config")).to(configFile);
        bind(AppConfig.class).toProvider(AppConfigProvider.class).asEagerSingleton();
    }
}
