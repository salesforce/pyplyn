/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import static com.salesforce.pyplyn.duct.appconfig.AppConfigFileLoader.loadFromCLI;
import static java.util.Objects.nonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Key;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.duct.appconfig.ConfigParseException;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationUpdateManager;
import com.salesforce.pyplyn.duct.etl.configuration.TaskManager;
import com.salesforce.pyplyn.status.SystemStatus;

/**
 * Main entry point
 * <p/>
 * See the execute() {@link #execute(Class, String...)} )} for additional details on how to implement an extension plugin
 */
public final class DuctMain {
    private static final Logger logger = LogManager.getLogger(DuctMain.class);
    private static String programName = "pyplyn";

    private static final Long SHUTDOWN_TIMEOUT_MILLIS = 10000L;


    /**
     * Main class should not be instantiated
     */
    private DuctMain() { }

    /**
     * Main entry point
     *
     * @param args CLI args
     */
    public static void main(String... args) {
        execute(AppBootstrap.class, args);
    }

    /**
     * Allows replacing the help output's program name
     *
     * @param programName what to display in usage
     */
    public static void setProgramName(String programName) {
        DuctMain.programName = programName;
    }

    /**
     * Main functionality wrapper
     * <p/>Defined with generic type to allow easy extension, for example:
     * <p/>
     * <p/>1. Extend AppBoostrap and overwrite modules(), specifying the any new objects
     * <p/>2. Define a new main(String... args) entry point and call execute(NewAppBoostrap.class, args)
     *
     * @param cls Bootstrap class, providing the complete list of modules to load
     * @param args main runtime args
     * @param <T> Class type of the AppBoostrap object to use
     */
    public static <T extends AppBootstrap> void execute(Class<T> cls, String... args) {
        final Instant START_TIME = Instant.now();
        try {
            // get config file path
            String configFile = loadFromCLI(programName, args);

            // bootstrap and create an app object
            T appBootstrap = cls.getConstructor(String.class).newInstance(configFile);
            appBootstrap.bootstrap();

            // init components
            ShutdownHook shutdownHook = appBootstrap.injector().getInstance(ShutdownHook.class);
            ConfigurationUpdateManager configurationManager = appBootstrap.injector().getInstance(ConfigurationUpdateManager.class);
            SystemStatus systemStatus = appBootstrap.injector().getInstance(SystemStatus.class);
            TaskManager<Configuration> taskManager = appBootstrap.injector().getInstance(new Key<TaskManager<Configuration>>() {});

            // register executor for shutdown
            final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
            try {
                shutdownHook.registerExecutor(executor);

                AppConfig appConfig = appBootstrap.injector().getInstance(AppConfig.class);


                // if executing in runOnce mode, wait until all configurations are processed and shut down
                if (appConfig.global().runOnce()) {
                    runOnceMode(executor, configurationManager, taskManager, shutdownHook);
                } else {
                    runAsService(executor, appConfig, configurationManager, systemStatus);
                }

                // await termination and shutdown executor
                shutdownHook.awaitShutdown();
                shutdownHook.awaitExecutorsTermination(SHUTDOWN_TIMEOUT_MILLIS);

            } finally {
                // ensure that we shut down the executor creating by this method; in most cases this should be a no-op
                ShutdownHook.awaitExecutorTermination(executor, SHUTDOWN_TIMEOUT_MILLIS);
            }

        } catch (ConfigParseException e) {
            // nothing to do, allow the program to exit gracefully
            logger.error("Failed to parse configuration, program will exit.", e);
        } catch (Exception e) {
            logger.error("Unexpected exception", e);

        } finally {
            logger.info("Stopping {} after {}s", programName, Duration.between(START_TIME, Instant.now()).getSeconds());
        }
    }

    /**
     * Only executes once then shuts down
     */
    private static void runOnceMode(ScheduledExecutorService EXECUTOR, ConfigurationUpdateManager configurationManager, TaskManager<Configuration> taskManager, ShutdownHook shutdownHook) throws InterruptedException {
        // execute service once
        EXECUTOR.execute(configurationManager);

        // wait until all tasks complete and shutdown
        configurationManager.awaitUntilConfigured();
        taskManager.awaitUntilFinished();
        shutdownHook.shutdown();
    }

    /**
     * Runs the program as a service
     */
    private static void runAsService(ScheduledExecutorService EXECUTOR, AppConfig appConfig, ConfigurationUpdateManager configurationManager, SystemStatus systemStatus) {
        // schedule service and execute immediately with initialDelay=0
        EXECUTOR.scheduleAtFixedRate(configurationManager, 0, appConfig.global().updateConfigurationIntervalMillis(), TimeUnit.MILLISECONDS);

        // schedule the system status task (if enabled)
        if (nonNull(appConfig.alert()) && appConfig.alert().isEnabled()) {
            Long interval = appConfig.alert().checkIntervalMillis();
            EXECUTOR.scheduleAtFixedRate(systemStatus, interval, interval, TimeUnit.MILLISECONDS);
        }
    }
}
