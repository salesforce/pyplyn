/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AbstractConnectorImpl extends AbstractConnector {
    private String connectorId;

    public AbstractConnectorImpl(String connectorId) {
        this.connectorId = connectorId;
    }

    @Override
    public String connectorId () {
        return connectorId;
    }

    @Override
    public String endpoint () {
        return null;
    }

    @Override
    public String username () {
        return null;
    }

    @Override
    public byte[] password () {
        return new byte[0];
    }

    @Override
    public String proxyHost () {
        return null;
    }

    @Override
    public int proxyPort () {
        return 0;
    }
}
