/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.systemstatus;

import com.salesforce.pyplyn.status.StatusMessage;
import com.salesforce.pyplyn.status.SystemStatusConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Simple implementation that outputs system status messages to console
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConsoleOutputConsumer implements SystemStatusConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleOutputConsumer.class);

    /**
     * Accepts the passed {@link StatusMessage}s and prints via the slf4j {@link Logger}
     */
    @Override
    public void accept(List<StatusMessage> messages) {
        logger.info("System status: " + Arrays.toString(messages.toArray()));
    }
}
