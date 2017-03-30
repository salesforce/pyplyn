/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model.builder;

import com.salesforce.refocus.model.Sample;
import com.salesforce.refocus.model.Subject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.salesforce.refocus.model.builder.AspectBuilderTest.LINK;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 * <p/>Ensures that the {@link SubjectBuilder} functions as expected (allows composition of fields and generates
 *  an immutable {@link Subject} object when {@link SubjectBuilder#build()} is called
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class SubjectBuilderTest {
    public static final Sample SAMPLE = SampleBuilderTest.defaultSampleBuilder().build();
    public static final Subject LEAF_SUBJECT = defaultSubjectBuilder().build();

    private SubjectBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        builder = defaultSubjectBuilder()
            .withChildren(Collections.singletonList(LEAF_SUBJECT));
    }

    @Test
    public void testExpectedFields() throws Exception {
        // ACT
        Subject sample = builder.build();

        // ASSERT
        assertThat(sample.id(), equalTo("id"));
        assertThat(sample.parentId(), equalTo("parentId"));
        assertThat(sample.name(), equalTo("name"));
        assertThat(sample.absolutePath(), equalTo("absolutePath"));
        assertThat(sample.parentAbsolutePath(), equalTo("parentAbsolutePath"));
        assertThat(sample.description(), equalTo("description"));
        assertThat(sample.isPublished(), equalTo(true));
        assertThat(sample.children(), equalTo(Collections.singletonList(LEAF_SUBJECT)));
        assertThat(sample.tags(), equalTo(Collections.singletonList("tags")));
        assertThat(sample.samples(), equalTo(Collections.singletonList(SAMPLE)));
        assertThat(sample.relatedLinks(), equalTo(Collections.singletonList(LINK)));
    }

    @Test
    public void testCompositionByReplacingAllFields() throws Exception {
        // ARRANGE
        builder = new SubjectBuilder(builder.build());

        // ACT
        Subject sample = builder
                .withId("idx")
                .withParentId("parentIdx")
                .withName("namex")
                .withAbsolutePath("absolutePathx")
                .withParentAbsolutePath("parentAbsolutePathx")
                .withDescription("descriptionx")
                .withPublished(false)
                .withChildren(null)
                .withTags(Collections.singletonList("tagsx"))
                .withSamples(null)
                .withRelatedLinks(null)
                .build();

        // ASSERT
        assertThat(sample.id(), not(equalTo("id")));
        assertThat(sample.parentId(), not(equalTo("parentId")));
        assertThat(sample.name(), not(equalTo("name")));
        assertThat(sample.absolutePath(), not(equalTo("absolutePath")));
        assertThat(sample.parentAbsolutePath(), not(equalTo("parentAbsolutePath")));
        assertThat(sample.description(), not(equalTo("description")));
        assertThat(sample.isPublished(), not(equalTo(true)));
        assertThat(sample.children(), not(equalTo(Collections.singletonList(LEAF_SUBJECT))));
        assertThat(sample.tags(), not(equalTo(Collections.singletonList("tags"))));
        assertThat(sample.samples(), not(equalTo(Collections.singletonList(SAMPLE))));
        assertThat(sample.relatedLinks(), not(equalTo(Collections.singletonList(LINK))));
    }

    /**
     * Initializes a default builder with no children set
     */
    public static SubjectBuilder defaultSubjectBuilder() {
        return new SubjectBuilder()
                .withId("id")
                .withParentId("parentId")
                .withName("name")
                .withAbsolutePath("absolutePath")
                .withParentAbsolutePath("parentAbsolutePath")
                .withDescription("description")
                .withPublished(true)
                .withTags(Collections.singletonList("tags"))
                .withSamples(Collections.singletonList(SAMPLE))
                .withRelatedLinks(Collections.singletonList(LINK));
    }
}