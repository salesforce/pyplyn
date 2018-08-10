package com.salesforce.pyplyn.util;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.type.CollectionType;

public class RawJsonCollectionDeserializer<T> extends JsonDeserializer<List<T>> implements ContextualDeserializer {
    @Override
    public List<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        throw ctxt.instantiationException(List.class,
                getClass().getSimpleName() + "." + "deserialize(JsonParser, DeserializationContext): should not be called directly!");
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        // validate contextual type
        final JavaType contextualType = ctxt.getContextualType();
        if (isNull(contextualType)) {
            throw ctxt.instantiationException(Collection.class, "Could not identify contextual type!");
        }

        return new JsonDeserializer<Object>() {
            @Override
            public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return deserializeWithRetry(p, ctxt, contextualType.getContentType());
            }
        };
    }


    private Collection<Object> deserializeWithRetry(JsonParser p, DeserializationContext ctxt, JavaType contentType) throws IOException {
        final CollectionType collectionType = ctxt.getTypeFactory().constructCollectionType(Collection.class, contentType);

        try {
            return p.getCodec().readValue(p, collectionType);

        } catch (JsonMappingException e) {
            // attempt to read the value as string
            final String escapedString = p.getValueAsString();

            // stop here if value could not be read
            if (isNull(escapedString)) {
                throw ctxt.instantiationException(Collection.class, "Read null value when attempting to deserialize " + collectionType.toString());
            }

            // un-escape double quotes
            String unescapedString = escapedString.replaceAll("\"", "\"");

            // and attempt to parse again
            return new ObjectMapper().readValue(unescapedString, collectionType);
        }
    }
}