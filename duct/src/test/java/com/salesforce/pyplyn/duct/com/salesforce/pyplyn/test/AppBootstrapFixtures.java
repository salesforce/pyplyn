/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.hazelcast.core.IMap;
import com.salesforce.argus.ArgusClient;
import com.salesforce.argus.model.MetricResponse;
import com.salesforce.pyplyn.cache.CacheFactory;
import com.salesforce.pyplyn.cache.ConcurrentCacheMap;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.configuration.UpdatableConfigurationSetProvider;
import com.salesforce.pyplyn.duct.app.AppBootstrap;
import com.salesforce.pyplyn.duct.app.BootstrapException;
import com.salesforce.pyplyn.duct.app.MetricDuct;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.duct.cluster.Cluster;
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
import com.salesforce.pyplyn.duct.etl.transform.standard.*;
import com.salesforce.pyplyn.duct.providers.client.RemoteClientFactory;
import com.salesforce.pyplyn.duct.providers.jackson.ObjectMapperProvider;
import com.salesforce.pyplyn.duct.systemstatus.ConsoleOutputConsumer;
import com.salesforce.pyplyn.duct.systemstatus.SystemStatusRunnable;
import com.salesforce.pyplyn.model.ETLMetadata;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.status.SystemStatus;
import com.salesforce.pyplyn.status.SystemStatusConsumer;
import com.salesforce.pyplyn.util.MultibinderFactory;
import com.salesforce.pyplyn.util.ObjectMapperWrapper;
import com.salesforce.pyplyn.util.SerializationHelper;
import com.salesforce.refocus.RefocusClient;
import com.salesforce.refocus.model.Sample;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static final String MOCK_CONNECTOR_NAME = "mock-connector";

    @Mock
    private AppConnector appConnector;

    @Mock
    private SystemStatusRunnable systemStatus;

    @Mock
    private Meter systemStatusMeter;

    @Mock
    private Timer systemStatusTimer;

    @Mock
    private RemoteClientFactory<ArgusClient> argusClientFactory;

    @Mock
    private ArgusClient argusClient;

    @Mock
    private RemoteClientFactory<RefocusClient> refocusClientFactory;

    @Mock
    private RefocusClient refocusClient;

    @Mock
    private SystemStatusConsumer statusConsumer;

    @Mock
    private ArgusExtractProcessor argusExtractProcessor;

    @Mock
    private RefocusExtractProcessor refocusExtractProcessor;

    @Mock
    private CacheFactory cacheFactory;

    @Mock
    private ConcurrentCacheMap<MetricResponse> metricResponseCache;

    @Mock
    private ConcurrentCacheMap<Sample> sampleCache;

    @Mock
    private TransformationResult transformationResult;

    @Mock
    private ETLMetadata transformationResultMetadata;

    @Mock
    private RefocusLoadProcessor refocusLoadProcessor;

    @Mock
    private ConfigurationProvider configurationProvider;

    @Mock
    private Cluster cluster;

    @Mock
    IMap<Configuration, ConfigurationWrapper> hazelcastConfigurationMap;

    @Mock
    private ShutdownHook shutdownHook;

    @Mock
    private UpdatableConfigurationSetProvider<ConfigurationWrapper> configurationSetProvider;

    private AppConfigMocks appConfigMocks;
    private List<AbstractConnector> connectors = new ArrayList<>();
    private Injector injector;
    private MetricDuct app;

    /**
     * If enabled, will initialize all latches in {@link AppBootstrapLatches} to wait, otherwise
     *  they are disabled by default and the program flows without any limits
     */
    private boolean withLatches = false;


    /**
     * Initializes the fixtures and default
     */
    public AppBootstrapFixtures() {
        MockitoAnnotations.initMocks(this);

        // init other mocks and fixtures
        appConfigMocks = new AppConfigMocks();

        // System status delegates
        doReturn(systemStatusMeter).when(systemStatus).meter(any(), any());
        doReturn(systemStatusTimer).when(systemStatus).timer(any(), any());

        // Remote clients
        doReturn(refocusClient).when(refocusClientFactory).getClient(any());
        doReturn(argusClient).when(argusClientFactory).getClient(any());

        // Transformation results
        doReturn(transformationResultMetadata).when(transformationResult).metadata();
        doReturn(1L).when(transformationResult).value();

        // Cache
        doReturn(metricResponseCache).when(cacheFactory).newCache();
        doReturn(sampleCache).when(cacheFactory).newCache();
    }


    /**
     * Freezes the setup and initializes the injector and MetricDuct app
     * <p/> From this point forward, any call to any of the fixture's methods will have no effect, unless freeze is called again
     */
    public AppBootstrapFixtures freeze() {
        // init latches
        AppBootstrapLatches.init(withLatches);

        // creates the injector and the MetricDuct app object
        injector = Guice.createInjector(new MockedDependenciesModule());
        app = spy(injector.getInstance(MetricDuct.class).setConfigurationProvider(configurationSetProvider));

        // initializes MetricDuct latches and other fixtures
        initAppFixtures();

        return this;
    }


    /*
     *  Mock logic
     */

    public AppBootstrapFixtures enableLatches() {
        withLatches = true;
        return this;
    }

    public AppBootstrapFixtures oneArgusToRefocusConfiguration() {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().argusExtract(null).buildWrapper(null))))
                .when(configurationSetProvider)
                .get();
        return this;
    }

    public AppBootstrapFixtures oneArgusToRefocusConfigurationWithCache() {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().argusExtract(null).shouldCache(60000).buildWrapper(null))))
                .when(configurationSetProvider)
                .get();
        return this;
    }

    public AppBootstrapFixtures oneArgusToRefocusConfigurationWithDefaultValue(Double defaultValue) {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().argusExtract(defaultValue).buildWrapper(null))))
                .when(configurationSetProvider)
                .get();
        return this;
    }

    public AppBootstrapFixtures argusToRefocusConfigurationWithTransforms(Transform[] transforms) {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().argusExtract(null).withTransforms(transforms).buildWrapper(null))))
                .when(configurationSetProvider)
                .get();
        return this;
    }

    public AppBootstrapFixtures oneRefocusToRefocusConfiguration() {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().refocusExtract(null).buildWrapper(null))))
                .when(configurationSetProvider)
                .get();
        return this;
    }

    public AppBootstrapFixtures oneRefocusToRefocusConfigurationWithCache() {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().refocusExtract(null).shouldCache(60000).buildWrapper(null))))
                .when(configurationSetProvider)
                .get();
        return this;
    }

    public AppBootstrapFixtures oneRefocusToRefocusConfigurationWithDefaultValue(Double defaultValue) {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().refocusExtract(defaultValue).buildWrapper(null))))
                .when(configurationSetProvider)
                .get();
        return this;
    }

    public AppBootstrapFixtures returnMockedTransformationResultFromAllExtractProcessors() {
        doReturn(Collections.singletonList(Collections.singletonList(transformationResult))).when(argusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Argus.class)).when(argusExtractProcessor).filter(any());
        doCallRealMethod().when(argusExtractProcessor).execute(any());

        doReturn(Collections.singletonList(Collections.singletonList(transformationResult))).when(refocusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Refocus.class)).when(refocusExtractProcessor).filter(any());
        doCallRealMethod().when(refocusExtractProcessor).execute(any());

        return this;
    }

    public AppBootstrapFixtures returnTransformationResultFromAllExtractProcessors(List<List<TransformationResult>> results) {
        doReturn(results).when(argusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Argus.class)).when(argusExtractProcessor).filter(any());
        doCallRealMethod().when(argusExtractProcessor).execute(any());

        doReturn(results).when(refocusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Refocus.class)).when(refocusExtractProcessor).filter(any());
        doCallRealMethod().when(refocusExtractProcessor).execute(any());

        return this;
    }

    public AppBootstrapFixtures callRealArgusExtractProcessor() {
        // we need to reinitialize the object to provide access to the real failed/succeeded (protected) methods
        argusExtractProcessor = spy(new ArgusExtractProcessor(argusClientFactory, cacheFactory, shutdownHook));

        // we are replacing the default filtering logic, since the passed object will be a mock
        doAnswer(invocation -> {
            AppBootstrapLatches.beforeExtractProcessorStarts().countDown();
            AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().await();

            return filter(invocation.getArguments(), Argus.class);
        }).when(argusExtractProcessor).filter(any());

        return this;
    }

    public AppBootstrapFixtures argusClientCanNotAuth() throws UnauthorizedException {
        doReturn(false).when(argusClient).isAuthenticated();
        doThrow(UnauthorizedException.class).when(argusClient).auth();
        return this;
    }

    public AppBootstrapFixtures argusClientReturns(List<MetricResponse> metrics) throws UnauthorizedException {
        doReturn(metrics).when(argusClient).getMetrics(any());
        return this;
    }

    public AppBootstrapFixtures argusClientThrowsExceptionOnGetMetrics() throws UnauthorizedException {
        doThrow(UnauthorizedException.class).when(argusClient).getMetrics(any());
        return this;
    }

    public AppBootstrapFixtures callRealRefocusExtractProcessor() {
        // we need to reinitialize the object to provide access to the real failed/succeeded (protected) methods
        refocusExtractProcessor = spy(new RefocusExtractProcessor(refocusClientFactory, cacheFactory, shutdownHook));

        // we are replacing the default filtering logic, since the passed object will be a mock
        doAnswer(invocation -> {
            AppBootstrapLatches.beforeExtractProcessorStarts().countDown();
            AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().await();

            return filter(invocation.getArguments(), Refocus.class);
        }).when(refocusExtractProcessor).filter(any());

        return this;
    }

    public AppBootstrapFixtures refocusClientCanNotAuth() throws UnauthorizedException {
        doReturn(false).when(refocusClient).isAuthenticated();
        doThrow(UnauthorizedException.class).when(refocusClient).auth();
        return this;
    }

    public AppBootstrapFixtures refocusClientReturns(List<Sample> samples) throws UnauthorizedException {
        doReturn(samples).when(refocusClient).getSamples(any());
        return this;
    }

    public AppBootstrapFixtures refocusClientThrowsExceptionOnGetSample() throws UnauthorizedException {
        doThrow(UnauthorizedException.class).when(refocusClient).getSamples(any());
        return this;
    }

    public AppBootstrapFixtures callRealRefocusLoadProcessor() {
        // we need to reinitialize the object to provide access to the real failed/succeeded (protected) methods
        refocusLoadProcessor = spy(new RefocusLoadProcessor(refocusClientFactory, shutdownHook));

        // we are replacing the default filtering logic, since the passed object will be a mock
        doAnswer(invocation -> {
            AppBootstrapLatches.beforeLoadProcessorStarts().countDown();
            AppBootstrapLatches.holdOffBeforeLoadProcessorStarts().await();

            return filter(invocation.getArguments(), com.salesforce.pyplyn.duct.etl.load.refocus.Refocus.class);
        }).when(refocusLoadProcessor).filter(any());

        return this;
    }

    public AppBootstrapFixtures simulateRefocusLoadProcessingDelay(final long duration) {
        // we need to reinitialize the object to provide access to the real failed/succeeded (protected) methods
        refocusLoadProcessor = spy(new RefocusLoadProcessor(refocusClientFactory, shutdownHook));

        // we are replacing the default filtering logic, since the passed object will be a mock
        doAnswer(invocation -> filter(invocation.getArguments(), com.salesforce.pyplyn.duct.etl.load.refocus.Refocus.class)).when(refocusLoadProcessor).filter(any());

        doAnswer(invocation -> {
            AppBootstrapLatches.beforeLoadProcessorStarts().countDown();
            AppBootstrapLatches.holdOffBeforeLoadProcessorStarts().await();

            sleepForMs(duration);
            return invocation.callRealMethod();

        }).when(refocusLoadProcessor).process(any(), any());

        return this;
    }

    public AppBootstrapFixtures configurationProviderReturns(Configuration ... configurations) {
        doReturn(new HashSet<>(Arrays.asList(configurations))).when(configurationProvider).get();
        return this;
    }

    public AppBootstrapFixtures configurationProviderThrowsException() {
        doThrow(BootstrapException.class).when(configurationProvider).get();
        return this;
    }

    public AppBootstrapFixtures clusterReturns(Configuration ... configurations) {
        doReturn(hazelcastConfigurationMap).when(cluster).distributedMap(any());

        Map<Configuration, ConfigurationWrapper> configurationMap =
                Stream.of(configurations).collect(Collectors.toMap(Function.identity(), c -> new ConfigurationWrapper(c, null)));

        doReturn(configurationMap.keySet()).when(hazelcastConfigurationMap).localKeySet();
        doReturn(configurationMap).when(hazelcastConfigurationMap).getAll(configurationMap.keySet());

        return this;
    }

    public AppBootstrapFixtures clusterMasterNode(boolean isMaster) {
        doReturn(isMaster).when(cluster).isMaster();
        return this;
    }

    public AppBootstrapFixtures realSampleCache() {
        sampleCache = spy(new CacheFactory().newCache());
        doReturn(sampleCache).when(cacheFactory).newCache();
        return this;
    }

    public AppBootstrapFixtures realMetricResponseCache() {
        metricResponseCache = spy(new CacheFactory().newCache());
        doReturn(metricResponseCache).when(cacheFactory).newCache();
        return this;
    }


    /**
     * Initializes any fixtures that need to be applied on the {@link MetricDuct} app spy
      */
    private void initAppFixtures() {
        /**
         * Counts down the app shutdown latch after {@link MetricDuct#run()} finishes executing
         */
        doAnswer(invocation -> {
            invocation.callRealMethod();
            AppBootstrapLatches.appHasShutdown().countDown();
            return null;
        }).when(app).run();
    }


    /**
     * Real logic
     */
    public AppBootstrapFixtures realSinglePartitionConfigProvider() {
        configurationSetProvider = spy(new SinglePartitionConfigurationProvider(configurationProvider, systemStatus));
        return this;
    }

    public AppBootstrapFixtures realDistributedConfigProvider() {
        configurationSetProvider = spy(new DistributedConfigurationProvider(configurationProvider, cluster, systemStatus));
        return this;
    }

    public AppBootstrapFixtures realSystemStatus() {
        systemStatus = spy(new SystemStatusRunnable(appConfigMocks.get()));
        return this;
    }

    public AppBootstrapFixtures realShutdownHook() {
        shutdownHook = spy(new ShutdownHook());
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

    public ShutdownHook shutdownHook() {
        return shutdownHook;
    }

    public ConfigurationProvider configurationProvider() {
        return configurationProvider;
    }

    public UpdatableConfigurationSetProvider<ConfigurationWrapper> configurationSetProvider() {
        return configurationSetProvider;
    }

    public RefocusLoadProcessor refocusLoadProcessor() {
        return refocusLoadProcessor;
    }

    public AppConnector appConnector() {
        return appConnector;
    }

    public ConcurrentCacheMap<MetricResponse> metricResponseCache() {
        return metricResponseCache;
    }

    public ConcurrentCacheMap<Sample> sampleCache() {
        return sampleCache;
    }

    //
    // Utility methods
    //

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
    private <T, S> List<T> filter(S[] objects, final Class<T> cls) throws InterruptedException {
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

            // Shutdown Hook
            bind(ShutdownHook.class).toInstance(shutdownHook);

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


    /**
     * Mocks general behavior defined by AppConfig
     */
    public class AppConfigMocks {
        @Mock AppConfig appConfig;

        @Mock
        AppConfig.Global global;

        @Mock
        AppConfig.Alert alert;

        @Mock
        AppConfig.Hazelcast hazelcast;

        Map<String, Double> thresholds = new HashMap<>();

        AppConfigMocks() {
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

        public AppConfigMocks connectorsPath(String path) {
            doReturn(path).when(global).connectorsPath();
            return this;
        }

        public AppConfigMocks configurationsPath(String path) {
            doReturn(path).when(global).configurationsPath();
            return this;
        }

        public AppConfig get() {
            return appConfig;
        }
    }


    /**
     * Mocks {@link Configuration} related behavior; allows running the ETL cycle with pre-defined configurations
     */
    public class ConfigurationMocks {
        @Mock
        Configuration configuration;

        @Mock
        Argus argusExtract;

        @Mock
        Refocus refocusExtract;

        @Mock
        com.salesforce.pyplyn.duct.etl.load.refocus.Refocus refocusLoad;

        ConfigurationMocks() {
            MockitoAnnotations.initMocks(this);

            // configure defaults
            this.enabledConfiguration()
                .lastDatapoint()
                .refocusLoad();
        }

        public ConfigurationMocks enabledConfiguration() {
            doReturn(Boolean.TRUE).when(configuration).isEnabled();
            return this;
        }

        public ConfigurationMocks argusExtract(Double defaultValue) {
            doReturn(new Argus[]{argusExtract}).when(configuration).extract();
            doReturn(MOCK_CONNECTOR_NAME).when(argusExtract).endpoint();
            doReturn(defaultValue).when(argusExtract).defaultValue();
            doReturn("argus-metric").when(argusExtract).name();
            return this;
        }

        public ConfigurationMocks refocusExtract(Double defaultValue) {
            doReturn(new Refocus[]{refocusExtract}).when(configuration).extract();
            doReturn(MOCK_CONNECTOR_NAME).when(refocusExtract).endpoint();
            doReturn(defaultValue).when(refocusExtract).defaultValue();
            doReturn("subject").when(refocusExtract).subject();
            doReturn("subject").when(refocusExtract).actualSubject();
            doReturn("aspect").when(refocusExtract).aspect();
            return this;
        }

        public ConfigurationMocks shouldCache(int cacheFor) {
            doReturn(cacheFor).when(refocusExtract).cacheMillis();
            doReturn(cacheFor).when(argusExtract).cacheMillis();
            return this;
        }

        public ConfigurationMocks refocusLoad() {
            doReturn(new com.salesforce.pyplyn.duct.etl.load.refocus.Refocus[]{refocusLoad}).when(configuration).load();
            doReturn(MOCK_CONNECTOR_NAME).when(refocusLoad).endpoint();
            return this;
        }

        public ConfigurationMocks lastDatapoint() {
            doReturn(new Transform[]{new LastDatapoint()}).when(configuration).transform();
            return this;
        }

        public ConfigurationMocks withTransforms(Transform[] transforms) {
            doReturn(transforms).when(configuration).transform();
            return this;
        }

        public Configuration get() {
            return configuration;
        }

        /**
         * Builds a {@link ConfigurationWrapper} based on the configuration fixture defined with this class' methods
         */
        public ConfigurationWrapper buildWrapper(Long lastRun) {
            return spy(new ConfigurationWrapper(configuration, lastRun));
        }
    }
}