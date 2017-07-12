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
import static org.hamcrest.Matchers.not;

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
        assertThat(request, equalTo(new AuthRequest("username", "password".getBytes())));
        assertThat(request, not(equalTo(new AuthRequest("username", "passwordx".getBytes()))));
        assertThat(request, not(equalTo(new AuthRequest("usernamex", "password".getBytes()))));
    }

    @Test
    public void testHashcode() throws Exception {
        // ARRANGE
        AuthRequest request = new AuthRequest("username", "password".getBytes());
        AuthRequest request2 = new AuthRequest("username", "password".getBytes());

        // ACT/ASSERT
        assertThat(request.hashCode(), equalTo(request2.hashCode()));
    }
}