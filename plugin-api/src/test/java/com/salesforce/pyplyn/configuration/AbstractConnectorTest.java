/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AbstractConnectorTest {
    @Mock
    private AbstractConnector connector;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testProxyEnabledWhenParamsAreDefined() throws Exception {
        // ARRANGE
        doReturn("proxy").when(connector).proxyHost();
        doReturn(10000).when(connector).proxyPort();

        // ACT
        boolean proxyEnabled = connector.isProxyEnabled();

        // ASSERT
        assertThat("Proxy should be enabled", proxyEnabled, equalTo(true));
    }

    @Test
    public void testProxyDisabled() throws Exception {
        // ARRANGE
        doReturn(null).when(connector).proxyHost();
        doReturn(0).when(connector).proxyPort();

        // ACT
        boolean proxyEnabled = connector.isProxyEnabled();

        // ASSERT
        assertThat("Proxy should not be enabled with invalid params", proxyEnabled, equalTo(false));
    }

    @Test
    public void testEquals() throws Exception {
        // ARRANGE
        AbstractConnector connector = new AbstractConnectorImpl("connector1");
        AbstractConnector anotherConnector = new AbstractConnectorImpl("connector1");

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
        AbstractConnector connector = new AbstractConnectorImpl("connector1");
        AbstractConnector anotherConnector = new AbstractConnectorImpl("connector2");

        // ACT
        boolean equal = connector.equals(anotherConnector);

        // ASSERT
        assertThat("Expecting connectors with different ids to be different", equal, is(false));
    }
}