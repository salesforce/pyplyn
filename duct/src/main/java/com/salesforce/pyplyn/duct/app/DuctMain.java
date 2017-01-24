/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.duct.appconfig.ConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.salesforce.pyplyn.duct.appconfig.AppConfigFileLoader.loadFromCLI;
import static java.util.Objects.nonNull;

/**
 * Main entry point
 * <p/>
 * See the execute() {@link #execute(Class, String...)} )} for additional details on how to implement an extension plugin
 */
public final class DuctMain {
    private static final Logger logger = LoggerFactory.getLogger(DuctMain.class);
    private static String programName = "duct";

    /**
     * Main class should not be instantiated
     */
    private DuctMain() { }

    /**
     * Main entry point
     *
     * @param args CLI args
     */
    public static void main(String[] args) {
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
        try {
            // get config file path
            String configFile = loadFromCLI(programName, args);


            // bootstrap and create an app object
            T appBootstrap = cls.getConstructor(String.class).newInstance(configFile);
            MetricDuct app = appBootstrap.app();


            // create executor object and allow it to receive the shutdown signal
            DuctExecutorWrapper executor = new DuctExecutorWrapper();
            executor.registerForShutdown(appBootstrap.shutdownHook());

            AppConfig appConfig = appBootstrap.appConfig();

            // schedule the configuration provider update process and execute immediately with initialDelay=0
            long updateInterval = appConfig.global().updateConfigurationIntervalMillis();
            executor.scheduleAtFixedRate(appBootstrap.configurationProvider(), 0, updateInterval, TimeUnit.MILLISECONDS);

            // schedule the system status task (if enabled)
            if (nonNull(appConfig.alert()) && appConfig.alert().isEnabled()) {
                Long interval = appConfig.alert().checkIntervalMillis();
                executor.scheduleAtFixedRate(appBootstrap.systemStatus(), interval, interval, TimeUnit.MILLISECONDS);
            }

            // execute app; this operation will block until program completion
            executor.schedule(app);

        } catch (ConfigParseException e) {//NOPMD
            // nothing to do, allow the program to exit gracefully

        } catch (Exception e) {
            logger.error("Unexpected exception", e);
        }
    }
}
