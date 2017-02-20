/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;


import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;

import static com.salesforce.refocus.model.builder.SampleBuilderTest.defaultSampleBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Sample test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public class SampleTest {
    @Test
    public void shouldStoreAllParameters() throws Exception {
        // ARRANGE
        Sample sample = defaultSampleBuilder().build();

        // ASSERT
        assertThat(sample.name(), equalTo("name"));
        assertThat(sample.toString(), containsString("name"));
        assertThat(sample.value(), equalTo("value"));
        assertThat(sample.relatedLinks().size(), equalTo(1));
    }

    @Test
    public void shouldHaveEmptyListAsRelatedLinks() throws Exception {
        // ARRANGE
        Sample sample = defaultSampleBuilder()
                .withRelatedLinks(Collections.emptyList())
                .build();

        // ASSERT
        assertThat(sample.name(), equalTo("name"));
        assertThat(sample.value(), equalTo("value"));
        assertThat(sample.relatedLinks(), is(instanceOf(Collection.class)));
        assertThat(sample.relatedLinks().size(), equalTo(0));
    }

    @Test
    public void testCacheKeyIsName() throws Exception {
        // ARRANGE
        Sample sample = defaultSampleBuilder().build();

        // ACT
        String cacheKey = sample.cacheKey();

        // ASSERT
        assertThat(cacheKey, equalTo(sample.name()));
    }
}