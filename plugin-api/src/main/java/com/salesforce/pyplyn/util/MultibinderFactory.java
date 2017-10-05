/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import java.util.List;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.salesforce.pyplyn.client.AbstractRemoteClient;
import com.salesforce.pyplyn.configuration.EndpointConnector;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.processor.ExtractProcessor;
import com.salesforce.pyplyn.processor.LoadProcessor;
import com.salesforce.pyplyn.status.SystemStatusConsumer;

/**
 * Formalizes the contract for any extending plugins that (based on {@link Guice}/{@link Multibinder}
 *
 * TODO: Refactor using {@link ProvidesIntoSet}
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public final class MultibinderFactory {

    /**
     * Utilities classes should not be instantiated
     */
    private MultibinderFactory() { }

    /**
     * Used to bind lists of {@link EndpointConnector} implementations
     *   this is a Set<List<Connector>> since plugins might need to define new connectors that are
     *   implemented differently
     */
    public static Multibinder<List<EndpointConnector>> appConnectors(Binder binder) {
         return Multibinder.newSetBinder(binder, new TypeLiteral<List<EndpointConnector>>() {});
    }

    /**
     * Used to bind {@link Extract} data source models
     */
    public static Multibinder<Class<? extends Extract>> extractDatasources(Binder binder) {
        return Multibinder.newSetBinder(binder, new TypeLiteral<Class<? extends Extract>>() {});
    }

    /**
     * Used to bind {@link ExtractProcessor}s for that can process corresponding {@link Extract} models
     */
    public static Multibinder<ExtractProcessor<? extends Extract>> extractProcessors(Binder binder) {
        return Multibinder.newSetBinder(binder, new TypeLiteral<ExtractProcessor<? extends Extract>>(){});
    }

    /**
     * Used to bind {@link Transform} functions
     */
    public static Multibinder<Class<? extends Transform>> transformFunctions(Binder binder) {
        return Multibinder.newSetBinder(binder, new TypeLiteral<Class<? extends Transform>>() {});
    }

    /**
     * Used to bind {@link Load} destination models
     */
    public static Multibinder<Class<? extends Load>> loadDestinations(Binder binder) {
        return Multibinder.newSetBinder(binder, new TypeLiteral<Class<? extends Load>>() {});
    }

    /**
     * Used to bind {@link LoadProcessor}s for that can process corresponding {@link Load} models
     */
    public static Multibinder<LoadProcessor<? extends Load>> loadProcessors(Binder binder) {
        return Multibinder.newSetBinder(binder, new TypeLiteral<LoadProcessor<? extends Load>>() {});
    }

    /**
     * Used to bind {@link SystemStatusConsumer}s that accept {@link com.salesforce.pyplyn.status.StatusMessage}s and
     *   report them to external monitoring systems
     */
    public static Multibinder<SystemStatusConsumer> statusConsumers(Binder binder) {
        return Multibinder.newSetBinder(binder, SystemStatusConsumer.class);
    }


    public static <S> Multibinder<AbstractRemoteClient<S>> apiClients(Binder binder) {
        return Multibinder.newSetBinder(binder, new TypeLiteral<AbstractRemoteClient<S>>(){});
    }
}
