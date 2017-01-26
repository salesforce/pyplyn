/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.configuration.UpdatableConfigurationSetProvider;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationWrapper;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.processor.ExtractProcessor;
import com.salesforce.pyplyn.processor.LoadProcessor;
import com.salesforce.pyplyn.status.SystemStatus;
import com.salesforce.pyplyn.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;

/**
 * Pyplyn's logic for processing {@link com.salesforce.pyplyn.configuration.Configuration}s is specified here
 * <p/>
 * Annotated as Singleton as there should only be one instance of this class in operation.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class MetricDuct implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MetricDuct.class);

    /**
     * The app's main configuration
     */
    private final AppConfig appConfig;

    /**
     * Shutdown hook, used to signal to all running processes when they should stop running
     */
    private final ShutdownHook shutdownHook;

    /**
     * Configuration set provider, that implements the logic for running configurations to their specified params
     */
    private UpdatableConfigurationSetProvider<ConfigurationWrapper> configurationProvider;

    private final SystemStatus systemStatus;
    /**
     * All defined extract processors
     */
    private final Set<ExtractProcessor<? extends Extract>> extractProcessors;

    /**
     * All defined load processors
     */
    private final Set<LoadProcessor<? extends Load>> loadProcessors;


    /**
     * Class constructor
     * <p/>All dependencies are injected.
     */
    @Inject
    public MetricDuct(AppConfig appConfig,
                       ShutdownHook shutdownHook,
                       SystemStatus systemStatus,
                       Set<ExtractProcessor<? extends Extract>> extractProcessors,
                       Set<LoadProcessor<? extends Load>> loadProcessors) {
        this.appConfig = appConfig;
        this.shutdownHook = shutdownHook;
        this.systemStatus = systemStatus;
        this.extractProcessors = extractProcessors;
        this.loadProcessors = loadProcessors;
    }

    /**
     * Allows {@link AppBootstrap} to specify the actual {@link UpdatableConfigurationSetProvider} at runtime
     *
     * @return this object (fluent interface implementation)
     */
    MetricDuct setConfigurationProvider(UpdatableConfigurationSetProvider<ConfigurationWrapper> configurationProvider) {
        this.configurationProvider = configurationProvider;
        return this;
    }

    /**
     * Executes the pyplyn ETL logic
     */
    @Override
    public void run() {
        while(!shutdownHook.isShutdown()) {
            // note parameters at start time
            final long time0 = System.currentTimeMillis();
            final AtomicLong extractCounter = new AtomicLong(0L);
            final AtomicLong transformCounter = new AtomicLong(0L);
            final AtomicLong loadCounter = new AtomicLong(0L);

            try {
                Optional<Long> nextRunTime;

                // time how long it takes to process the ETL cycle
                try (Timer.Context context = systemStatus.timer(this.getClass().getSimpleName(), "etl-cycle").time()) {
                    // process all isEnabled configurationProvider
                    nextRunTime = configurationProvider.get().parallelStream()
                            .filter(configuration -> !shutdownHook.isShutdown()) // do not initialize stream if app is shutdown
                            .filter(ConfigurationWrapper::isEnabled)             // only process enabled configurations
                            .filter(ConfigurationWrapper::shouldRun)             // only process configurations that are scheduled

                            // EXTRACT
                            .map(wrapper -> {
                                List<List<TransformationResult>> data = extractProcessors.parallelStream()
                                        .map(processor -> processor.execute(wrapper.configuration().extract()))
                                        .flatMap(Collection::stream)
                                        .collect(Collectors.toList());

                                extractCounter.incrementAndGet();
                                return new Stage(data, wrapper);
                            })

                            // TRANSFORM
                            .map(stage -> {
                                Transform[] transforms = stage.wrapper().configuration().transform();

                                List<List<TransformationResult>> data = stage.data();
                                for (Transform transform : transforms) {

                                    // apply transformations
                                    data = transform.apply(data);
                                }

                                transformCounter.incrementAndGet();
                                return new Stage(data, stage.wrapper());
                            })

                            // LOAD
                            .map(stage -> {
                                Load[] destinations = stage.wrapper().configuration().load();

                                // send all transformation results to all known load processors
                                List<List<TransformationResult>> transformationResults = stage.data();
                                for (List<TransformationResult> result : transformationResults) {
                                    loadProcessors.parallelStream()
                                            .forEach(processor -> processor.execute(result, destinations));
                                }

                                loadCounter.incrementAndGet();

                                // mark configuration as having run
                                //   compute next run, offset by how long it took to process this configuration
                                return stage.wrapper().ran().nextRun(System.currentTimeMillis() - time0);
                            })

                            .min(Long::compareTo);
                }

                // get approximate time of next run
                long nextRun = getNextRun(nextRunTime);

                // print benchmarking data
                String duration = formatNumber((System.currentTimeMillis() - time0) / 1000);
                logger.info("Completed full run of {}/{}/{} e/t/l configurations in {}s; running again in {}s",
                        extractCounter.get(), transformCounter.get(), loadCounter.get(), duration, formatNumber(nextRun / 1000));

                // Sleep until the next configuration is ready to be processed,
                //   or for the maximum specified interval (in AppConfig)
                Thread.sleep(nextRun);

            } catch (InterruptedException e) {
                // exit loop if shutting down
                break;

            } catch (RuntimeException e) {
                logger.error("Unexpected runtime exception", e);
            }
        }
    }


    /**
     * Determines how long until the next configuration needs to be processed
     * <p/>
     * <b>Note: if {@link AppConfig} specifies <i>minRepeatIntervalMillis <= 0</i>, it processes configurations only once, then exits</b>
     */
    long getNextRun(Optional<Long> nearestNextRun) throws InterruptedException {
        // determine time at which the cycle should be started again and sleep until then
        long nextAppRunInterval = appConfig.global().minRepeatIntervalMillis();

        // if run interval is zero, signal we need to stop (run-once)
        if (nextAppRunInterval <= 0) {
            shutdownHook.shutdown();

            // throw an Exception to exit the main loop
            throw new InterruptedException("Running only once!");
        }

        // determine next run if a configuration is present
        if (nearestNextRun.isPresent()) {
            //   if the the next run point in time has already passed, return 0 to run straight away
            nextAppRunInterval = Math.max(nearestNextRun.get() - System.currentTimeMillis(), 0);
        }

        return nextAppRunInterval;
    }
}
