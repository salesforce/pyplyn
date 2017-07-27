/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.connector;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.util.MultibinderFactory;

/**
 * Defines the {@link AppConnectors} binding and the default file-based connector configuration provider
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AppConnectorModule extends AbstractModule {
    // TODO: configure the provider directly in the module, using self-inject for AppConfig
    @Inject
    AppConfig appConfig;

    @Override
    protected void configure() {
        bind(AppConnectors.class).asEagerSingleton();

        // multibinder for all connector configuration types
        //   this allows extenders of this library to define other connectors using Guice modules
        MultibinderFactory.appConnectors(binder()).addBinding().toProvider(SimpleConnectorProvider.class);
    }
}
