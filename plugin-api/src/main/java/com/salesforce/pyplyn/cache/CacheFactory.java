/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.cache;

/**
 * Constructs new caches when required
 * <p/>
 * <p/>At this moment this class only return {@link ConcurrentCacheMap}s. It was written like this to support
 * adding more types in the future, but also to be able to globally control the cache implementation used
 * throughout the project
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class CacheFactory {
    /**
     * Constructs a new Cache object
     *
     * @param <T> type of elements that the returned cache can hold
     */
    public <T extends Cacheable> ConcurrentCacheMap<T> newCache() {
        return new ConcurrentCacheMap<>();
    }
}
