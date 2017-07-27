package com.salesforce.refocus.model;

import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public class SampleTest {
    @Test
    public void testToStringContainsLastDatapoint() throws Exception {
        // ARRANGE
        Sample sample = ImmutableSample.builder().name("sample").value("1.0").build();


        // ACT
        String stringResponse = sample.toString();


        // ASSERT
        assertThat(stringResponse, containsString("sample"));
        assertThat(stringResponse, containsString("1.0"));
    }
}
