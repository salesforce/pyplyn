/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.NotificationObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 * <p/>Ensures that the {@link NotificationObjectBuilder} functions as expected (allows composition of fields and generates
 *  an immutable {@link NotificationObject} object when {@link NotificationObjectBuilder#build()} is called
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class NotificationObjectBuilderTest {
    private NotificationObjectBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        builder = defaultNotificationObjectBuilder();
    }

    @Test
    public void testExpectedFields() throws Exception {
        // ARRANGE
        NotificationObject alertObject = builder.build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(alertObject);
    }

    @Test
    public void testExpectedFieldsUsingTheCopyConstructor() throws Exception {
        // ARRANGE
        NotificationObject alertObject = new NotificationObjectBuilder(builder.build()).build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(alertObject);
    }

    /**
     * Runs all the assertions
     */
    private void assertAllExpectedFieldValues(NotificationObject dashboardObject) {
        assertThat(dashboardObject.id(), equalTo(1L));
        assertThat(dashboardObject.createdById(), equalTo(2L));
        assertThat(dashboardObject.createdDate(), equalTo(3L));
        assertThat(dashboardObject.modifiedById(), equalTo(4L));
        assertThat(dashboardObject.modifiedDate(), equalTo(5L));
        assertThat(dashboardObject.name(), equalTo("name"));
        assertThat(dashboardObject.notifierName(), equalTo("notifierName"));
        assertThat(dashboardObject.subscriptions(), equalTo(new String[]{"subscriptions"}));
        assertThat(dashboardObject.metricsToAnnotate(), equalTo(new String[]{"metricsToAnnotate"}));
        assertThat(dashboardObject.cooldownPeriod(), equalTo(6L));
        assertThat(dashboardObject.cooldownExpiration(), equalTo(7L));
        assertThat(dashboardObject.triggerIds(), equalTo(new Long[]{8L}));
        assertThat(dashboardObject.alertId(), equalTo(9L));
        assertThat(dashboardObject.customText(), equalTo("customText"));
        assertThat(dashboardObject.isSRactionable(), equalTo(true));
    }

    /**
     * Initializes a default object builder
     */
    public static NotificationObjectBuilder defaultNotificationObjectBuilder() {
        return new NotificationObjectBuilder()
                .withId(1L)
                .withCreatedById(2L)
                .withCreatedDate(3L)
                .withModifiedById(4L)
                .withModifiedDate(5L)
                .withName("name")
                .withNotifierName("notifierName")
                .withSubscriptions(new String[]{"subscriptions"})
                .withMetricsToAnnotate(new String[]{"metricsToAnnotate"})
                .withCooldownPeriod(6L)
                .withCooldownExpiration(7L)
                .withTriggerIds(new Long[]{8L})
                .withAlertId(9L)
                .withCustomText("customText")
                .withSRactionable(true);
    }
}