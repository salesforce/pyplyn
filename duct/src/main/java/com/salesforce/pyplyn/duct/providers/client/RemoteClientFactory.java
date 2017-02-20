/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.providers.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.cache.Cacheable;
import com.salesforce.pyplyn.client.ClientFactory;
import com.salesforce.pyplyn.client.ClientFactoryException;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.pyplyn.duct.connector.AppConnector;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Prepares a {@link ClientFactory} that can build configured implementations of {@link com.salesforce.pyplyn.client.AbstractRemoteClient}
 * <p/>
 * <p/>Annotated as Singleton since there should only be one factory in operation per class type
 *
 * @param <T> Class type of implementing remote client
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class RemoteClientFactory<T extends Cacheable> implements ClientFactory<T> {
    private AppConnector appConnector;
    private final Class<T> clientClass;
    private final Map<String, T> initializedClients = new ConcurrentHashMap<>();

    /**
     * This map is used to provide per-endpoint locks, to ensure only one client object is constructed per endpointId
     */
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();


    /**
     * Constructs a remote client factory for the specified generic type
     *
     * @param appConnector {@link AppConnector} object that retrieves {@link AbstractConnector} implementations by their <b>endpointId</b>
     * @param clientClass Class type for the remote client object provided by this object
     */
    @Inject
    public RemoteClientFactory(AppConnector appConnector, Class<T> clientClass) {
        this.appConnector = appConnector;
        this.clientClass = clientClass;
    }

    /**
     * Retrieves an existing client and cache for the specified endpoint
     * <p/>Initializes a new client if one doesn't exist already.
     * <p/>This implementation will ensure that multiple concurrent threads will not instantiate multiple clients.
     *
     * @param endpointId Connector endpoint id that this client will connect to
     */
    private T getClientCacheHolder(String endpointId) {
        // check if we already have a client
        T client = initializedClients.get(endpointId);

        if (isNull(client)) {
            // do not allow multiple threads to initialize the same client for the same endpointId
            ReentrantLock lock = getLock(endpointId);
            lock.lock();

            try {
                // after the lock is obtained, try loading the client again
                //   as another locking thread might have already initialized it
                client = initializedClients.get(endpointId);

                // initialize the client, if required
                if (isNull(client)) {
                    client = initializeClient(endpointId);
                }
            } finally {
                lock.unlock();
            }
        }


        return client;
    }

    /**
     * Retrieves or initializes a client for the specified endpoint
     */
    @Override
    public T getClient(String endpointId) {
        return getClientCacheHolder(endpointId);
    }

    /**
     * Initializes or retrieves an existing {@link ReentrantLock} for the specified <b>endpointId</b>
     */
    private ReentrantLock getLock(String endpointId) {
        // get existing lock, or create a new one
        ReentrantLock lock = new ReentrantLock();
        ReentrantLock oldLock = locks.putIfAbsent(endpointId, lock);

        // if putIfAbsent returns non-null, replace lock with the actual object
        if (nonNull(oldLock)) {
            lock = oldLock;
        }

        return lock;
    }

    /**
     * Constructs a new client for the specified <b>endpointId</b>
     */
    private T initializeClient(String endpointId) {
        try {
            // attempt to find a constructor with a single AbstractConnector parameter and build an instance
            Constructor<T> constructor = clientClass.getConstructor(AbstractConnector.class);
            T client = constructor.newInstance(appConnector.get(endpointId));

            // caches the object if successful
            initializedClients.put(client.cacheKey(), client);

            return client;

        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            // any of these exceptions will cause an unrecoverable exception, which is why we're wrapping it as uncaught
            throw new ClientFactoryException("Unexpected error constructing a client for " + endpointId, e);
        }
    }
}
