/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;


import com.salesforce.refocus.model.builder.SampleBuilder;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.*;
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
        Sample sample = new SampleBuilder()
                .withId("id")
                .withName("name")
                .withValue("value")
                .withRelatedLinks(Collections.singletonList(new Link("link", "url")))
                .build();

        // ASSERT
        assertThat(sample.name(), equalTo("name"));
        assertThat(sample.toString(), containsString("name"));
        assertThat(sample.value(), equalTo("value"));
        assertThat(sample.relatedLinks().size(), equalTo(1));
    }

    @Test
    public void shouldHaveEmptyListAsRelatedLinks() throws Exception {
        // ARRANGE
        Sample sample = new SampleBuilder()
                .withId("id")
                .withName("name")
                .withValue("value")
                .withRelatedLinks(Collections.emptyList())
                .build();

        // ASSERT
        assertThat(sample.name(), equalTo("name"));
        assertThat(sample.value(), equalTo("value"));
        assertThat(sample.relatedLinks(), is(instanceOf(Collection.class)));
        assertThat(sample.relatedLinks().size(), equalTo(0)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }
}