/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;


import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Link test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public class LinkTest {
    @Test
    public void toStringShouldExposeBothNameAndUrl() throws Exception {
        // ARRANGE
        Link link = defaultLink();

        // ACT
        String linkString = link.toString();

        // ASSERT
        assertThat(linkString, containsString("name"));
        assertThat(linkString, containsString("url"));
    }

    @Test
    public void testEqualsInCollections() throws Exception {
        // ARRANGE
        Link link = defaultLink();
        List<Link> links = Collections.singletonList(link);
        Map<Link, Integer> linkMap = new HashMap<>();
        linkMap.put(link, 1);

        // ASSERT
        assertThat(links, contains(link));
        assertThat(linkMap.get(link), is(not(nullValue())));
    }

    /**
     * Initializes a default link
     */
    public static Link defaultLink() {
        return new Link("name", "url");
    }
}