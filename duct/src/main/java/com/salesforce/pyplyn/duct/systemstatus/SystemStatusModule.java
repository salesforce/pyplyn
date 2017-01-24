/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.systemstatus;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.salesforce.pyplyn.status.SystemStatus;
import com.salesforce.pyplyn.util.MultibinderFactory;

/**
 * Initializes the {@link SystemStatus} thresholds and the {@link SystemStatusRunnable} implementation
 */
public class SystemStatusModule extends AbstractModule {
    @Override
    protected void configure() {
        // binds the System Status process to run
        bind(SystemStatus.class).to(SystemStatusRunnable.class).in(Scopes.SINGLETON);

        // multibinder to use for adding additional consumers
        MultibinderFactory.statusConsumers(binder()).addBinding().to(ConsoleOutputConsumer.class).asEagerSingleton();
    }
}
