/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.Inject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class for serializing/deserializing to JSON and loading resources/files
 *   using the same Jackson {@link ObjectMapper} object (providing consistent configuration).
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public final class ObjectMapperWrapper implements SerializationHelper {

    private final ObjectMapper mapper;
    private final ObjectWriter writer;


    /**
     * Constructs a new wrapper object and configures the {@link ObjectWriter} to indent arrays on new lines
     */
    @Inject
    public ObjectMapperWrapper(ObjectMapper mapper) {
        this.mapper = mapper;

        // configure a Jackson ObjectWriter
        DefaultPrettyPrinter jsonPrinter = new DefaultPrettyPrinter();
        jsonPrinter.indentArraysWith(new DefaultIndenter());
        writer = mapper.writer(jsonPrinter);
    }

    /**
     * Serializes an object to json
     *
     * @throws JsonProcessingException if object cannot be serialized
     */
    @Override
    public String serializeJson(Object object) throws JsonProcessingException {
        return writer.writeValueAsString(object);
    }

    /**
     * Deserializes data from an input file into the specified class type
     *
     * @param filename path to filename containing the JSON data
     * @param cls Class type
     * @param <T> Object type to deserialize to
     * @throws IOException on deserialization errors
     */
    @Override
    public <T> T deserializeJsonFile(String filename, Class<T> cls) throws IOException {
        return deserializeJsonStream(SerializationHelper.loadResourceInsecure(filename), cls);
    }

    /**
     * Deserializes data from a string into the specified class type
     *
     * @param jsonData String containing the JSON data
     * @param cls Class type
     * @param <T> Object type to deserialize to
     * @throws IOException on deserialization errors
     */
    @Override
    public <T> T deserializeJsonString(String jsonData, Class<T> cls) throws IOException {
        return mapper.readValue(jsonData, cls);
    }

    /**
     * Deserializes data from an {@InputStream} into the specified class type
     *
     * @param is InputStream containing the JSON data
     * @param cls Class type
     * @param <T> Object type to deserialize to
     * @throws IOException on deserialization errors
     */
    @Override
    public <T> T deserializeJsonStream(InputStream is, Class<T> cls) throws IOException {
        return mapper.readValue(is, cls);
    }
}
