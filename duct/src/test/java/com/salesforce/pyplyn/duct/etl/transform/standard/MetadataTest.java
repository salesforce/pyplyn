package com.salesforce.pyplyn.duct.etl.transform.standard;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.Transmutation;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class MetadataTest {
    private Transmutation data;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        Transmutation.Metadata metadata = ImmutableTransmutation.Metadata.builder()
                .tags(singletonMap("data", "point"))
                .build();
        data = ImmutableTransmutation.of(ZonedDateTime.now(ZoneOffset.UTC),
                "test",
                1.0d,
                100.0d,
                metadata);
    }

    @Test
    public void metadataTagsAreAppended() throws Exception {
        // ARRANGE
        Metadata test = ImmutableMetadata.builder().tags(Collections.singletonMap("key", "value")).build();

        // ACT
        List<List<Transmutation>> result = test.apply(singletonList(singletonList(data)));

        // ASSERT
        assertThat(result, hasSize(1));
        assertThat(result.get(0), hasSize(1));
        assertThat(result.get(0).get(0).metadata().tags().entrySet(), hasSize(2));
        assertThat(result.get(0).get(0).metadata().tags(), allOf(
                hasEntry("key", "value"), hasEntry("data", "point")));
    }

    @Test
    public void metadataTagsNotSpecifiedDoesNotLoseTags() throws Exception {
        // ARRANGE
        Metadata test = ImmutableMetadata.builder().build();

        // ACT
        List<List<Transmutation>> result = test.apply(singletonList(singletonList(data)));

        // ASSERT
        assertThat(result, hasSize(1));
        assertThat(result.get(0), hasSize(1));
        assertThat(result.get(0).get(0).metadata().tags().entrySet(), hasSize(1));
        assertThat(result.get(0).get(0).metadata().tags(), hasEntry("data", "point"));
    }


    @Test
    public void duplicateMetadataTagsOverrideOlderValues() throws Exception {
        // ARRANGE
        Metadata test = ImmutableMetadata.builder().tags(Collections.singletonMap("data", "overridden")).build();

        // ACT
        List<List<Transmutation>> result = test.apply(singletonList(singletonList(data)));

        // ASSERT
        assertThat(result, hasSize(1));
        assertThat(result.get(0), hasSize(1));
        assertThat(result.get(0).get(0).metadata().tags().entrySet(), hasSize(1));
        assertThat(result.get(0).get(0).metadata().tags(), hasEntry("data", "overridden"));
    }
}