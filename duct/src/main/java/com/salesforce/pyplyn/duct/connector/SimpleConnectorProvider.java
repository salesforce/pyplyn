/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.connector;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.salesforce.pyplyn.configuration.AbstractConnector;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.util.SerializationHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Reads all connector configurations from the input JSON and returns a list of connectors
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class SimpleConnectorProvider implements Provider<List<AbstractConnector>> {
    private final List<AbstractConnector> connectors;

    @Inject
    public SimpleConnectorProvider(AppConfig appConfig, SerializationHelper serializer) throws IOException {
        // if the connectors path is undefined, stop here
        String connectorPath = appConfig.global().connectorsPath();
        if (isNull(connectorPath)) {
            this.connectors = Collections.emptyList();
            return;
        }

        this.connectors = readFromConnectorsFile(serializer, connectorPath);
    }

    /**
     * Reads all connectors from file
     *   and returns a list of all defined connectors, without loading the password bytes
     *
     * @throws IOException on any deserialization errors
     */
    private List<AbstractConnector> readFromConnectorsFile(SerializationHelper serializer, String connectorPath) throws IOException {
        return Collections.unmodifiableList(
                Arrays.stream(serializer.deserializeJsonFile(connectorPath, SimpleConnectorConfig[].class))
                        .peek(c -> c.setConnectorFilePath(connectorPath))
                        .collect(Collectors.toList())
        );
    }

    /**
     * @return the list of loaded {@link AbstractConnector}s
     */
    @Override
    public List<AbstractConnector> get() {
        return connectors;
    }
}
