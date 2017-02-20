/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import org.testng.annotations.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class AuthRequestTest {
    @Test
    public void testIsUsername() throws Exception {
        // ARRANGE
        AuthRequest request = new AuthRequest("username", "password".getBytes());

        // ACT/ASSERT
        assertThat(request.isSameUsername("username"), equalTo(true));
        assertThat(request.isSamePassword("password".getBytes()), equalTo(true));
    }
}