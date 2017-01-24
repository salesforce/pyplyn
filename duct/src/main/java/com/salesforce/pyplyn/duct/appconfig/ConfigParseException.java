/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.appconfig;

/**
 * Wraps {@link net.sourceforge.argparse4j.inf.ArgumentParserException}s thrown when reading CLI arguments,
 *   to avoid exporting the <b>arg4j</b> dependency
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConfigParseException extends Exception {
    private static final long serialVersionUID = 4050860859458981423L;

    public ConfigParseException(Throwable cause) {
        super(cause);
    }
}
