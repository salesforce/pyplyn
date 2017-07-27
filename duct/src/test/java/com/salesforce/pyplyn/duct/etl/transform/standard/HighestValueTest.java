/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.standard;

import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.Transmutation;
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
    private List<List<Transmutation>> transformationResults;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        Transmutation.Metadata metadata = ImmutableTransmutation.Metadata.builder().build();
        List<Transmutation> series = Collections.singletonList(
                ImmutableTransmutation.of(NOW, "metric", 10d, 10d, metadata)
        );

        transformationResults = Collections.singletonList(series);
    }



    @Test
    public void testOriginalDateIsAddedToMessageBody() throws Exception {
        // ARRANGE
        HighestValue transform = ImmutableHighestValue.of(null, HighestValue.Display.ORIGINAL_TIMESTAMP);

        // ACT
        List<List<Transmutation>> results = transform.apply(transformationResults);

        // ASSERT
        assertThat(results, not(empty()));
        assertThat(results.get(0), not(empty()));

        Transmutation result = results.get(0).get(0);
        assertThat("The result should contain the original time",
                result.metadata().messages(), hasItem(containsString(NOW.toString())));
    }
}