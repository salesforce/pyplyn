/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.status;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesforce.pyplyn.client.AbstractRemoteClient;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.EndpointConnector;
import com.salesforce.status.model.Instance;

/**
 * Refocus API implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public class Trust1Client extends AbstractRemoteClient<Trust1Service> {
    private static final Logger logger = LoggerFactory.getLogger(Trust1Client.class);

    /**
     * Default simplified constructor that uses the specified connection defaults
     *
     * @param connector The InfluxDB API endpoint to use in calls
     */
    public Trust1Client(EndpointConnector connector) {
        super(connector, Trust1Service.class);
    }

    /**
     * Simplified call to load the known instances, without the need to specify the default parameters
     */
    public List<Instance> instancePreview() {
        return instancePreview(null, false, null);
    }

    /**
     * Returns the list of known instances and their statuses
     */
    public List<Instance> instancePreview(String productId, boolean childProducts, String locale) {
        try {
            return executeAndRetrieveBody(svc().instancePreview(productId, childProducts, locale), null);

        } catch (UnauthorizedException e) {
            // this should never happen
            logger.error("Unexpected auth exception in Trust1 API call", e);
            return null;
        }
    }

    /**
     * @return true if the current client is authenticated against its endpoint;
     *   defaults to true since this API does not support authentication
     */
    @Override
    public boolean isAuthenticated() {
        return true;
    }

    /**
     * Not implemented
     */
    @Override
    protected boolean auth() throws UnauthorizedException {
        return true;
    }

    /**
     * Not implemented
     */
    @Override
    protected void resetAuth() {

    }

}