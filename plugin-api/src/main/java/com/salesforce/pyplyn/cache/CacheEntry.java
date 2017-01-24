/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.cache;

/**
 * Represents a cache entry
 * <p/>
 * <p/>Holds a value and its expiration time
 * <p/>
 * <p/>The decision to not support entries that do not expire, is by design.
 * <p/>
 * <b>Avoid setting very long expiration durations and allow objects to be garbage collected every so often!</b>
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class CacheEntry<T> {
    /**
     * Cached value
     */
    private final T value;

    /**
     * When the cache expires
     */
    private final long expiresAt;

    /**
     * Constructs a new cache entry and sets its expiration
     */
    public CacheEntry(T value, long expireMillis) {
        this.value = value;
        this.expiresAt = System.currentTimeMillis() + expireMillis;
    }

    /**
     * @return the cache entry's value
     */
    public T value() {
        return value;
    }

    /**
     * @return true if the current time is past the <b>expiresAt</b> value
     */
    public boolean expired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
