/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Byte array serializer
 * <p/>
 * Avoids creating an immutable String; useful for handling sensitive bytes, such as passwords
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class SensitiveByteArraySerializer extends JsonSerializer<byte[]> {

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
