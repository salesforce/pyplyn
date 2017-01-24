/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.providers.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.duct.etl.ETLConfigDeserializer;
import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.Transform;

import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Provides a configured Jackson object mapper
 * <p/>
 * <p/>The provided object is annotated as Singleton to avoid unnecessarily instantiating Jackson mappers with the same configuration.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ObjectMapperProvider implements Provider<ObjectMapper> {
    private final ETLConfigDeserializer<Extract> extractDeserializer;
    private final ETLConfigDeserializer<Transform> transformDeserializer;
    private final ETLConfigDeserializer<Load> loadDeserializer;


    /**
     * Initializes the provider
     * <p/>
     * <p/>Accepts the ETL deserialization formats defined with
     *   {@link com.salesforce.pyplyn.util.MultibinderFactory#extractDatasources(Binder)},
     *   {@link com.salesforce.pyplyn.util.MultibinderFactory#transformFunctions(Binder)},
     *   and {@link com.salesforce.pyplyn.util.MultibinderFactory#loadDestinations(Binder)}
     */
    @Inject
    public ObjectMapperProvider(
            Set<Class<? extends Extract>> extractFormats,
            Set<Class<? extends Transform>> transformFormats,
            Set<Class<? extends Load>> loadFormats) {
        extractDeserializer = buildExtractDeserializer(extractFormats);
        transformDeserializer = buildTransformDeserializer(transformFormats);
        loadDeserializer = buildLoadDeserializer(loadFormats);
    }

    /**
     * Uses the initialized modules and predefined settings to construct a fully configured {@link ObjectMapper}
     */
    @Singleton
    @Override
    public ObjectMapper get() {
        // define Jackson module, add deserializers
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Extract.class, extractDeserializer);
        module.addDeserializer(Transform.class, transformDeserializer);
        module.addDeserializer(Load.class, loadDeserializer);

        // create an ObjectMapper, configure it and return
        return new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .setSerializationInclusion(NON_NULL)
                .registerModule(module);
    }

    /**
     * Builds the {@link Extract} datasource deserializer
     * <p/>
     * <p/>Note: The <b>typeField</b> specified as the second parameter to {@link ETLConfigDeserializer#ETLConfigDeserializer(Class, String)}
     *   should match what is specified in the Jackson {@link com.fasterxml.jackson.annotation.JsonIgnoreProperties#value()} annotation of the <b>cls</b> interface
     *   specified as the first parameter of ETLConfigDeserializer's constructor.
     */
    private static ETLConfigDeserializer<Extract> buildExtractDeserializer(Set<Class<? extends Extract>> formats) {
        ETLConfigDeserializer<Extract> deserializer = new ETLConfigDeserializer<>(Extract.class, "format");
        deserializer.registerTypes(formats);
        return deserializer;
    }

    /**
     * Builds the {@link Transform} function deserializer
     * <p/>
     * <p/>Note: The <b>typeField</b> specified as the second parameter to {@link ETLConfigDeserializer#ETLConfigDeserializer(Class, String)}
     *   should match what is specified in the Jackson {@link com.fasterxml.jackson.annotation.JsonIgnoreProperties#value()} annotation of the <b>cls</b> interface
     *   specified as the first parameter of ETLConfigDeserializer's constructor.
     */
    private static ETLConfigDeserializer<Transform> buildTransformDeserializer(Set<Class<? extends Transform>> formats) {
        ETLConfigDeserializer<Transform> deserializer = new ETLConfigDeserializer<>(Transform.class, "name");
        deserializer.registerTypes(formats);
        return deserializer;
    }

    /**
     * Builds the {@link Load} destination deserializer
     * <p/>
     * <p/>Note: The <b>typeField</b> specified as the second parameter to {@link ETLConfigDeserializer#ETLConfigDeserializer(Class, String)}
     *   should match what is specified in the Jackson {@link com.fasterxml.jackson.annotation.JsonIgnoreProperties#value()} annotation of the <b>cls</b> interface
     *   specified as the first parameter of ETLConfigDeserializer's constructor.
     */
    private static ETLConfigDeserializer<Load> buildLoadDeserializer(Set<Class<? extends Load>> formats) {
        ETLConfigDeserializer<Load> deserializer = new ETLConfigDeserializer<>(Load.class, "format");
        deserializer.registerTypes(formats);
        return deserializer;
    }
}
