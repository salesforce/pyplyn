/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Byte array deserializer
 * <p/>
 * Avoids creating an immutable String; useful for handling sensitive bytes, such as passwords
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class SensitiveByteArrayDeserializer extends JsonDeserializer<byte[]> {
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
