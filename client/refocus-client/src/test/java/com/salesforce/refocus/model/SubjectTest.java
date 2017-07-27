package com.salesforce.refocus.model;

import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class SubjectTest {
    @Test
    public void testToStringContainsPaths() throws Exception {
        // ARRANGE
        Subject subject = ImmutableSubject.builder()
                .name("subject")
                .isPublished(true)
                .absolutePath("absolutePath")
                .parentAbsolutePath("parentAbsolutePath")
                .parentId("parentId")
                .build();


        // ACT
        String stringResponse = subject.toString();


        // ASSERT
        assertThat(stringResponse, allOf(
                containsString("absolutePath"),
                containsString("parentAbsolutePath"),
                containsString("parentId")));
    }
}
