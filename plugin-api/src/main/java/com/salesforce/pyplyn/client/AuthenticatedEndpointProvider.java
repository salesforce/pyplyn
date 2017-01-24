/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

import com.salesforce.pyplyn.cache.Cache;
import com.salesforce.pyplyn.cache.CacheFactory;
import com.salesforce.pyplyn.cache.Cacheable;
import com.salesforce.pyplyn.cache.ConcurrentCacheMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.isNull;

/**
 * Defines how providing a client for a passed <b>endpointId</b> should work
 * <p/>
 * Also handles {@link AbstractRemoteClient} authentication
 * <p/>
 * @param <T> specific client type implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface AuthenticatedEndpointProvider<T extends AbstractRemoteClient> {
    /**
     * This lock is used to avoid calling {@link AbstractRemoteClient}.auth() multiple times from multiple
     *   threads requiring the same client concurrently
     */
    ReentrantLock authLock = new ReentrantLock();

    /**
     * @return a factory object that can build a client for the passed endpointId
     */
    ClientFactory<T> factory();

    /**
     * Constructs the required client, based on the provided <b>endpointId</b>
     * <p/>
     * Authenticates the client before returning (if not already authenticated)
     * <p/><p/>
     * The factory() can return a previously authenticated object, if one exists and was cached;
     *   this is left at the discretion of the class implementing {@link ClientFactory}
     *
     * @throws ClientFactoryException if an invalid or misconfigured client was specified
     * @throws UnauthorizedException if authentication has failed
     */
    default T remoteClient(String endpointId) throws UnauthorizedException {
        // retrieve client and authenticate if necessary
        T client = factory().getClient(endpointId);

        // prevent locking if already authenticated
        if (!client.isAuthenticated()) {
            authLock.lock();

            try {
                // the extra authentication check is performed to avoid unnecessarily authenticating extra threads
                //   that were waiting to get the lock and authenticate, while another (the first thread that obtained it)
                //   had already performed the operation
                if (!client.isAuthenticated()) {
                    client.auth();
                }

            } finally {
                authLock.unlock();
            }
        }

        return client;
    }

    /**
     * Constructs a cache object that is used only for the specified <b>client</b>
     *
     * @param cacheMap stores all known caches for the specified R/C pair
     * @param client object that the cache object pertains to
     * @param cacheFactory will be used to construct a new cache, if one does not exist
     * @param <R> cacheable response type
     * @param <C> object to cache
     * @return {@link Cache} implementation
     */
    default <R extends Cacheable, C> Cache<R> getOrInitializeCacheFor(ConcurrentHashMap<C, ConcurrentCacheMap<R>> cacheMap,
                                                                      C client,
                                                                      CacheFactory cacheFactory) {
        // retrieve cache object
        ConcurrentCacheMap<R> cache = cacheMap.get(client);

        // if one does not exist, initialize it
        if (isNull(cache)) {
            cacheMap.putIfAbsent(client, cacheFactory.newCache());
            cache = cacheMap.get(client);
        }

        return cache;
    }
}
