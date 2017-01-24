/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

/**
 * Thrown by ClientFactory, when a client cannot be constructed
 * <p/><p/>
 * Generally used throughout the project to wrap other exceptions that can be thrown by reflection constructors.
 * <p/>
 * If such an exception is thrown, it's reasonable to assume that the program should not recover, as it's
 *   either misconfigured, incomplete, or incorrectly configured dependencies are being injected.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ClientFactoryException extends RuntimeException {
    private static final long serialVersionUID = 2350861849452971823L;

    public ClientFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
