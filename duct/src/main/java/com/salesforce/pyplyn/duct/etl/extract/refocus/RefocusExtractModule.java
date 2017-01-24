/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.refocus;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.salesforce.pyplyn.util.MultibinderFactory;

/**
 * Defines the {@link Refocus} extract bindings
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class RefocusExtractModule extends AbstractModule {
    @Override
    protected void configure() {
        // register Refocus extract model for deserialization
        MultibinderFactory.extractDatasources(binder()).addBinding().toInstance(Refocus.class);

        // register Refocus extract processor
        MultibinderFactory.extractProcessors(binder()).addBinding().to(RefocusExtractProcessor.class).in(Scopes.SINGLETON);
    }
}
