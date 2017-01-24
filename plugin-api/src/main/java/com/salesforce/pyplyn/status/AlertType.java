/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

/**
 * Determines which type of check we should apply
 * <p/>
 * <p/>Alerts are triggered when they are either greater or less than the specified thresholds
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public enum AlertType {
    GREATER_THAN, LESS_THAN
}
