/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.connector;

import com.google.inject.AbstractModule;
import com.salesforce.pyplyn.util.MultibinderFactory;

/**
 * Defines the {@link AppConnector} binding and the default file-based connector configuration provider
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AppConnectorModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AppConnector.class).asEagerSingleton();

        // multibinder for all connector configuration types
        //   this allows extenders of this library to define other connectors using Guice modules
        MultibinderFactory.appConnectors(binder()).addBinding().toProvider(SimpleConnectorProvider.class);
    }
}
