/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.cache;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

/**
 * Cache implementation based on a ConcurrentMap
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConcurrentCacheMap<T extends Cacheable> implements Cache<T> {
    /**
     * Concurrent map that holds the cached values
     */
    private final Map<String, WeakReference<CacheEntry<T>>> cache = new ConcurrentHashMap<>();

    /**
     * Caches an <b>object</b> for <b>millis</b> milliseconds
     * <p/>
     * <p/>Values are stored as {@link WeakReference}s to allow them to be garbage collected if memory is low.
     *
     * @param object the value to cache
     * @param millis the number of milliseconds to cache for
     */
    @Override
    public void cache(T object, long millis) {
        cache.put(object.cacheKey(), new WeakReference<>(new CacheEntry<>(object, millis)));
    }

    /**
     * @param key cache key to retrieve
     * @return the cached value if found, or null if it expired or was garbage collected
     */
    @Override
    public T isCached(final String key) {
        // retrieve reference from cache and then attempt to retrieve the entry
        final WeakReference<CacheEntry<T>> entryRef = cache.get(key);

        // stop here if cache entry does not exist
        if (isNull(entryRef)) {
            return null;
        }

        // weak reference was GC'd or entry has expired
        CacheEntry<T> entry = entryRef.get();
        if (isNull(entry) || entry.expired()) {
            // remove from cache and return null
            cache.remove(key, entryRef);
            return null;
        }

        return entry.value();
    }
}
