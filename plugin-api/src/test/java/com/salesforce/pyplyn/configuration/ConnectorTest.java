/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class ConnectorTest {
    private Connector connector;

    @BeforeMethod
    public void setUp() throws Exception {
        connector = ImmutableConnector.builder()
                .id("connector")
                .endpoint("endpoint")
                .proxyHost("proxyHost")
                .proxyPort(1)
                .password("password".getBytes())
                .build();
    }

    @Test
    public void testProxyEnabledWhenParamsAreDefined() throws Exception {
        // ACT
        boolean proxyEnabled = connector.isProxyEnabled();

        // ASSERT
        assertThat("Proxy should be enabled", proxyEnabled, equalTo(true));
    }

    @Test
    public void testProxyDisabled() throws Exception {
        // ARRANGE
        connector = ImmutableConnector.builder()
                .id("connector1")
                .endpoint("endpoint")
                .password("".getBytes())
                .build();

        // ACT
        boolean proxyEnabled = connector.isProxyEnabled();

        // ASSERT
        assertThat("Proxy should not be enabled with invalid params", proxyEnabled, equalTo(false));
    }

    @Test
    public void testEquals() throws Exception {
        // ARRANGE
        Connector connector = ImmutableConnector.builder()
                .id("connector")
                .endpoint("endpoint1")
                .password("".getBytes())
                .build();

        Connector anotherConnector = ImmutableConnector.builder()
                .id("connector")
                .endpoint("endpoint2")
                .password("".getBytes())
                .build();

        // ACT
        boolean equal = connector.equals(anotherConnector);
        boolean sameHashcode = connector.hashCode() == anotherConnector.hashCode();

        // ASSERT
        assertThat("Expecting connectors with similar ids to be equal", equal, is(true));
        assertThat("Expecting different instances", connector, not(sameInstance(anotherConnector)));
        assertThat("Expecting hashcodes to be the same", sameHashcode, is(true)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }

    @Test
    public void testDifferent() throws Exception {
        // ARRANGE
        Connector connector = ImmutableConnector.builder()
                .id("connector1")
                .endpoint("endpoint")
                .password("".getBytes())
                .build();

        Connector anotherConnector = ImmutableConnector.builder()
                .id("connector2")
                .endpoint("endpoint")
                .password("".getBytes())
                .build();

        // ACT
        boolean equal = connector.equals(anotherConnector);

        // ASSERT
        assertThat("Expecting connectors with different ids to be different", equal, is(false));
    }

    @Test
    public void testPasswordNotExposed() throws Exception {
        // ACT
        String connectorAsString = connector.toString();

        assertThat(connectorAsString, not(containsString("connectorAsString")));
    }
}