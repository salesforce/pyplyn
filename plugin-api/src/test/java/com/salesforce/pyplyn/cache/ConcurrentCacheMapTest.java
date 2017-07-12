/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.cache;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.salesforce.pyplyn.testutil.TimeUtil.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConcurrentCacheMapTest {
    static final String CACHE_KEY = "cacheKey";

    @Mock
    Cacheable expected;

    ConcurrentCacheMap<Cacheable> cache;


    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        doReturn(CACHE_KEY).when(expected).cacheKey(); // FindBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT - IGNORE

        cache = new CacheFactory().newCache();
        cache.cache(expected, 86400);
    }


    @Test
    public void testCacheableWithSameKeyReplacesEntry() throws Exception {
        Cacheable replacedWith = mock(Cacheable.class);
        doReturn(CACHE_KEY).when(replacedWith).cacheKey(); // FindBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT - IGNORE

        // ACT
        cache.cache(replacedWith, 86400);
        Cacheable actual = cache.isCached(CACHE_KEY);

        // ASSERT
        assertThat("Caching another object with the same key should have replaced previous entry",
                actual, is(not(expected)));
    }


    @Test
    public void testCacheKeyNotFound() throws Exception {
        // ACT
        Cacheable actual = cache.isCached("invalidKey");

        // ASSERT
        assertThat("There should be no object cached for that key", actual, is(nullValue()));
    }

    @Test
    public void testExpiredEntryShouldNotBeReturned() throws Exception {
        // ARRANGE
        cache.cache(expected, 1);

        // ACT
        await().atLeastMs(2);
        Cacheable actual = cache.isCached(CACHE_KEY);

        // ASSERT
        assertThat("Since entry had expired, it should not have been returned", actual, is(nullValue()));
    }
}