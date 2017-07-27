/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import com.salesforce.pyplyn.model.StatusCode;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
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
        StatusMessage message = new StatusMessage(StatusCode.ERR, "error 1.23");

        // ACT
        StatusCode level = message.level();
        String messageBody = message.toString();

        // ASSERT
        assertThat(level, equalTo(StatusCode.ERR));
        assertThat(messageBody, allOf(containsString("error"), containsString("1.23")));
    }
}