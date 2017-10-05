package com.salesforce.pyplyn.duct.app;

import static java.util.Collections.singletonList;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.ImmutableConnector;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.duct.appconfig.ImmutableAppConfig;

/**
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class DuctMainTest {
    private final static ObjectMapper mapper = new ObjectMapper();

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        DuctMain.setProgramName("DuctMainTest");
    }

    @Test(timeOut = 5000L)
    public void testAppRuns() throws Exception {
        // ARRANGE
        Path configurations = Files.createTempDirectory("test-configurations");
        Path connectors = createConnector();
        Path appConfig = createAppConfig(configurations, connectors);

        // ACT
        // run program
        DuctMain.main("--config", appConfig.toAbsolutePath().toString());

        // ASSERT
        // there are no specific assertions to be run, other than the fact that the program should complete
        //   right away, without any exceptions
    }


    /**
     * Creates a connectors file
     */
    public static Path createConnector() throws IOException {
        Path connectorFile = Files.createTempFile("test-connector", ".json");
        Connector connector = ImmutableConnector.builder()
                .id("connector")
                .endpoint("http://localhost/")
                .password("password".getBytes(Charset.defaultCharset()))
                .build();
        mapper.writeValue(connectorFile.toFile(), singletonList(connector));
        return connectorFile;
    }

    /**
     * Creates a simple {@link AppConfig} object
     */
    public static Path createAppConfig(Path configurations, Path connectors) throws IOException {
        Path configFile = Files.createTempFile("test-config", ".json");
        AppConfig.Global global = ImmutableAppConfig.Global.builder()
                .configurationsPath(configurations.toAbsolutePath().toString())
                .connectorsPath(connectors.toAbsolutePath().toString())
                .runOnce(true)
                .updateConfigurationIntervalMillis(100000L)
                .build();
        AppConfig config = ImmutableAppConfig.builder().global(global).build();
        mapper.writeValue(configFile.toFile(), config);
        return configFile;
    }
}