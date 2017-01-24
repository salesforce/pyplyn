/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.cache;

/**
 * Simple caching interface
 * <p/>
 * <p/>1. cache: will save a value in the cache for the number of specified millis
 * <p/>2. isCached: will return the cached value, if it exists
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface Cache<T extends Cacheable> {
    /**
     * Cache an object, with the specified key and duration
     *
     * @param object the value to cache
     * @param millis the number of milliseconds to cache for
     */
    void cache(T object, long millis);

    /**
     * Returns the object if it exists in cache
     * <p/>
     * <p/>Should return null if it doesn't, but the functionality depends on the underlying implementation.
     *
     * @param key cache key name to retrieve
     * @return object of type <T> or null
     */
    T isCached(String key);
}
