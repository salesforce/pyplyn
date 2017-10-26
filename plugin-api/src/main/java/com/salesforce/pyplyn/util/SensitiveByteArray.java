/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Jackson String to byte[] serializer/deserializer implementations
 * <p/>
 * Avoids creating immutable (and interned) {@link String} objects; useful for handling sensitive bytes, such as passwords
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class SensitiveByteArray {
    /**
     * byte[] to String serializer
     *
     * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
     * @since 3.0
     */
    public static class Serializer extends JsonSerializer<byte[]> {
        /**
         * Jackson serializer implementation
         *
         * @throws IOException if the bytes could not be serialized/written
         */
        @Override
        public void serialize(byte[] bytes, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
            jsonGenerator.writeUTF8String(bytes, 0, bytes.length);
        }
    }

    /**
     * String to byte[] deserializer
     *
     * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
     * @since 10.0.0
     */
    public static class Deserializer extends JsonDeserializer<byte[]> {
        /**
         * Jackson byte deserializer implementation
         *
         * @throws IOException if the bytes could not be serialized/written
         */
        @Override
        public byte[] deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            return jsonParser.getValueAsString().getBytes(Charset.defaultCharset());
        }
    }

}
