/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

/**
 * Defines how a client factory should operate
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface ClientFactory<T> {

    /**
     * @return a configured client object, connecting to the specified endpointId
     * @throws ClientFactoryException implementations should throw this exception if a client can not be configured
     *                                for the required endpoint
     */
    T getClient(String endpointId);
}
