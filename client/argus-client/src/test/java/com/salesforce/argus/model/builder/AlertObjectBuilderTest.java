/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import org.testng.annotations.*;

import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class AlertObjectBuilderTest {
    private AlertObjectBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        builder = defaultAlertObjectBuilder();
    }

    @Test
    public void testBuild() throws Exception {
        // ARRANGE

        // ACT

        // ASSERT

    }

    /**
     * Initializes a default alert object builder
     */
    public static AlertObjectBuilder defaultAlertObjectBuilder() {
        return new AlertObjectBuilder()
                .withId(1L)
                .withCreatedById(2L)
                .withCreatedDate(3L)
                .withModifiedById(4L)
                .withModifiedDate(5L)
                .withName("name")
                .withExpression("expression")
                .withCronEntry("cronEntry")
                .withEnabled(true)
                .withMissingDataNotificationEnabled(true)
                .withNotificationsIds(new Long[]{6L})
                .withTriggersIds(new Long[]{7L})
                .withOwnerName("ownerName")
                .withShared(true);
    }
}