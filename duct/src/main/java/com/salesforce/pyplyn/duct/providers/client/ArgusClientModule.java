/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.providers.client;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.salesforce.argus.ArgusClient;

/**
 * Configures the bindings required by {@link ArgusClient}
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ArgusClientModule extends AbstractModule {
    @Override
    protected void configure() {
        // allows injection of the ArgusClient.class
        bind(new TypeLiteral<Class<ArgusClient>>(){}).toInstance(ArgusClient.class);

        // allows injection of the Argus RemoteClient factory
        bind(new TypeLiteral<RemoteClientFactory<ArgusClient>>(){}).in(Scopes.SINGLETON);
    }
}
