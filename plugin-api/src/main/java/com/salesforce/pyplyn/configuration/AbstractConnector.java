/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

import static java.util.Objects.nonNull;

/**
 * Base connector class
 * <p/>
 * Provides endpoint, user, password, proxy host, and port values that are used by implementations of
 *   {@link com.salesforce.pyplyn.client.AbstractRemoteClient} to authenticate against the endpoints specified
 *   in {@link com.salesforce.pyplyn.model.Extract} and {@link com.salesforce.pyplyn.model.Load}
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public abstract class AbstractConnector {
    /**
     * @return a unique value, used to identify the connector to use in the
     *   specified {@link Configuration} (Configuration.endpoint should match connectorId())
     */
    public abstract String connectorId();

    /**
     * @return the URL to the API's endpoint
     */
    public abstract String endpoint();

    /**
     * @return username to authenticate against
     */
    public abstract String username();

    /**
     * @return password to authenticate with
     */
    public abstract byte[] password();

    /**
     * If using an HTTP proxy, specify the hostname (without protocol information)
     * <p/> i.e.: proxy-host.example.com
     *
     * @return null if not using a proxy
     */
    public abstract String proxyHost();

    /**
     * If using an HTTP proxy, specify the port
     * <p/>i.e.: 8080
     *
     * @return null if not using a proxy
     */
    public abstract int proxyPort();

    /**
     * Helper method that determines if a valid proxy configuration was specified
     *
     * @return true if the Connector has valid proxy settings
     */
    public final boolean isProxyEnabled() {
        return nonNull(proxyHost()) && proxyPort() > 0;
    }

    /**
     * Object.equals override, used to ensure we don't have more than one connectors with the same id
     *
     * @param other connector to compare against
     * @return true if same object or connectorId()
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof AbstractConnector)) {
            return false;
        }

        AbstractConnector that = (AbstractConnector) other;

        return connectorId().equals(that.connectorId());
    }

    @Override
    public int hashCode() {
        return connectorId().hashCode();
    }
}
