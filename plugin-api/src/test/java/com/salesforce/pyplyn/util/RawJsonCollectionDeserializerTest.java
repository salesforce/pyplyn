package com.salesforce.pyplyn.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class RawJsonCollectionDeserializerTest {
    private final static ObjectMapper mapper = new ObjectMapper();
    private String input;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        input = "{\"value\": \"[\\\"one_string\\\"]\"}";
    }

    @Test(expectedExceptions = JsonMappingException.class)
    public void couldNotDeserializeJsonWithRawJsonCollection() throws Exception {
        // ACT
        mapper.readValue(input, Json.class);
    }

    @Test
    public void deserializeJsonWithRawJsonCollection() throws Exception {
        // ARRANGE
        ObjectMapper mapper = new ObjectMapper();
        String input = "{\"value\": \"[\\\"one_string\\\"]\"}";

        // ACT
        AnnotatedJson json = mapper.readValue(input, AnnotatedJson.class);

        // ASSERT
        assertThat(json, notNullValue());
        assertThat(json.values(), notNullValue());
        assertThat(json.values(), hasItem(equalTo("one_string")));
    }


    public static class Json {
        @JsonProperty
        List<String> value;
    }

    public static class AnnotatedJson {
        @JsonProperty
        @JsonDeserialize(using = RawJsonCollectionDeserializer.class)
        List<String> value;

        public List<String> values() {
            return value;
        }
    }
}