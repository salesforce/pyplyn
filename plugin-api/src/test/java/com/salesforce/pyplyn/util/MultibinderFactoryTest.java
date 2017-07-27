/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.ConnectorInterface;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.processor.ExtractProcessor;
import com.salesforce.pyplyn.processor.LoadProcessor;
import com.salesforce.pyplyn.status.SystemStatusConsumer;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class MultibinderFactoryTest {
    /**
     * This test provides little value other than asserting that all methods return a valid Multibinder
     */
    @Test
    public void testHelperMethodsReturnNonNullSets() throws Exception {
        // ARRANGE
        Module module = new Module();

        // ACT
        Guice.createInjector(module);

        // ASSERT
        assertThat(module.appConnectors, not(nullValue()));
        assertThat(module.extractDatasources, not(nullValue()));
        assertThat(module.extractProcessors, not(nullValue()));
        assertThat(module.loadDestinations, not(nullValue()));
        assertThat(module.loadProcessors, not(nullValue()));
        assertThat(module.statusConsumers, not(nullValue()));
        assertThat(module.transformFunctions, not(nullValue()));
    }


    /**
     * Module implementation that makes use of Guice's Binder to create all expected Multibinders
     */
    private static class Module extends AbstractModule {
        Multibinder<List<ConnectorInterface>> appConnectors;
        Multibinder<Class<? extends Extract>> extractDatasources;
        Multibinder<ExtractProcessor<? extends Extract>> extractProcessors;
        Multibinder<Class<? extends Load>> loadDestinations;
        Multibinder<LoadProcessor<? extends Load>> loadProcessors;
        Multibinder<SystemStatusConsumer> statusConsumers;
        Multibinder<Class<? extends Transform>> transformFunctions;

        @Override
        protected void configure() {
            appConnectors = MultibinderFactory.appConnectors(binder());
            extractDatasources = MultibinderFactory.extractDatasources(binder());
            extractProcessors = MultibinderFactory.extractProcessors(binder());
            loadDestinations = MultibinderFactory.loadDestinations(binder());
            loadProcessors = MultibinderFactory.loadProcessors(binder());
            statusConsumers = MultibinderFactory.statusConsumers(binder());
            transformFunctions = MultibinderFactory.transformFunctions(binder());
        }
    }
}