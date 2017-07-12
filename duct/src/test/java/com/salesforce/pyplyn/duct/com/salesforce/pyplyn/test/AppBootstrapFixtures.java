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
import com.google.inject.*;
import com.hazelcast.core.IMap;
import com.salesforce.argus.ArgusClient;
import com.salesforce.argus.model.MetricResponse;
import com.salesforce.pyplyn.cache.CacheFactory;
import com.salesforce.pyplyn.cache.ConcurrentCacheMap;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.duct.app.AppBootstrap;
import com.salesforce.pyplyn.duct.app.BootstrapException;
import com.salesforce.pyplyn.duct.app.DuctMain;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.duct.cluster.Cluster;
import com.salesforce.pyplyn.duct.connector.AppConnector;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationLoader;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationUpdateManager;
import com.salesforce.pyplyn.duct.etl.configuration.TaskManager;
import com.salesforce.pyplyn.duct.etl.extract.argus.Argus;
import com.salesforce.pyplyn.duct.etl.extract.argus.ArgusExtractProcessor;
import com.salesforce.pyplyn.duct.etl.extract.refocus.Refocus;
import com.salesforce.pyplyn.duct.etl.extract.refocus.RefocusExtractProcessor;
import com.salesforce.pyplyn.duct.etl.load.refocus.RefocusLoadProcessor;
import com.salesforce.pyplyn.duct.etl.transform.standard.LastDatapoint;
import com.salesforce.pyplyn.duct.systemstatus.ConsoleOutputConsumer;
import com.salesforce.pyplyn.duct.systemstatus.SystemStatusRunnable;
import com.salesforce.pyplyn.model.*;
import com.salesforce.pyplyn.processor.ExtractProcessor;
import com.salesforce.pyplyn.processor.LoadProcessor;
import com.salesforce.pyplyn.status.SystemStatus;
import com.salesforce.pyplyn.status.SystemStatusConsumer;
import com.salesforce.pyplyn.util.MultibinderFactory;
import com.salesforce.refocus.RefocusClient;
import com.salesforce.refocus.model.Sample;
import io.reactivex.Flowable;
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
    private AbstractConnector connector;

    @Mock
    private AppConnector appConnector;

    @Mock
    private SystemStatusRunnable systemStatus;

    @Mock
    private Meter systemStatusMeter;

    @Mock
    private Timer systemStatusTimer;

    @Mock
    private ArgusClient argusClient;

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
    private ConfigurationLoader configurationLoader;

    @Mock
    private Cluster cluster;

    @Mock
    private TaskManager<Configuration> taskManager;

    @Mock
    private ConfigurationUpdateManager configurationManager;

    @Mock
    private IMap<Configuration, Configuration> hazelcastConfigurationMap;

    private AppConfigMocks appConfigMocks;
    private List<AbstractConnector> connectors = new ArrayList<>();
    private Set<ExtractProcessor<? extends Extract>> extractProcessors = new HashSet<>();
    private Set<LoadProcessor<? extends Load>> loadProcessors = new HashSet<>();
    private ShutdownHook shutdownHook;

    private Injector injector;

    /**
     * If enabled, will initialize all latches in {@link AppBootstrapLatches} to wait, otherwise
     *  they are disabled by default and the program flows without any limits
     */
    private boolean withLatches = false;


    /**
     * Initializes the fixtures and default
     */
    // TODO: Rewrite fixtures as extension of AppBootstrap
    public AppBootstrapFixtures() {
        MockitoAnnotations.initMocks(this);

        // init other mocks and fixtures
        appConfigMocks = new AppConfigMocks();
        shutdownHook = spy(new ShutdownHook());

        // App connector
        connectors.add(connector);
        doReturn(connector).when(appConnector).get(MOCK_CONNECTOR_NAME);
        doReturn("http://localhost/").when(connector).endpoint();
        doReturn(30000L).when(connector).connectTimeout();
        doReturn(30000L).when(connector).readTimeout();
        doReturn(30000L).when(connector).writeTimeout();

        // System status delegates
        doReturn(systemStatusMeter).when(systemStatus).meter(any(), any());
        doReturn(systemStatusTimer).when(systemStatus).timer(any(), any());

        // Transformation results
        doReturn(transformationResultMetadata).when(transformationResult).metadata();
        doReturn(1L).when(transformationResult).value();

        // Cache
        doReturn(metricResponseCache).when(cacheFactory).newCache();
        doReturn(sampleCache).when(cacheFactory).newCache();

        // Cluster
        doReturn(false).when(cluster).isEnabled();

        // Extract Processors
        doReturn(Flowable.empty()).when(argusExtractProcessor).processAsync(any());
        doReturn(Flowable.empty()).when(argusExtractProcessor).executeAsync(any());

        doReturn(Flowable.empty()).when(refocusExtractProcessor).processAsync(any());
        doReturn(Flowable.empty()).when(refocusExtractProcessor).executeAsync(any());

        // Load Processors
        doReturn(Flowable.just(Collections.singletonList(true))).when(refocusLoadProcessor).processAsync(any(), any());
        doReturn(Flowable.just(Collections.singletonList(true))).when(refocusLoadProcessor).executeAsync(any(), any());
    }


    /**
     * Initializes the injector
     */
    public AppBootstrapFixtures initializeFixtures() {
        // init latches
        AppBootstrapLatches.init(withLatches);

        // add mocked dependencies and configuration model deserialization modules
        List<Module> modules = new ArrayList<>();
        modules.add(new MockedDependenciesModule());
        modules.addAll(AppBootstrap.modelDeserializationModules());

        // creates the injector and the MetricDuct app object
        injector = Guice.createInjector(modules);
        hookAfterInitialize();

        return this;
    }

    /**
     * Initializes any fixtures that need to be applied on the {@link DuctMain} app spy
     */
    private void hookAfterInitialize() {
        // required to run the tasks
        initConfigurationManager();
        if (withLatches) {
            applyExtractAndLoadProcessorLatches();
        }

        /**
         * Add all processors to previously defined task observer
         */
        extractProcessors.addAll(injector.getInstance(new Key<Set<ExtractProcessor<? extends Extract>>>(){}));
        loadProcessors.addAll(injector.getInstance(new Key<Set<LoadProcessor<? extends Load>>>(){}));
    }

    /**
     * Apply latches to all processors
     * Note: this will cause issues if not using real extract/load processors!
     */
    private void applyExtractAndLoadProcessorLatches() {
        // Argus extract processor

        // we are replacing the default filtering logic, since the passed object will be a mock
        doAnswer(invocation -> {
            AppBootstrapLatches.beforeExtractProcessorStarts().countDown();
            AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().await();

            return filter(invocation.getArguments(), Argus.class);
        }).when(argusExtractProcessor).filter(any());

        // note when extract processing has completed
        doAnswer(invocation -> {
            try {
                return invocation.callRealMethod();

            } finally {
                AppBootstrapLatches.holdOffUntilExtractProcessorFinishes().countDown();
            }
        }).when(argusExtractProcessor).process(any());


        // Refocus extract processor

        // we are replacing the default filtering logic, since the passed object will be a mock
        doAnswer(invocation -> {
            AppBootstrapLatches.beforeExtractProcessorStarts().countDown();
            AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().await();

            return filter(invocation.getArguments(), Refocus.class);
        }).when(refocusExtractProcessor).filter(any());

        // note when extract processing has completed
        doAnswer(invocation -> {
            try {
                return invocation.callRealMethod();

            } finally {
                AppBootstrapLatches.holdOffUntilExtractProcessorFinishes().countDown();
            }
        }).when(refocusExtractProcessor).process(any());


        // Refocus load processor

        // we are replacing the default filtering logic, since the passed object will be a mock
        doAnswer(invocation -> {
            AppBootstrapLatches.beforeLoadProcessorStarts().countDown();
            AppBootstrapLatches.holdOffBeforeLoadProcessorStarts().await();

            return filter(invocation.getArguments(), com.salesforce.pyplyn.duct.etl.load.refocus.Refocus.class);
        }).when(refocusLoadProcessor).filter(any());

        // note when load has completed
        doAnswer(invocation -> {
            try {
                return invocation.callRealMethod();

            } finally {
                AppBootstrapLatches.holdOffUntilLoadProcessorFinishes().countDown();
            }
        }).when(refocusLoadProcessor).process(any(), any());
    }


    /**
     * Shorthand for waiting until all processing is done (useful in runOnce mode)
     *   and then shutting down the {@link ConfigurationUpdateManager}
     */
    public void awaitUntilAllTasksHaveBeenProcessed(boolean shutdown) throws InterruptedException {
        configurationManager.awaitUntilConfigured();
        taskManager.awaitUntilFinished();
        if (shutdown) {
            shutdownHook.shutdown();
            shutdownHook.awaitShutdown();
        }
    }


    /*
     *  Mock logic
     */

    private void initTaskManager() {
        taskManager = spy(new TaskManagerWithLatches<>(appConfigMocks().appConfig, extractProcessors, loadProcessors, shutdownHook)).initLatches();
    }

    public void stopTaskManager() {
        ((TaskManagerWithLatches)taskManager()).notifyCompleted();
    }

    public AppBootstrapFixtures initConfigurationManager() {
        initTaskManager();
        configurationManager = spy(new ConfigurationUpdateManager(configurationLoader, taskManager, cluster, shutdownHook));
        configurationManager.initialize();
        return this;
    }

    public AppBootstrapFixtures clusterMasterNode() {
        initTaskManager();
        doReturn(true).when(cluster).isEnabled();
        doReturn(true).when(cluster).isMaster();
        return this;
    }

    public AppBootstrapFixtures clusterSlaveNode() {
        initTaskManager();
        doReturn(true).when(cluster).isEnabled();
        doReturn(false).when(cluster).isMaster();
        return this;
    }

    /**
     * DON'T FORGET to wrap the test code in a try-finally block and call {@link AppBootstrapLatches#release()} after the test code is done
     *   This method only works with real extract/load processors!
     */
    public AppBootstrapFixtures enableLatches() {
        withLatches = true;
        return this;
    }

    public AppBootstrapFixtures oneArgusToRefocusConfiguration() {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().argusExtract(null).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures oneArgusToRefocusConfigurationWithRepeatInterval(long millis) {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().repeatIntervalMillis(millis).argusExtract(null).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures oneArgusToRefocusConfigurationWithCache() {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().shouldCache(60000).argusExtract(null).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures oneArgusToRefocusConfigurationWithCacheAndRepeatInterval(long millis) {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().repeatIntervalMillis(millis).shouldCache(60000).argusExtract(null).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures oneArgusToRefocusConfigurationWithDefaultValueAndRepeatInterval(Double defaultValue, long millis) {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().argusExtract(defaultValue).repeatIntervalMillis(millis).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures argusToRefocusConfigurationWithTransformsAndRepeatInterval(Transform[] transforms, long millis) {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().argusExtract(null).withTransforms(transforms).repeatIntervalMillis(millis).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures oneRefocusToRefocusConfiguration() {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().refocusExtract(null).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures oneRefocusToRefocusConfigurationWithRepeatInterval(long millis) {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().repeatIntervalMillis(millis).refocusExtract(null).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures oneRefocusToRefocusConfigurationWithCache() {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().shouldCache(60000).refocusExtract(null).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures oneRefocusToRefocusConfigurationWithDefaultValue(Double defaultValue) {
        doReturn(new HashSet<>(Collections.singleton(new ConfigurationMocks().refocusExtract(defaultValue).build())))
                .when(configurationLoader)
                .load();
        return this;
    }

    public AppBootstrapFixtures returnMockedTransformationResultFromAllExtractProcessors() {
        doReturn(Collections.singletonList(Collections.singletonList(transformationResult))).when(argusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Argus.class)).when(argusExtractProcessor).filter(any());
        doCallRealMethod().when(argusExtractProcessor).execute(any());
        doCallRealMethod().when(argusExtractProcessor).processAsync(any());
        doCallRealMethod().when(argusExtractProcessor).executeAsync(any());
        doReturn(argusClient).when(argusExtractProcessor).client(any());
        doReturn(metricResponseCache).when(argusExtractProcessor).cache(any());


        doReturn(Collections.singletonList(Collections.singletonList(transformationResult))).when(refocusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Refocus.class)).when(refocusExtractProcessor).filter(any());
        doCallRealMethod().when(refocusExtractProcessor).execute(any());
        doCallRealMethod().when(refocusExtractProcessor).processAsync(any());
        doCallRealMethod().when(refocusExtractProcessor).executeAsync(any());
        doReturn(refocusClient).when(refocusExtractProcessor).client(any());
        doReturn(sampleCache).when(refocusExtractProcessor).cache(any());

        return this;
    }

    public AppBootstrapFixtures returnTransformationResultFromAllExtractProcessors(List<List<TransformationResult>> results) {
        doReturn(results).when(argusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Argus.class)).when(argusExtractProcessor).filter(any());
        doCallRealMethod().when(argusExtractProcessor).execute(any());
        doCallRealMethod().when(argusExtractProcessor).processAsync(any());
        doCallRealMethod().when(argusExtractProcessor).executeAsync(any());
        doReturn(argusClient).when(argusExtractProcessor).client(any());
        doReturn(metricResponseCache).when(argusExtractProcessor).cache(any());

        doReturn(results).when(refocusExtractProcessor).process(any());
        doAnswer(invocation -> filter(invocation.getArguments(), Refocus.class)).when(refocusExtractProcessor).filter(any());
        doCallRealMethod().when(refocusExtractProcessor).execute(any());
        doCallRealMethod().when(refocusExtractProcessor).processAsync(any());
        doCallRealMethod().when(refocusExtractProcessor).executeAsync(any());
        doReturn(refocusClient).when(refocusExtractProcessor).client(any());
        doReturn(sampleCache).when(refocusExtractProcessor).cache(any());

        return this;
    }

    public AppBootstrapFixtures callRealArgusExtractProcessor() {
        // we need to reinitialize the object to provide access to the real failed/succeeded (protected) methods
        argusExtractProcessor = spy(new ArgusExtractProcessor(appConnector, cacheFactory, shutdownHook));
        doReturn(argusClient).when(argusExtractProcessor).client(any());
        doReturn(metricResponseCache).when(argusExtractProcessor).cache(any());
        return this;
    }

    public AppBootstrapFixtures argusClientCanNotAuth() throws UnauthorizedException {
        doReturn(false).when(argusClient).isAuthenticated();
        doThrow(UnauthorizedException.class).when(argusClient).auth();
        doCallRealMethod().when(argusExtractProcessor).client(any());
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
        refocusExtractProcessor = spy(new RefocusExtractProcessor(appConnector, cacheFactory, shutdownHook));
        doReturn(refocusClient).when(refocusExtractProcessor).client(any());
        doReturn(sampleCache).when(refocusExtractProcessor).cache(any());
        return this;
    }

    public AppBootstrapFixtures refocusClientCanNotAuth() throws UnauthorizedException {
        doReturn(false).when(refocusClient).isAuthenticated();
        doThrow(UnauthorizedException.class).when(refocusClient).auth();
        doCallRealMethod().when(refocusExtractProcessor).client(any());
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
        refocusLoadProcessor = spy(new RefocusLoadProcessor(appConnector, shutdownHook));
        doReturn(refocusClient).when(refocusLoadProcessor).client(any());
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

    public AppBootstrapFixtures configurationProviderReturns(Configuration ... configurations) {
        doReturn(new HashSet<>(Arrays.asList(configurations))).when(configurationLoader).load();
        return this;
    }

    public AppBootstrapFixtures configurationProviderThrowsException() {
        doThrow(BootstrapException.class).when(configurationLoader).load();
        return this;
    }

    public AppBootstrapFixtures clusterReturns(Configuration ... configurations) {
        doReturn(hazelcastConfigurationMap).when(cluster).distributedMap(any());

        Map<Configuration, Configuration> configurationMap =
                Stream.of(configurations).collect(Collectors.toMap(Function.identity(), Function.identity()));

        doReturn(configurationMap.keySet()).when(hazelcastConfigurationMap).localKeySet();
        doReturn(configurationMap).when(hazelcastConfigurationMap).getAll(configurationMap.keySet());

        return this;
    }

    public AppBootstrapFixtures realSystemStatus() {
        systemStatus = spy(new SystemStatusRunnable(appConfigMocks.get()));
        return this;
    }



    /**
     * Getters
     */

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

    public ConfigurationUpdateManager configurationManager() {
        return configurationManager;
    }

    public ConfigurationLoader configurationLoader() {
        return configurationLoader;
    }

    public TaskManager<Configuration> taskManager() {
        return taskManager;
    }

    public RefocusLoadProcessor refocusLoadProcessor() {
        return refocusLoadProcessor;
    }

    public AppConnector appConnector() {
        return appConnector;
    }

    public CacheFactory cacheFactory() {
        return cacheFactory;
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
     */
    private class MockedDependenciesModule extends AbstractModule {
        @Override
        protected void configure() {
            // app configuration
            bind(AppConfig.class).toInstance(appConfigMocks.get());

            // app connectors
            bind(AppConnector.class).toInstance(appConnector);
            MultibinderFactory.appConnectors(binder()).addBinding().toInstance(connectors);

            // System Status
            bind(SystemStatus.class).toInstance(systemStatus);
            MultibinderFactory.statusConsumers(binder()).addBinding().toInstance(new ConsoleOutputConsumer());
            MultibinderFactory.statusConsumers(binder()).addBinding().toInstance(statusConsumer);

            // Shutdown Hook
            bind(ShutdownHook.class).toInstance(shutdownHook);

            // ArgusClient
            bind(new TypeLiteral<Class<ArgusClient>>() {}).toInstance(ArgusClient.class);

            // RefocusClient
            bind(new TypeLiteral<Class<RefocusClient>>() {}).toInstance(RefocusClient.class);

            // Argus Extract
            MultibinderFactory.extractProcessors(binder()).addBinding().toInstance(argusExtractProcessor);

            // Refocus Extract
            MultibinderFactory.extractProcessors(binder()).addBinding().toInstance(refocusExtractProcessor);

            // Refocus Load
            MultibinderFactory.loadProcessors(binder()).addBinding().toInstance(refocusLoadProcessor);

            // Configurations
            bind(Cluster.class).toInstance(cluster);
            bind(ConfigurationLoader.class).toInstance(configurationLoader);
            bind(new TypeLiteral<TaskManager<Configuration>>(){}).toInstance(taskManager);
            bind(new TypeLiteral<Set<Configuration>>() {}).toProvider(configurationManager);
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
            // only run app once
            doReturn(-1L).when(global).minRepeatIntervalMillis();
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
    public static class ConfigurationMocks {
        Boolean isEnabled = false;
        Long repeatIntervalMillis = 10000L;
        List<Extract> extracts = new ArrayList<>();
        List<Transform> transform = new ArrayList<>();
        List<Load> load = new ArrayList<>();

        Integer cacheMillis = null;

        public ConfigurationMocks() {
            MockitoAnnotations.initMocks(this);

            // configure defaults
            this.enabledConfiguration()
                .lastDatapoint()
                .refocusLoad();
        }

        public ConfigurationMocks repeatIntervalMillis(long repeatIntervalMillis) {
            this.repeatIntervalMillis = repeatIntervalMillis;
            return this;
        }

        public ConfigurationMocks enabledConfiguration() {
            isEnabled = Boolean.TRUE;
            return this;
        }

        public ConfigurationMocks shouldCache(int cacheFor) {
            cacheMillis = cacheFor;
            return this;
        }

        public ConfigurationMocks argusExtract(Double defaultValue) {
            extracts.add(new Argus(MOCK_CONNECTOR_NAME, "expression", "argus-metric", cacheMillis, defaultValue));
            return this;
        }

        public ConfigurationMocks refocusExtract(Double defaultValue) {
            extracts.add(new Refocus(MOCK_CONNECTOR_NAME, "subject", "subject", "aspect", cacheMillis, defaultValue));
            return this;
        }

        public ConfigurationMocks refocusLoad() {
            load.add(new com.salesforce.pyplyn.duct.etl.load.refocus.Refocus(MOCK_CONNECTOR_NAME, "subject", "aspect", null, null, null));
            return this;
        }

        public ConfigurationMocks lastDatapoint() {
            transform.add(new LastDatapoint());
            return this;
        }

        public ConfigurationMocks withTransforms(Transform[] transforms) {
            transform.addAll(Arrays.asList(transforms));
            return this;
        }

        /**
         * Builds a {@link Configuration} based on the configuration fixture defined with this class' methods
         */
        public Configuration build() {
            return new Configuration(repeatIntervalMillis, extracts.toArray(new Extract[0]),
                    transform.toArray(new Transform[0]),
                    load.toArray(new Load[0]), !isEnabled);
        }
    }

    /**
     * Interjects a latch to detect when all tasks have been processed
     */
    private static class TaskManagerWithLatches<T extends Configuration> extends TaskManager<T> {
        public TaskManagerWithLatches(AppConfig config, Set<ExtractProcessor<? extends Extract>> extractProcessors, Set<LoadProcessor<? extends Load>> loadProcessors, ShutdownHook shutdownHook) {
            super(config, extractProcessors, loadProcessors, shutdownHook);
        }

        private TaskManager<T> initLatches() {
            doAnswer(invocation -> {
                invocation.callRealMethod();
                AppBootstrapLatches.appHasShutdown().countDown();
                return null;
            }).when(this).hookAfterTaskProcessed();
            return this;
        }

        /**
         * Expose the completion method in testing
         */
        @Override
        public void notifyCompleted() {
            super.notifyCompleted();
        }
    }
}