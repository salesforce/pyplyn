/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class AuthResponseTest {
    @Test
    public void testSuccess() throws Exception {
        // ARRANGE
        AuthResponse response = new AuthResponse("success", "message", "token");

        // ACT/ASSERT
        assertThat(response.success(), equalTo("success"));
        assertThat(response.message(), equalTo("message"));
        assertThat(response.token(), equalTo("token"));
    }
}