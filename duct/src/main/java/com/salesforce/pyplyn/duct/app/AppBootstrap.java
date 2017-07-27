/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.appconfig.AppConfigModule;
import com.salesforce.pyplyn.duct.cluster.ClusterModule;
import com.salesforce.pyplyn.duct.connector.AppConnectorModule;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationModule;
import com.salesforce.pyplyn.duct.etl.extract.argus.Argus;
import com.salesforce.pyplyn.duct.etl.extract.argus.ArgusExtractProcessor;
import com.salesforce.pyplyn.duct.etl.extract.refocus.Refocus;
import com.salesforce.pyplyn.duct.etl.extract.refocus.RefocusExtractProcessor;
import com.salesforce.pyplyn.duct.etl.load.refocus.RefocusLoadProcessor;
import com.salesforce.pyplyn.duct.etl.transform.standard.*;
import com.salesforce.pyplyn.duct.providers.client.ArgusClientModule;
import com.salesforce.pyplyn.duct.providers.client.RefocusClientModule;
import com.salesforce.pyplyn.duct.providers.jackson.PyplynObjectMapperModule;
import com.salesforce.pyplyn.duct.systemstatus.SystemStatusModule;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.processor.ExtractProcessor;
import com.salesforce.pyplyn.processor.LoadProcessor;
import com.salesforce.pyplyn.util.ModuleBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.salesforce.pyplyn.util.SerializationHelper.canRead;
import static java.util.Objects.isNull;

/**
 * Takes care of initializing the {@link Guice} injector with all the required modules
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AppBootstrap {

    /**
     * Location of the app config file
     */
    private final String configFile;


    /**
     * Determines if the bootstrap operation was performed or not
     */
    private boolean initialized;


    /**
     * A reference to the Guice injector
     */
    private Injector injector;


    /**
     * Class constructor that accepts the app config file path to load
     *
     * @param configFile
     */
    public AppBootstrap(String configFile) {
        this.configFile = configFile;

        // guard against invalid config file specified
        if (!canRead(configFile)) {
            throw new BootstrapException("Could not read Config file " + configFile);
        }
    }


    /**
     * Returns all the modules required to be able to deserialize configurations
     * <p/>  Any modules defined in this method will be included by {@link AppBootstrap#modules()} before initializing the injector
     * <p/>
     * <p/>  The reason for this separation is to allow easy initialization of {@link Configuration} serializers in tests and other projects that need to easily read Pyplyn configurations
     * <p/>
     * <p/><strong>NOTE: If you want to define new models and processors, it is important you follow these guidelines:</strong>
     * <p/> - Use {@link AppBootstrap#modelDeserializationModules()} for {@link Extract}, {@link Transform}, and {@link Load} models
     * <p/> - Use {@link AppBootstrap#modules()} for {@link ExtractProcessor}s, and {@link LoadProcessor}s
     */
    public static List<Module> modelDeserializationModules() {
        return Collections.unmodifiableList(Arrays.asList(
                // Extract
                ModuleBuilder.forExtract(Argus.class),
                ModuleBuilder.forExtract(Refocus.class),

                // Transform
                ModuleBuilder.forTransform(HighestValue.class),
                ModuleBuilder.forTransform(LastDatapoint.class),
                ModuleBuilder.forTransform(SaveMetricMetadata.class),
                ModuleBuilder.forTransform(Threshold.class),
                ModuleBuilder.forTransform(ThresholdMetForDuration.class),
                ModuleBuilder.forTransform(InfoStatus.class),

                // Load
                ModuleBuilder.forLoad(com.salesforce.pyplyn.duct.etl.load.refocus.Refocus.class),

                // Deserialization module
                new PyplynObjectMapperModule()
        ));
    }

    /**
     * Returns all the modules used to configure Pyplyn
     * <p/>
     * <p/>NOTE 1: Override this method to add, replace, or remove modules
     * <p/>
     * <p/><strong>NOTE 2: If you want to define new models and processors, it is important you follow these guidelines:</strong>
     * <p/> - Use {@link AppBootstrap#modelDeserializationModules()} for {@link Extract}, {@link Transform}, and {@link Load} models
     * <p/> - Use {@link AppBootstrap#modules()} for {@link ExtractProcessor}s, and {@link LoadProcessor}s
     */
    public List<Module> modules() {
        List<Module> defaultModules = new ArrayList<>();

        // App config
        defaultModules.add(new AppConfigModule(configFile));
        defaultModules.add(new AppConnectorModule());
        defaultModules.add(new SystemStatusModule());

        // remote clients
        defaultModules.add(new ArgusClientModule());
        defaultModules.add(new RefocusClientModule());

        // processors
        defaultModules.add(ModuleBuilder.forExtractProcessor(ArgusExtractProcessor.class));
        defaultModules.add(ModuleBuilder.forExtractProcessor(RefocusExtractProcessor.class));
        defaultModules.add(ModuleBuilder.forLoadProcessor(RefocusLoadProcessor.class));

        // configuration modules
        defaultModules.addAll(modelDeserializationModules());

        // Enables injection of Set<Configuration> objects
        defaultModules.add(new ClusterModule());
        defaultModules.add(new ConfigurationModule());

        return Collections.unmodifiableList(defaultModules);
    }


    /**
     * Initializes the required components
     */
    public final AppBootstrap bootstrap() {
        // prevent multiple bootstrap operations
        guardInitialized();

        // initialize injector, using configFile, modules(), and defaultModules()
        injector = Guice.createInjector(modules());

        // mark bootstrap complete, if injector was created
        initialized = true;

        // register the shutdown hook into the runtime, to allow it to control program flow if interrupted
        Runtime.getRuntime().addShutdownHook(injector.getInstance(ShutdownHook.class));

        // call hook
        hookAfterBootstrap();

        return this;
    }

    /**
     * Allows extending modules to hook into the bootstrap process, after it has completed
     * <p/> One use-case for this is to store a reference to the initialized injector
     */
    public void hookAfterBootstrap() { }


    /**
     * Returns the initialized Guice injector
     *
     * @throws BootstrapException thrown if the bootstrap was not performed or it failed
     */
    public Injector injector() {
        guardUninitialized();

        return injector;
    }


    /**
     * Guards against multiple bootstrap operations on the same object
     *
     * @throws BootstrapException if app bootstrap already done
     */
    private void guardInitialized() {
        if (initialized) {
            throw new BootstrapException("The app has already been bootstrapped");
        }
    }


    /**
     * Prevents required functionality from executing, if bootstrap was unsuccessful
     *
     * @throws IllegalStateException if app bootstrap was not performed, or in an invalid state
     */
    private void guardUninitialized() {
        if (!initialized || isNull(injector)) {
            throw new IllegalStateException("Bootstrap incomplete or invalid, cannot run app");
        }
    }
}
