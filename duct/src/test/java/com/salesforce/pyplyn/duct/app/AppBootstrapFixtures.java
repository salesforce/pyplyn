/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.app;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.salesforce.argus.ArgusClient;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.configuration.UpdatableConfigurationSetProvider;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.duct.connector.AppConnector;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationProvider;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationWrapper;
import com.salesforce.pyplyn.duct.etl.configuration.DistributedConfigurationProvider;
import com.salesforce.pyplyn.duct.etl.configuration.SinglePartitionConfigurationProvider;
import com.salesforce.pyplyn.duct.etl.extract.argus.Argus;
import com.salesforce.pyplyn.duct.etl.extract.argus.ArgusExtractProcessor;
import com.salesforce.pyplyn.duct.etl.extract.refocus.Refocus;
import com.salesforce.pyplyn.duct.etl.extract.refocus.RefocusExtractProcessor;
import com.salesforce.pyplyn.duct.etl.load.refocus.RefocusLoadProcessor;
import com.salesforce.pyplyn.duct.etl.transform.highestvalue.HighestValue;
import com.salesforce.pyplyn.duct.etl.transform.infostatus.InfoStatus;
import com.salesforce.pyplyn.duct.etl.transform.lastdatapoint.LastDatapoint;
import com.salesforce.pyplyn.duct.etl.transform.savemetricmetadata.SaveMetricMetadata;
import com.salesforce.pyplyn.duct.etl.transform.threshold.Threshold;
import com.salesforce.pyplyn.duct.providers.client.RemoteClientFactory;
import com.salesforce.pyplyn.duct.providers.jackson.ObjectMapperProvider;
import com.salesforce.pyplyn.duct.systemstatus.ConsoleOutputConsumer;
import com.salesforce.pyplyn.duct.systemstatus.SystemStatusRunnable;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.status.MeterType;
import com.salesforce.pyplyn.status.SystemStatus;
import com.salesforce.pyplyn.status.SystemStatusConsumer;
import com.salesforce.pyplyn.util.MultibinderFactory;
import com.salesforce.pyplyn.util.ObjectMapperWrapper;
import com.salesforce.pyplyn.util.SerializationHelper;
import com.salesforce.refocus.RefocusClient;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test fixtures class
 * <p/>Prepares a mocked version of the modules defined in {@link AppBootstrap}
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class AppBootstrapFixtures {
    private static final Logger logger = LoggerFactory.getLogger(AppBootstrapFixtures.class);
    public static final String LOAD_PROCESSOR_TIMER_NAME = "LoadProcessor";

    AppConfigMocks appConfigMocks;

    @Mock
    AppConnector appConnector;

    List<AbstractConnector> connectors = new ArrayList<>();

    @Mock
    SystemStatusRunnable systemStatus;

    @Mock
    RemoteClientFactory<ArgusClient> argusClientFactory;

    @Mock
    RemoteClientFactory<RefocusClient> refocusClientFactory;

    @Mock
    SystemStatusConsumer statusConsumer;

    @Mock
    ArgusExtractProcessor argusExtractProcessor;

    @Mock
    RefocusExtractProcessor refocusExtractProcessor;

    @Mock
    TransformationResult transformationResult;

    @Mock
    RefocusLoadProcessor refocusLoadProcessor;

    @Mock
    ConfigurationProvider configurationProvider;

    @Mock
    private UpdatableConfigurationSetProvider<ConfigurationWrapper> configurationSetProvider;

    private Injector injector;
    private MetricDuct app;


    public AppBootstrapFixtures() {
        MockitoAnnotations.initMocks(this);

        // init other mocks and fixtures
        appConfigMocks = new AppConfigMocks();
    }


    /**
     * Freezes the setup and initializes the injector and MetricDuct app
     * <p/> From this point forward, any call to any of the fixture's methods will have no effect, unless freeze is called again
     */
    public AppBootstrapFixtures freeze() {
        injector = Guice.createInjector(new MockedDependenciesModule());
        app = injector.getInstance(MetricDuct.class).setConfigurationProvider(configurationSetProvider);
        return this;
    }


    /**
     *  Mock logic
     */
    public AppBootstrapFixtures oneConfiguration() {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().getWrapper(null))))
                .when(configurationSetProvider)
                .get();
        return this;
    }

    public AppBootstrapFixtures allProcessorsReturnAnExtractResult() {
        doReturn(Collections.singletonList(Collections.singletonList(transformationResult))).when(argusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Argus.class)).when(argusExtractProcessor).filter(any());
        doCallRealMethod().when(argusExtractProcessor).execute(any());

        doReturn(Collections.singletonList(Collections.singletonList(transformationResult))).when(refocusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Refocus.class)).when(refocusExtractProcessor).filter(any());
        doCallRealMethod().when(refocusExtractProcessor).execute(any());

        return this;
    }

    public AppBootstrapFixtures simulateLoadProcessingTime(final long duration) {
        doAnswer(invocation -> filter(invocation.getArguments(), com.salesforce.pyplyn.duct.etl.load.refocus.Refocus.class)).when(refocusLoadProcessor).filter(any());
        doAnswer(invocation -> {
            try (Timer.Context timer = systemStatus.timer(LOAD_PROCESSOR_TIMER_NAME, LOAD_PROCESSOR_TIMER_NAME).time()) {
                logger.info("Sleeping for {}ms before exiting LoadProcessor.process", duration);
                sleepForMs(duration);
                systemStatus.meter(LOAD_PROCESSOR_TIMER_NAME, MeterType.LoadSuccess).mark();
            }
            return Collections.singletonList(Boolean.TRUE);

        }).when(refocusLoadProcessor).process(any(), any());
        doCallRealMethod().when(refocusLoadProcessor).execute(any(), any());

        return this;
    }



    /**
     * Real logic
     */
    public AppBootstrapFixtures realSinglePartitionConfigProvider(Injector injector) {
        configurationSetProvider = spy(injector.getInstance(SinglePartitionConfigurationProvider.class));
        return this;
    }

    public AppBootstrapFixtures realDistributedConfigProvider() {
        configurationSetProvider = spy(injector.getInstance(DistributedConfigurationProvider.class));
        return this;
    }

    public AppBootstrapFixtures realSystemStatus() {
        systemStatus = spy(new SystemStatusRunnable(appConfigMocks.get()));
        return this;
    }



    /**
     * Getters
     */

    public MetricDuct app() {
        return app;
    }

    public Injector injector() {
        return injector;
    }

    public AppConfigMocks appConfigMocks() {
        return appConfigMocks;
    }

    public SystemStatusRunnable systemStatus() {
        return systemStatus;
    }

    public SystemStatusConsumer statusConsumer() {
        return statusConsumer;
    }

    /**
     * Utility methods
     */

    /**
     * Sleeps for the specified duration and ignores any interrupts, guaranteeing the specified time has passed
     * <p/> Do not call this method with large duration values, as it will affect your tests' executions
     *
     * @param duration how many milliseconds the thread will sleep for
     */
    public static void sleepForMs(long duration) {
        long time0 = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(System.currentTimeMillis()-time0);

            } catch (InterruptedException e) {
                // ignoring any interrupts, this is test code and
                //   it's the developer's responsibility to not call this method with huge values
                //   and cause the test to not be able to be gracefully interrupted
            }
        } while (System.currentTimeMillis() - time0 < duration);
    }

    /**
     * Used to replace the default filtering in Extract and Load processors
     *   since we are mocking the models, which then will then in turn not match the "obj instanceof ModelClass" test
     */
    private <T, S> List<T> filter(S[] objects, final Class<T> cls) {
        return Arrays.stream(objects)
                .filter(cls::isInstance)
                .map(cls::cast)
                .collect(Collectors.toList());
    }


    /**
     * Initializes all default components with mocks
     * <p/> Does not mock the serialization helper nor the transform functions
     */
    private class MockedDependenciesModule extends AbstractModule {
        @Override
        protected void configure() {
            // app configuration
            bind(AppConfig.class).toInstance(appConfigMocks.get());

            // app connectors
            bind(AppConnector.class).toInstance(appConnector);
            MultibinderFactory.appConnectors(binder()).addBinding().toInstance(connectors);

            // Jackson mappers (depends on MultibinderFactory.extractDatasources, MultibinderFactory.transformFunctions, MultibinderFactory.loadDestinations)
            bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);
            bind(SerializationHelper.class).to(ObjectMapperWrapper.class).asEagerSingleton();

            // System Status
            bind(SystemStatus.class).toInstance(systemStatus);
            MultibinderFactory.statusConsumers(binder()).addBinding().toInstance(new ConsoleOutputConsumer());
            MultibinderFactory.statusConsumers(binder()).addBinding().toInstance(statusConsumer);

            // ArgusClient
            bind(new TypeLiteral<Class<ArgusClient>>() {}).toInstance(ArgusClient.class);
            bind(new TypeLiteral<RemoteClientFactory<ArgusClient>>() {}).toInstance(argusClientFactory);

            // RefocusClient
            bind(new TypeLiteral<Class<RefocusClient>>() {}).toInstance(RefocusClient.class);
            bind(new TypeLiteral<RemoteClientFactory<RefocusClient>>() {}).toInstance(refocusClientFactory);

            // Argus Extract
            MultibinderFactory.extractDatasources(binder()).addBinding().toInstance(Argus.class);
            MultibinderFactory.extractProcessors(binder()).addBinding().toInstance(argusExtractProcessor);

            // Refocus Extract
            MultibinderFactory.extractDatasources(binder()).addBinding().toInstance(Refocus.class);
            MultibinderFactory.extractProcessors(binder()).addBinding().toInstance(refocusExtractProcessor);

            // Transforms
            MultibinderFactory.transformFunctions(binder()).addBinding().toInstance(HighestValue.class);
            MultibinderFactory.transformFunctions(binder()).addBinding().toInstance(LastDatapoint.class);
            MultibinderFactory.transformFunctions(binder()).addBinding().toInstance(SaveMetricMetadata.class);
            MultibinderFactory.transformFunctions(binder()).addBinding().toInstance(Threshold.class);
            MultibinderFactory.transformFunctions(binder()).addBinding().toInstance(InfoStatus.class);

            // Refocus Load
            MultibinderFactory.loadDestinations(binder()).addBinding().toInstance(com.salesforce.pyplyn.duct.etl.load.refocus.Refocus.class);
            MultibinderFactory.loadProcessors(binder()).addBinding().toInstance(refocusLoadProcessor);

            // Configurations
            bind(new TypeLiteral<Set<Configuration>>() {}).toProvider(configurationProvider);
        }
    }


    public class AppConfigMocks {
        @Mock AppConfig appConfig;

        @Mock
        AppConfig.Global global;

        @Mock
        AppConfig.Alert alert;

        @Mock
        AppConfig.Hazelcast hazelcast;

        Map<String, Double> thresholds = new HashMap<>();

        public AppConfigMocks() {
            MockitoAnnotations.initMocks(this);
            doReturn(global).when(appConfig).global();
            doReturn(alert).when(appConfig).alert();
            doReturn(hazelcast).when(appConfig).hazelcast();

            // set defaults
            doReturn(100L).when(global).minRepeatIntervalMillis(); // run flow every 100ms
            doReturn(60000L).when(global).updateConfigurationIntervalMillis(); // update configurations every minute (avoid updating configs in most tests)

            doReturn(Boolean.FALSE).when(hazelcast).isEnabled();

            doReturn(Boolean.FALSE).when(alert).isEnabled();
            doReturn(500L).when(alert).checkIntervalMillis(); // publish status updates every 500ms
            doReturn(thresholds).when(alert).thresholds();
        }

        public AppConfigMocks enableAlerts() {
            doReturn(Boolean.TRUE).when(alert).isEnabled();
            return this;
        }

        public AppConfigMocks runOnce() {
            doReturn(-1L).when(global).minRepeatIntervalMillis(); // only run app once
            return this;
        }

        public AppConfigMocks checkMeter(String name, Double value) {
            thresholds.put(name, value);
            return this;
        }

        public AppConfig get() {
            return appConfig;
        }
    }


    public class ConfigurationMocks {
        @Mock
        Configuration configuration;

        @Mock
        Argus argusExtract;

        @Mock
        Refocus refocusExtract;

        @Mock
        com.salesforce.pyplyn.duct.etl.load.refocus.Refocus refocusLoad;

        public ConfigurationMocks() {
            MockitoAnnotations.initMocks(this);

            // configure defaults
            this
                    .enabledConfiguration()
                    .argusExtract()
                    .lastDatapoint()
                    .refocusLoad();
        }

        public ConfigurationMocks enabledConfiguration() {
            doReturn(Boolean.TRUE).when(configuration).isEnabled();
            return this;
        }

        public ConfigurationMocks argusExtract() {
            doReturn(new Argus[]{argusExtract}).when(configuration).extract();
            return this;
        }

        public ConfigurationMocks refocusExtract() {
            doReturn(new Refocus[]{refocusExtract}).when(configuration).extract();
            return this;
        }

        public ConfigurationMocks refocusLoad() {
            doReturn(new com.salesforce.pyplyn.duct.etl.load.refocus.Refocus[]{refocusLoad}).when(configuration).load();
            return this;
        }

        public ConfigurationMocks lastDatapoint() {
            doReturn(new Transform[]{new LastDatapoint()}).when(configuration).transform();
            return this;
        }


        public Configuration get() {
            return configuration;
        }

        public ConfigurationWrapper getWrapper(Long lastRun) {
            return spy(new ConfigurationWrapper(configuration, lastRun));
        }
    }
}