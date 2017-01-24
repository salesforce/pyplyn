/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.connector;

import com.google.inject.Inject;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.pyplyn.duct.app.BootstrapException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.nonNull;

/**
 * Allows multiple {@link com.google.inject.Guice} modules to specify their own {@link AbstractConnector}s and collects
 *   all the passed connectors in a map, allowing clients to retrieve them at a later stage.
 * <p/>
 * <p/>This implementation will ensure that no duplicate connectors exist and will throw a {@link BootstrapException} if
 *   duplicates are detected.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AppConnector {
    private final Map<String, AbstractConnector> connectors;

    /**
     * Collects all connector lists into a map, verifying if duplicates exist
     *
     * @throws BootstrapException if the same connectorId is specified in more than one connector list
     */
    @Inject
    public AppConnector(Set<List<AbstractConnector>> allConnectors) {
        this.connectors = new HashMap<>();

        // iterate through all passed connectors and add them to our list of known connectors
        for (List<AbstractConnector> connectors : allConnectors) {
            for (AbstractConnector connector : connectors) {
                // ensure uniqueness and throw exception if a duplicate connector is found
                if (nonNull(this.connectors.putIfAbsent(connector.connectorId(), connector))) {
                    throw new BootstrapException(String.format("Duplicate connector %s{endpointId=\"%s\"} is not allowed!",
                            connector.getClass().getSimpleName(), connector.connectorId()));
                }
            }
        }
    }

    /**
     * @return the required connector for the specified <b>endpointId</b> or null if not found
     */
    public AbstractConnector get(String endpointId) {
        return connectors.get(endpointId);
    }
}
