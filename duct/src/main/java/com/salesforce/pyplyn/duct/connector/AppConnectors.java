/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.connector;

import static java.util.Objects.nonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.cache.CacheFactory;
import com.salesforce.pyplyn.cache.Cacheable;
import com.salesforce.pyplyn.cache.ConcurrentCacheMap;
import com.salesforce.pyplyn.client.RemoteClient;
import com.salesforce.pyplyn.configuration.Connector;
import com.salesforce.pyplyn.configuration.EndpointConnector;
import com.salesforce.pyplyn.duct.app.BootstrapException;

/**
 * Allows multiple {@link com.google.inject.Guice} modules to specify their own {@link Connector}s and collects
 *   all the passed connectors in a map, allowing clients to retrieve them at a later stage.
 * <p/>
 * <p/>This implementation will ensure that no duplicate connectors exist and will throw a {@link BootstrapException} if
 *   duplicates are detected.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class AppConnectors {
    public static final String DUPLICATE_CONNECTOR_ERROR = "Duplicate connector object (%s) not allowed, with id=\"%s\"!";
    private final Map<String, EndpointConnector> connectors;
    private final Map<String, ClientAndCache<? extends RemoteClient, ? extends Cacheable>> registeredClients;
    private final CacheFactory cacheFactory;


    /**
     * Collects all connector lists into a map, verifying if duplicates exist
     *
     * @throws BootstrapException if the same connectorId is specified in more than one connector list
     */
    @Inject
    public AppConnectors(Set<List<EndpointConnector>> allConnectors, CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
        this.connectors = new HashMap<>();

        // iterate through all passed connectors and add them to our list of known connectors
        for (List<EndpointConnector> connectors : allConnectors) {
            for (EndpointConnector connector : connectors) {
                // ensure uniqueness and throw exception if a duplicate connector is found
                if (nonNull(this.connectors.putIfAbsent(connector.id(), connector))) {
                    throw new BootstrapException(String.format(DUPLICATE_CONNECTOR_ERROR,
                            connector.getClass().getSimpleName(), connector.id()));
                }
            }
        }

        registeredClients = new ConcurrentHashMap<>();
    }

    /**
     * @return the required connector for the specified <b>connectorId</b> or null if not found
     */
    public EndpointConnector findConnector(String connectorId) {
        return connectors.get(connectorId);
    }

    /**
     * Returns a (client, cache) pair, either by retrieving it from a cache,
     *   or by constructing the required objects on demand
     *
     *   TODO: define an interface for this class in plugin-api
     *   TODO: find a better way to achieve this (mapping of String ID + Target class, allowing any number of mappings for a specified identifier)
     */
    @SuppressWarnings("unchecked")
    public <CLIENT extends RemoteClient, CACHE extends Cacheable> ClientAndCache<CLIENT, CACHE> retrieveOrBuildClient(final String connectorId,
                                                                               Class<CLIENT> clientClass,
                                                                               Class<CACHE> cacheClass) {
        return (ClientAndCache<CLIENT, CACHE>) registeredClients.computeIfAbsent(connectorId, key -> {
            try {
                // init client
                Constructor<CLIENT> constructor = clientClass.getConstructor(EndpointConnector.class);
                CLIENT client = constructor.newInstance(findConnector(key));

                // init cache
                ConcurrentCacheMap<CACHE> cache = cacheFactory.newCache();

                // return
                return new ClientAndCache<>(client, cache);

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                // this signals an implementation error (development error) and as such we can't do anything but stop
                //   it should never happen if the clients are implemented correctly:
                //   i.e.: any Remote Client relying on this class, should provide a constructor with a single parameter of type Connector
                throw new BootstrapException(String.format("Could not find connector with specified id or a valid constructor in %s:",
                        clientClass.getTypeName()), e);
            }
        });
    }

    /**
     * Used to store a client and its corresponding cache object
     */
    public static class ClientAndCache<CLIENT, CACHE extends Cacheable> {
        final CLIENT client;
        final ConcurrentCacheMap<CACHE> cache;

        public ClientAndCache(CLIENT client, ConcurrentCacheMap<CACHE> cache) {
            this.client = client;
            this.cache = cache;
        }

        public CLIENT client() {
            return client;
        }

        public ConcurrentCacheMap<CACHE> cache() {
            return cache;
        }
    }
}
