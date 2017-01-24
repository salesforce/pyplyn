/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.Charset;

import static org.mockito.Mockito.verify;

/**
 * Password serializer test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class SensitiveByteArraySerializerTest {
    @Mock
    SerializerProvider provider;

    @Mock
    JsonGenerator generator;

    SensitiveByteArraySerializer serializer;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        serializer = new SensitiveByteArraySerializer();
    }

    @Test
    public void testSerialize() throws Exception {
        // ARRANGE
        byte[] bytes = "bytes".getBytes(Charset.defaultCharset());

        // ACT
        serializer.serialize(bytes, generator, provider);

        // ASSERT

        // tests the fact that the password is serialized as byte array and not converted to an immutable String object
        verify(generator).writeUTF8String(bytes, 0, bytes.length);
    }
}