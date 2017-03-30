/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model.builder;

import com.salesforce.argus.model.DashboardObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 * <p/>Ensures that the {@link DashboardObjectBuilder} functions as expected (allows composition of fields and generates
 *  an immutable {@link DashboardObject} object when {@link DashboardObjectBuilder#build()} is called
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class DashboardObjectBuilderTest {
    private DashboardObjectBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        builder = defaultDashboardObjectBuilder();
    }

    @Test
    public void testExpectedFields() throws Exception {
        // ARRANGE
        DashboardObject alertObject = builder.build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(alertObject);
    }

    @Test
    public void testExpectedFieldsUsingTheCopyConstructor() throws Exception {
        // ARRANGE
        DashboardObject alertObject = new DashboardObjectBuilder(builder.build()).build();

        // ACT/ASSERT
        assertAllExpectedFieldValues(alertObject);
    }

    /**
     * Runs all the assertions
     */
    private void assertAllExpectedFieldValues(DashboardObject dashboardObject) {
        assertThat(dashboardObject.id(), equalTo(1L));
        assertThat(dashboardObject.createdById(), equalTo(2L));
        assertThat(dashboardObject.createdDate(), equalTo(3L));
        assertThat(dashboardObject.modifiedById(), equalTo(4L));
        assertThat(dashboardObject.modifiedDate(), equalTo(5L));
        assertThat(dashboardObject.name(), equalTo("name"));
        assertThat(dashboardObject.content(), equalTo("content"));
        assertThat(dashboardObject.ownerName(), equalTo("ownerName"));
        assertThat(dashboardObject.isShared(), equalTo(true));
        assertThat(dashboardObject.description(), equalTo("description"));
    }

    /**
     * Initializes a default object builder
     */
    public static DashboardObjectBuilder defaultDashboardObjectBuilder() {
        return new DashboardObjectBuilder()
                .withId(1L)
                .withCreatedById(2L)
                .withCreatedDate(3L)
                .withModifiedById(4L)
                .withModifiedDate(5L)
                .withName("name")
                .withContent("content")
                .withOwnerName("ownerName")
                .withShared(true)
                .withDescription("description");
    }
}