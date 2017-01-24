/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.load.refocus;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.salesforce.pyplyn.util.MultibinderFactory;

/**
 * Defines the {@link Refocus} load bindings
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class RefocusLoadModule extends AbstractModule {
    @Override
    protected void configure() {
        // register Refocus load model for deserialization
        MultibinderFactory.loadDestinations(binder()).addBinding().toInstance(Refocus.class);

        // register Refocus load processor
        MultibinderFactory.loadProcessors(binder()).addBinding().to(RefocusLoadProcessor.class).in(Scopes.SINGLETON);
    }
}
