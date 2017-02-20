/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.AlertObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test class
 * <p/>Ensures that the {@link AlertObjectBuilder} functions as expected (allows composition of fields and generates
 *  an immutable {@link AlertObject} object when {@link AlertObjectBuilder#build()} is called
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
    public void testExpectedFields() throws Exception {
        // ARRANGE
        AlertObject alertObject = builder.build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(alertObject);
    }

    @Test
    public void testExpectedFieldsUsingTheCopyConstructor() throws Exception {
        // ARRANGE
        AlertObject alertObject = new AlertObjectBuilder(builder.build()).build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(alertObject);
    }

    /**
     * Runs all the assertions
     */
    private void assertAllExpectedFieldValues(AlertObject alertObject) {
        assertThat(alertObject.id(), equalTo(1L));
        assertThat(alertObject.createdById(), equalTo(2L));
        assertThat(alertObject.createdDate(), equalTo(3L));
        assertThat(alertObject.modifiedById(), equalTo(4L));
        assertThat(alertObject.modifiedDate(), equalTo(5L));
        assertThat(alertObject.name(), equalTo("name"));
        assertThat(alertObject.expression(), equalTo("expression"));
        assertThat(alertObject.cronEntry(), equalTo("cronEntry"));
        assertThat(alertObject.isEnabled(), equalTo(true));
        assertThat(alertObject.isMissingDataNotificationEnabled(), equalTo(true));
        assertThat(alertObject.notificationsIds(), equalTo(new Long[]{6L}));
        assertThat(alertObject.triggersIds(), equalTo(new Long[]{7L}));
        assertThat(alertObject.ownerName(), equalTo("ownerName"));
        assertThat(alertObject.isShared(), equalTo(true));
    }

    /**
     * Initializes a default object builder
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