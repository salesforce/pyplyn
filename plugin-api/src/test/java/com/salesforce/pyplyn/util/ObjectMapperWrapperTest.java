/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ObjectMapperWrapperTest {
    private ObjectMapperWrapper wrapper;


    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .setSerializationInclusion(NON_NULL);
        wrapper = new ObjectMapperWrapper(mapper);
    }


    @Test
    public void testSerializeJson() throws Exception {
        // ACT
        String result = wrapper.serializeJson(Collections.singletonList("one"));

        // ASSERT
        assertThat(result, equalTo("[\n  \"one\"\n]"));
    }


    @Test
    public void testDeserializeJson() throws Exception {
        // ACT
        @SuppressWarnings("unchecked")
        List<String> result = wrapper.deserializeJsonString("[\"one\"]", List.class);

        // ASSERT
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0), equalTo("one"));
    }


    @Test
    public void testDeserializeJsonFile() throws Exception {
        // ARRANGE
        Path file = Files.createTempFile("temp", ".json");
        Files.write(file, "[\"one\"]".getBytes(Charset.defaultCharset()));

        try {
            // ACT
            @SuppressWarnings("unchecked")
            List<String> result = wrapper.deserializeJsonFile(file.toString(), List.class);

            // ASSERT
            assertThat(result.size(), equalTo(1));
            assertThat(result.get(0), equalTo("one"));

        } finally {
            // cleanup
            Files.delete(file);
        }
    }


    @Test
    public void testDeserializeJsonStream() throws Exception {
        // ARRANGE
        Path file = Files.createTempFile("temp", ".json");
        Files.write(file, "[\"one\"]".getBytes(Charset.defaultCharset()));

        try {
            // ACT
            boolean canReadFile = SerializationHelper.canRead(file.toString());

            @SuppressWarnings("unchecked")
            List<String> result = wrapper.deserializeJsonStream(Files.newInputStream(file), List.class);

            // ASSERT
            assertThat(canReadFile, is(true));
            assertThat(result.size(), equalTo(1));
            assertThat(result.get(0), equalTo("one"));

        } finally {
            // cleanup
            Files.delete(file);
        }
    }


    @Test
    public void testCannotReadFile() throws Exception {
        // ARRANGE
        Path file = Files.createTempFile("temp", ".json");
        Files.delete(file);

        // ACT
        boolean canReadFile = SerializationHelper.canRead(file.toString());

        // ASSERT
        assertThat(canReadFile, is(false));
    }
}
