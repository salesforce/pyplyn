/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model;

import com.salesforce.pyplyn.model.builder.ETLMetadataBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since ${project.version}
 */
public class ETLMetadataTest {
    private ETLMetadata metadata;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        metadata = new ETLMetadataBuilder()
                .setMessageCode("code")
                .addMessage("message1")
                .addMessages(Arrays.asList("message2", "message3"))
                .addTag("tag1", "value1")
                .addTags(Collections.singletonMap("tag2", "value2"))
                .build();
    }

    @Test
    public void testConstructMetadata() throws Exception {
        // ACT
        String messageCode = metadata.messageCode();
        List<String> messages = metadata.messages();
        Map<String, String> tags = metadata.tags();

        // ASSERT
        assertThat(messageCode, equalTo("code"));
        assertThat(messages, contains("message1", "message2", "message3"));
        assertThat(tags, hasEntry("tag1", "value1"));
        assertThat(tags, hasEntry("tag2", "value2"));
    }

    @Test
    public void testCloneExistingObject() throws Exception {
        // ARRANGE
        ETLMetadata cloned = new ETLMetadataBuilder(metadata).resetMessages().build();

        // ACT
        String messageCode = metadata.messageCode();
        List<String> messages = cloned.messages();

        // ASSERT
        assertThat("Expecting message code to be preserved", messageCode, equalTo("code"));
        assertThat("Expecting no messages", messages, hasSize(0));
    }
}