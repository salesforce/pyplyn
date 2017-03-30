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
import com.salesforce.pyplyn.configuration.UpdatableConfigurationSetProvider;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.duct.appconfig.AppConfigModule;
import com.salesforce.pyplyn.duct.connector.AppConnectorModule;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationModule;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationWrapper;
import com.salesforce.pyplyn.duct.etl.configuration.DistributedConfigurationProvider;
import com.salesforce.pyplyn.duct.etl.configuration.SinglePartitionConfigurationProvider;
import com.salesforce.pyplyn.duct.etl.extract.argus.Argus;
import com.salesforce.pyplyn.duct.etl.extract.argus.ArgusExtractProcessor;
import com.salesforce.pyplyn.duct.etl.extract.refocus.Refocus;
import com.salesforce.pyplyn.duct.etl.extract.refocus.RefocusExtractProcessor;
import com.salesforce.pyplyn.duct.etl.load.refocus.RefocusLoadProcessor;
import com.salesforce.pyplyn.duct.etl.transform.standard.*;
import com.salesforce.pyplyn.duct.providers.client.ArgusClientModule;
import com.salesforce.pyplyn.duct.providers.client.RefocusClientModule;
import com.salesforce.pyplyn.duct.providers.jackson.JacksonSerializationInitModule;
import com.salesforce.pyplyn.duct.systemstatus.SystemStatusModule;
import com.salesforce.pyplyn.status.SystemStatus;
import com.salesforce.pyplyn.util.ModuleBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.salesforce.pyplyn.util.SerializationHelper.canRead;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
     * Override this method to include additional modules in Guice
     *
     * @return list that will be appended to the defaultModules() list and then used to initialize the Guice injector
     */
    public List<Module> modules() {
        return Collections.emptyList();
    }


    /**
     * Defines the default modules used by Duct
     * <p/>
     * <p/>Making this private was a conscious choice to limit the possibility of changing this program's expected (standard) behavior.
     * <p/>
     * <p/>Extending plugins should add functionality, and not replace it, guaranteeing, as much as possible,
     *   deterministic functionality.
     */
    private List<Module> defaultModules() {
        return Collections.unmodifiableList(Arrays.asList(
                // App config
                new AppConfigModule(configFile),
                new AppConnectorModule(),
                new JacksonSerializationInitModule(),
                new SystemStatusModule(),

                // remote clients
                new ArgusClientModule(),
                new RefocusClientModule(),

                // Extract
                ModuleBuilder.forExtract(Argus.class, ArgusExtractProcessor.class),
                ModuleBuilder.forExtract(Refocus.class, RefocusExtractProcessor.class),

                // Transform
                ModuleBuilder.forTransform(HighestValue.class),
                ModuleBuilder.forTransform(LastDatapoint.class),
                ModuleBuilder.forTransform(SaveMetricMetadata.class),
                ModuleBuilder.forTransform(Threshold.class),
                ModuleBuilder.forTransform(ThresholdMetForDuration.class),
                ModuleBuilder.forTransform(InfoStatus.class),

                // Load
                ModuleBuilder.forLoad(com.salesforce.pyplyn.duct.etl.load.refocus.Refocus.class, RefocusLoadProcessor.class),

                // ETL configuration parsing
                new ConfigurationModule()
        ));
    }


    /**
     * Initializes the required components
     */
    public final AppBootstrap bootstrap() {
        // prevent multiple bootstrap operations
        guardInitialized();

        // initialize injector, using configFile, modules(), and defaultModules()
        injector = Guice.createInjector(initializeModules());

        // mark bootstrap complete, if injector was created
        initialized = true;

        // register the shutdown hook into the runtime, to allow it to control program flow if interrupted
        Runtime.getRuntime().addShutdownHook(shutdownHook());

        return this;
    }


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
     * Shutdown hook, used to signal to all running processes when they should stop running
     *
     * @throws BootstrapException thrown if the bootstrap was not performed or it failed
     */
    public ShutdownHook shutdownHook() {
        guardUninitialized();

        return injector.getInstance(ShutdownHook.class);
    }


    /**
     * Returns the {@link AppConfig} singleton, initialized after parsing the specified configFile
     *
     * @throws BootstrapException thrown if the bootstrap was not performed or it failed
     */
    public AppConfig appConfig() {
        guardUninitialized();

        return injector.getInstance(AppConfig.class);
    }


    /**
     * SystemStatus thread that monitors the app and provides status updates to registered consumers
     *
     * @throws BootstrapException thrown if the bootstrap was not performed or it failed
     */
    public SystemStatus systemStatus() {
        guardUninitialized();

        return injector.getInstance(SystemStatus.class);
    }


    /**
     * Returns the Configuration provider used to load the Set&lt;{@link Configuration}&gt; partition
     *   for the current host (returns all configurations if running in single partition mode
     *
     * @throws BootstrapException thrown if the bootstrap was not performed or it failed
     */
    public UpdatableConfigurationSetProvider<ConfigurationWrapper> configurationProvider() {
        guardUninitialized();

        if (nonNull(appConfig().hazelcast()) && appConfig().hazelcast().isEnabled()) {
            return injector.getInstance(DistributedConfigurationProvider.class);
        } else {
            return injector.getInstance(SinglePartitionConfigurationProvider.class);
        }
    }


    /**
     * Returns a MetricDuct object, ready to execute
     * <p/>
     * If the app hasn't been bootstrapped yet, it is handled before {@link MetricDuct} is returned
     *
     * @throws BootstrapException thrown if the bootstrap was not performed or it failed
     */
    public MetricDuct app() {
        if (!initialized) {
            bootstrap();
        }

        // ensures that the injector was initialized successfully
        guardUninitialized();

        return injector.getInstance(MetricDuct.class).setConfigurationProvider(configurationProvider());
    }


    /**
     * Creates a new list of modules that is to be passed to the Guice injector
     */
    private List<Module> initializeModules() {
        // combine defaultModules() and modules() to provide the final list used to initialize Guice
        List<Module> modules = new ArrayList<>(defaultModules());
        modules.addAll(modules());
        return Collections.unmodifiableList(modules);
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
     * @throws BootstrapException if app bootstrap was not performed, or in an invalid state
     */
    private void guardUninitialized() {
        if (!initialized || isNull(injector)) {
            throw new IllegalStateException("Bootstrap incomplete or invalid, cannot run app");
        }
    }
}
