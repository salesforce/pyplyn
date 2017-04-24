/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.standard;

import com.salesforce.pyplyn.model.TransformationResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class HighestValueTest {
    private final ZonedDateTime NOW = ZonedDateTime.now(ZoneOffset.UTC);
    private List<List<TransformationResult>> transformationResults;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        List<TransformationResult> series = Collections.singletonList(
                new TransformationResult(NOW, "metric", 10d, 10d)
        );

        transformationResults = Collections.singletonList(series);
    }



    @Test
    public void testOriginalDateIsAddedToMessageBody() throws Exception {
        // ARRANGE
        HighestValue transform = new HighestValue(null, HighestValue.Display.ORIGINAL_TIMESTAMP);

        // ACT
        List<List<TransformationResult>> results = transform.apply(transformationResults);

        // ASSERT
        assertThat(results, not(empty()));
        assertThat(results.get(0), not(empty()));

        TransformationResult result = results.get(0).get(0);
        assertThat("The result should contain the original time",
                result.metadata().messages(), hasItem(containsString(NOW.toString())));
    }
}