/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Deserializes ETL configuration models ({@link com.salesforce.pyplyn.model.Extract}, {@link com.salesforce.pyplyn.model.Transform}, {@link com.salesforce.pyplyn.model.Load}),
 *   based on their specified format
 * <p/>
 * <p/>Based on Bruce Coleman's <a href="http://programmerbruce.blogspot.co.uk/2011/05/deserialize-json-with-jackson-into.html">excellent examples</a>
 *   and <a href="https://stackoverflow.com/questions/38490969/deserialize-into-polymorphic-collection-using-jackson-without-using-jsontypeinf">this modification</a>
 * <p/>
 * @param <T> Upper type bound of the object we are deserializing into
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ETLConfigDeserializer<T> extends StdDeserializer<T> {
    private static final long serialVersionUID = 1494795072400801666L;
    private final String typeField;
    private final Map<String, Class<? extends T>> knownTypes;


    /**
     * Constructs a deserializer for the specified class object,
     *   using the field name specified in <b>typeField</b>
     *   to load the actual class name to deserialize as.
     *
     * <b>Types</b> specify the list of possible deserialization targets
     * If typeField doesn't match any classes listed in <b>types</b>,
     *   a {@link JsonMappingException} will be thrown
     */
    public ETLConfigDeserializer(Class<T> cls, String typeField, Set<Class<? extends T>> types) {
        super(cls);
        this.typeField = typeField;
        this.knownTypes = Collections.unmodifiableMap(types.stream().collect(
                Collectors.toMap(Class::getSimpleName, Function.identity())));
    }

    /**
     * Registers known types which will be used to identify known serialization formats
     *   if the method is called multiple times, it will override previously defined mappings
     */

    /**
     * Performs the deserialization logic, that identifies the target type from <b>typeField</b>
     *   and matches it against <b>knownTypes</b> for this object
     *
     * @throws JsonMappingException if <b>typeField</b> cannot be matched to any <b>knownTypes</b>
     * @throws IOException on any other issues
     */
    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
        ObjectCodec mapper = jsonParser.getCodec();
        ObjectNode rootNode = mapper.readTree(jsonParser);
        Class<? extends T> objectType = findDeserializationClass(ctx, rootNode);

        // parse as specified object type
        return mapper.treeToValue(rootNode, objectType);
    }

    /**
     * Identifies the object type to deserialize into by matching the value specified in {@link ETLConfigDeserializer#typeField}
     *   against {@link ETLConfigDeserializer#knownTypes} for this object
     */
    private Class<? extends T> findDeserializationClass(DeserializationContext ctx, ObjectNode rootNode) throws JsonMappingException {
        if (nonNull(rootNode)) {
            Iterator<Map.Entry<String, JsonNode>> elements = rootNode.fields();
            while (elements.hasNext()) {
                // retrieve the field's name and determine if it's the searched type
                Map.Entry<String, JsonNode> element = elements.next();
                String field = element.getKey();
                if (!typeField.equals(field)) {
                    continue;
                }

                // retrieve the value and continue if not a text value
                String value = element.getValue().textValue();
                if (isNull(value)) {
                    continue;
                }

                // throw exception if unable to match type
                if (!knownTypes.containsKey(value)) {
                    throw ctx.mappingException("The specified type \"%s\" is not known to the deserializer!", value);
                }

                return knownTypes.get(value);
            }
        }

        // stop here if the object's deserialization type was not specified
        throw ctx.mappingException("Could not find deserialization type. Did you specify \"%s\"?", typeField);
    }
}
