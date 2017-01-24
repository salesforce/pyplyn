/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

/**
 * Exception thrown when a remote operation fails due to the endpoint not being correctly authenticated
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class UnauthorizedException extends Exception {
    static final long serialVersionUID = -4327511993224269948L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
