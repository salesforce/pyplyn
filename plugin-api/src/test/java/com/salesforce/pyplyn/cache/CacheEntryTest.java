/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.cache;

import org.testng.annotations.Test;

import static com.salesforce.pyplyn.testutil.TimeUtil.await;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class CacheEntryTest {
    @Test
    public void testValue() throws Exception {
        // ARRANGE
        CacheEntry<Double> entry = new CacheEntry<>(1.0d, 86400);

        // ACT
        Double value = entry.value();
        boolean isExpired = entry.expired();

        // ASSERT
        assertThat("Value is different to what was passed in", value, equalTo(1.0d));
        assertThat("Cache entry should not have expired", isExpired, is(false));
    }

    @Test
    public void testExpired() throws Exception {
        // ARRANGE
        CacheEntry<Double> entry = new CacheEntry<>(1.0d, 1);

        // ACT
        await().atLeastMs(2);
        boolean isExpired = entry.expired();

        // ASSERT
        assertThat("Cache entry should have expired already", isExpired, is(true));
    }
}