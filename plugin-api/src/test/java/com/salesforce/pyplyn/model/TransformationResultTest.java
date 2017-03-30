/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model;

import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since ${project.version}
 */
public class TransformationResultTest {
    private TransformationResult result;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        result = new TransformationResult(Instant.now().atZone(ZoneOffset.UTC), "name", 0.0d, 0.0d);
    }

    @Test
    public void testResultShouldAlwaysHaveMetadata() throws Exception {
        // ACT
        ETLMetadata metadata = result.metadata();

        // ASSERT
        assertThat(metadata, notNullValue());
    }


    @Test
    public void testBuildResult() throws Exception {
        // ARRANGE
        ZonedDateTime newTime = Instant.now().atZone(ZoneOffset.UTC);
        TransformationResult newResult = new TransformationResultBuilder(result)
                .withValue(1.0d)
                .withTime(newTime)
                .withName("new")
                .withOriginalValue(2.0d)
                .metadata((metadata) -> {
                    metadata.setMessageCode("code");
                }).build();

        // ACT
        Number value = newResult.value();
        ZonedDateTime time = newResult.time();
        String name = newResult.name();
        Number originalValue = newResult.originalValue();
        String messageCode = newResult.metadata().messageCode();

        // ASSERT
        assertThat(value, is(1.0d));
        assertThat(time, is(newTime));
        assertThat(name, is("new"));
        assertThat(originalValue, is(2.0d));
        assertThat(messageCode, is("code"));
    }
}