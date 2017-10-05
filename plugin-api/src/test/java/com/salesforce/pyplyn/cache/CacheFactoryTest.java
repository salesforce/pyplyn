/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.cache;

import static com.salesforce.pyplyn.cache.ConcurrentCacheMapTest.CACHE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class CacheFactoryTest {
    @Mock
    Cacheable expected;

    ConcurrentCacheMap<Cacheable> cache;


    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        doReturn(CACHE_KEY).when(expected).cacheKey();

        cache = new CacheFactory().newCache();
        cache.cache(expected, 86400);
    }

    @Test
    public void testConstructNewCache() throws Exception {
        // ACT
        Cacheable actual = cache.isCached(CACHE_KEY);

        // ASSERT
        assertThat("The same object should have been returned from the cache", actual, is(expected));
    }
}