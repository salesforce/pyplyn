/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class StatusMessageTest {

    @Test
    public void testCreateStatusMessage() throws Exception {
        // ARRANGE
        StatusMessage message = new StatusMessage(AlertLevel.ERR, "error", 1.23d);

        // ACT
        AlertLevel level = message.level();
        String messageBody = message.toString();

        // ASSERT
        assertThat(level, equalTo(AlertLevel.ERR));
        assertThat(messageBody, allOf(containsString("error"), containsString("rate=1.23")));
    }

    @Test
    public void testStatusOk() throws Exception {
        // ARRANGE
        StatusMessage.Ok message = new StatusMessage.Ok();

        // ACT
        AlertLevel level = message.level();
        String messageBody = message.toString();

        // ASSERT
        assertThat(level, equalTo(AlertLevel.OK));
        assertThat(messageBody, containsString(StatusMessage.MESSAGE_NAME_OK));
    }
}