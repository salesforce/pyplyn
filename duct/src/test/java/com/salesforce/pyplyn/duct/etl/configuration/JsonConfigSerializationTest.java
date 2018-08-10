/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.configuration;

import static com.salesforce.pyplyn.duct.connector.ConnectorTest.ONE_CONNECTOR;
import static com.salesforce.pyplyn.util.SerializationHelper.loadResourceInsecure;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.salesforce.pyplyn.configuration.Configuration;
import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.ImmutableConfiguration;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.etl.extract.argus.ImmutableArgus;
import com.salesforce.pyplyn.duct.etl.extract.refocus.ImmutableRefocus;
import com.salesforce.pyplyn.duct.etl.transform.standard.ImmutableHighestValue;
import com.salesforce.pyplyn.duct.etl.transform.standard.ImmutableInfoStatus;
import com.salesforce.pyplyn.duct.etl.transform.standard.ImmutableLastDatapoint;
import com.salesforce.pyplyn.duct.etl.transform.standard.ImmutableMetadata;
import com.salesforce.pyplyn.duct.etl.transform.standard.ImmutableSaveMetricMetadata;
import com.salesforce.pyplyn.duct.etl.transform.standard.ImmutableThreshold;
import com.salesforce.pyplyn.duct.etl.transform.standard.ImmutableThresholdMetForDuration;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.PollingTransform;
import com.salesforce.pyplyn.model.ThresholdType;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.Transmutation;

/**
 * Integration test for testing that configurations and connectors can be deserialized
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class JsonConfigSerializationTest {
    public static final String ONE_CONFIGURATION = "/configuration.example.json";
    private AppBootstrapFixtures fixtures;
    private ObjectMapper mapper;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();

        Injector injector = fixtures.initializeFixtures().injector();
        mapper = injector.getProvider(ObjectMapper.class).get();
    }

    @Test
    public void testDeserializeConfiguration() throws Exception {
        // ACT
        Configuration[] configuration = mapper.readValue(loadResourceInsecure(ONE_CONFIGURATION), Configuration[].class);

        // ASSERT
        assertThat(configuration, notNullValue());
    }

    @Test
    public void testSerializeConfiguration() throws Exception {
        // ARRANGE
        List<Extract> extracts = Arrays.asList(
                ImmutableArgus.of("endpoint", "expression", "name", 100, null),
                ImmutableRefocus.of("endpoint", "subject", "actualSubject", "aspect", 100, null)
        );

        List<Transform> transforms = Arrays.asList(
                ImmutableHighestValue.of(null, null),
                ImmutableThreshold.of("name", null, null, null, ThresholdType.GREATER_THAN),
                ImmutableInfoStatus.builder().build(),
                ImmutableLastDatapoint.builder().build(),
                ImmutableMetadata.of(singletonMap("key", "value")),
                ImmutableSaveMetricMetadata.builder().build(),
                ImmutableThresholdMetForDuration.of(100d, ThresholdType.GREATER_THAN, 1000L, 2000L, 3000L),
                new PollingTransformImpl("endpoint")
        );

        List<Load> loads = Arrays.asList(
                com.salesforce.pyplyn.duct.etl.load.refocus.ImmutableRefocus.of("endpoint", "subject", "aspect", null, null, emptyList())
        );

        // build a configuration object
        Configuration configuration = ImmutableConfiguration.of(100, extracts, transforms, loads, false);

        // register deserialization type
        mapper.registerSubtypes(PollingTransformImpl.class);


        // ACT
        // test that all known subtypes can be serialized and then de-serialized back into the same configuration value
        String serializedConfiguration = mapper.writeValueAsString(configuration);
        Configuration deserializedConfiguration = mapper.readValue(serializedConfiguration, Configuration.class);


        // ASSERT
        assertThat(configuration, equalTo(deserializedConfiguration));
    }

    @Test
    public void testDeserializeConnector() throws Exception {
        // ACT
        Connector[] appConnectors = mapper.readValue(loadResourceInsecure(ONE_CONNECTOR), Connector[].class);

        // ASSERT
        assertThat(appConnectors, notNullValue());
    }

    /**
     * Non generated (Immutables) sub-class, to verify that we can also de/serialize this type of implementation
     */
    @JsonTypeName("PollingTransformImpl")
    private static class PollingTransformImpl extends PollingTransform<String> {
        private static final long serialVersionUID = 4084771288222543708L;

        @JsonProperty
        private final String endpoint;

        @JsonCreator
        private PollingTransformImpl(@JsonProperty("endpoint") String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public String endpoint() {
            return endpoint;
        }

        @Nullable
        @Override
        public Double threshold() {
            return null;
        }

        @Nullable
        @Override
        public ThresholdType type() {
            return null;
        }

        @Override
        public String sendRequest(List<List<Transmutation>> input) {
            return null;
        }

        @Override
        public List<List<Transmutation>> retrieveResult(String request) {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PollingTransformImpl that = (PollingTransformImpl) o;

            return endpoint.equals(that.endpoint);
        }

        @Override
        public int hashCode() {
            return endpoint.hashCode();
        }
    }
}
