/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model.builder;

import com.salesforce.refocus.model.Sample;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.salesforce.refocus.model.builder.AspectBuilderTest.LINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * Test class
 * <p/>Ensures that the {@link SampleBuilder} functions as expected (allows composition of fields and generates
 *  an immutable {@link Sample} object when {@link SampleBuilder#build()} is called
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class SampleBuilderTest {
    private SampleBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        builder = defaultSampleBuilder();
    }

    @Test
    public void testExpectedFields() throws Exception {
        // ACT
        Sample sample = builder.build();

        // ASSERT
        assertThat(sample.id(), equalTo("id"));
        assertThat(sample.name(), equalTo("name"));
        assertThat(sample.value(), equalTo("value"));
        assertThat(sample.updatedAt(), equalTo("updatedAt"));
        assertThat(sample.tags(), equalTo(Collections.singletonList("tags")));
        assertThat(sample.relatedLinks(), equalTo(Collections.singletonList(LINK)));
        assertThat(sample.messageCode(), equalTo("messageCode"));
        assertThat(sample.messageBody(), equalTo("messageBody"));
    }

    @Test
    public void testCompositionByReplacingAllFields() throws Exception {
        // ARRANGE
        builder = new SampleBuilder(builder.build());

        // ACT
        Sample sample = builder
                .withId("idx")
                .withName("namex")
                .withValue("valuex")
                .withUpdatedAt("updatedAtx")
                .withTags(Collections.singletonList("tagsx"))
                .withRelatedLinks(null)
                .withMessageCode("messageCodex")
                .withMessageBody("messageBodyx")
                .build();

        // ASSERT
        assertThat(sample.id(), not(equalTo("id")));
        assertThat(sample.name(), not(equalTo("name")));
        assertThat(sample.value(), not(equalTo("value")));
        assertThat(sample.updatedAt(), not(equalTo("updatedAt")));
        assertThat(sample.tags(), not(equalTo(Collections.singletonList("tags"))));
        assertThat(sample.relatedLinks(), not(equalTo(Collections.singletonList(LINK))));
        assertThat(sample.messageCode(), not(equalTo("messageCode")));
        assertThat(sample.messageBody(), not(equalTo("messageBody")));
    }

    /**
     * Initializes a default builder that sets all fields accordingly
     */
    public static SampleBuilder defaultSampleBuilder() {
        return new SampleBuilder()
                .withId("id")
                .withName("name")
                .withValue("value")
                .withUpdatedAt("updatedAt")
                .withTags(Collections.singletonList("tags"))
                .withRelatedLinks(Collections.singletonList(LINK))
                .withMessageCode("messageCode")
                .withMessageBody("messageBody");
    }
}