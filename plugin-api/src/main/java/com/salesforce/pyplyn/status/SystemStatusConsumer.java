/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import java.util.List;

/**
 * Interface that defines the {@link SystemStatus} consumer contract
 * <p/>
 * <p/>Used to implement functionality to report Pyplyn statuses to other systems
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface SystemStatusConsumer {
    void accept(List<StatusMessage> messages);
}
