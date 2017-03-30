/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.TriggerObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 * <p/>Ensures that the {@link TriggerObjectBuilder} functions as expected (allows composition of fields and generates
 *  an immutable {@link TriggerObject} object when {@link TriggerObjectBuilder#build()} is called
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class TriggerObjectBuilderTest {
    private TriggerObjectBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        builder = defaultTriggerObjectBuilder();
    }

    @Test
    public void testExpectedFields() throws Exception {
        // ARRANGE
        TriggerObject triggerObject = builder.build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(triggerObject);
    }

    @Test
    public void testExpectedFieldsUsingTheCopyConstructor() throws Exception {
        // ARRANGE
        TriggerObject triggerObject = new TriggerObjectBuilder(builder.build()).build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(triggerObject);
    }

    /**
     * Runs all the assertions
     */
    private void assertAllExpectedFieldValues(TriggerObject triggerObject) {
        assertThat(triggerObject.id(), equalTo(1L));
        assertThat(triggerObject.createdById(), equalTo(2L));
        assertThat(triggerObject.createdDate(), equalTo(3L));
        assertThat(triggerObject.modifiedById(), equalTo(4L));
        assertThat(triggerObject.modifiedDate(), equalTo(5L));
        assertThat(triggerObject.type(), equalTo("type"));
        assertThat(triggerObject.name(), equalTo("name"));
        assertThat(triggerObject.threshold(), equalTo(6d));
        assertThat(triggerObject.secondaryThreshold(), equalTo(7d));
        assertThat(triggerObject.inertia(), equalTo(8L));
        assertThat(triggerObject.alertId(), equalTo(9L));
        assertThat(triggerObject.notificationIds(), equalTo(new Long[]{10L}));
    }

    /**
     * Initializes a default object builder
     */
    public static TriggerObjectBuilder defaultTriggerObjectBuilder() {
        return new TriggerObjectBuilder()
                .withId(1L)
                .withCreatedById(2L)
                .withCreatedDate(3L)
                .withModifiedById(4L)
                .withModifiedDate(5L)
                .withType("type")
                .withName("name")
                .withThreshold(6d)
                .withSecondaryThreshold(7d)
                .withInertia(8L)
                .withAlertId(9L)
                .withNotificationIds(new Long[]{10L});
    }
}