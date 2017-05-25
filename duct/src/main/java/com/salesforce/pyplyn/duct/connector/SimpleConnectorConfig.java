/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.pyplyn.configuration.AbstractConnector;

import java.util.Optional;

/**
 * File-based connector implementation
 * <p/>
 * <p/>Equality is determined by similarity of the values returned by {@link #connectorId()}, as implemented
 *   in {@link AbstractConnector#equals(Object)}
 * <p/>This class does not mark its fields final, since we don't have to define an explicit constructor
 *   and we're relying on Jackson deserialization to initialize it.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleConnectorConfig extends AbstractConnector {
    @JsonProperty(required = true)
    protected String id;

    @JsonProperty
    protected String endpoint;

    @JsonProperty
    protected String username;

    @JsonProperty
    protected String proxyHost;

    @JsonProperty
    protected Integer proxyPort;

    @JsonProperty
    protected Long connectTimeout;

    @JsonProperty
    protected Long readTimeout;

    @JsonProperty
    protected Long writeTimeout;


    /**
     * Holds a reference to where this connector was loaded from, to allow retrieving the password whenever we require it
     */
    @JsonIgnore
    private transient String connectorPath;


    @Override
    public String connectorId() {
        return id;
    }

    @Override
    public String endpoint() {
        return endpoint;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public byte[] password() {
        return InsecurePasswordUtil.readPasswordBytes(connectorFilePath(), connectorId());
    }

    @Override
    public String proxyHost() {
        return proxyHost;
    }

    @Override
    public int proxyPort() {
        return proxyPort;
    }

    @Override
    public long connectTimeout() {
        return Optional.ofNullable(connectTimeout).orElse(10L);
    }

    @Override
    public long readTimeout() {
        return Optional.ofNullable(readTimeout).orElse(10L);
    }

    @Override
    public long writeTimeout() {
        return Optional.ofNullable(writeTimeout).orElse(10L);
    }

    /**
     * Sets the source connector to use when retrieving the password
     * <p/>
     * <p/>This method is only available in the declaring package, since we don't want these to be changed
     *   after a connector has been initialized
     */
    void setConnectorFilePath(String file) {
        this.connectorPath = file;
    }

    /**
     * Retrieves the path to the file that holds this connector's definition
     * <p/>
     * <p/>Declared as package-private in order to be used in testing to ensure the password is read every time
     *  from disk and not cached!
     */
    String connectorFilePath() {
        return this.connectorPath;
    }
}
