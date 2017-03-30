/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model.builder;

import com.salesforce.refocus.model.Aspect;
import com.salesforce.refocus.model.Link;
import com.salesforce.refocus.model.LinkTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 * <p/>Ensures that the {@link AspectBuilder} functions as expected (allows composition of fields and generates
 *  an immutable {@link Aspect} object when {@link AspectBuilder#build()} is called
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class AspectBuilderTest {
    public static final double[] OK_RANGE = new double[]{0d, 0d};
    public static final double[] INFO_RANGE = new double[]{1d, 1d};
    public static final double[] WARN_RANGE = new double[]{2d, 2d};
    public static final double[] CRIT_RANGE = new double[]{3d, 3d};
    public static final double[] OTHER_RANGE = new double[]{9d, 9d};
    public static final Link LINK = LinkTest.defaultLink();

    private AspectBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        builder = defaultAspectBuilder();
    }

    @Test
    public void testExpectedFields() throws Exception {
        // ACT
        Aspect aspect = builder.build();

        // ASSERT
        assertThat(aspect.name(), equalTo("name"));
        assertThat(aspect.description(), equalTo("description"));
        assertThat(aspect.isPublished(), equalTo(true));
        assertThat(aspect.criticalRange(), equalTo(CRIT_RANGE));
        assertThat(aspect.warningRange(), equalTo(WARN_RANGE));
        assertThat(aspect.infoRange(), equalTo(INFO_RANGE));
        assertThat(aspect.okRange(), equalTo(OK_RANGE));
        assertThat(aspect.timeout(), equalTo("timeout"));
        assertThat(aspect.valueType(), equalTo("valueType"));
        assertThat(aspect.tags(), equalTo(Collections.singletonList("tags")));
        assertThat(aspect.relatedLinks(), equalTo(Collections.singletonList(LINK)));
    }

    @Test
    public void testCompositionByReplacingAllFields() throws Exception {
        // ARRANGE
        Aspect originalAspect = builder.build();
        builder = new AspectBuilder(originalAspect)
                .withId("idx")
                .withName("namex")
                .withDescription("descriptionx")
                .withHelpEmail("helpEmailx")
                .withPublished(false)
                .withCriticalRange(OTHER_RANGE)
                .withWarningRange(OTHER_RANGE)
                .withInfoRange(OTHER_RANGE)
                .withOkRange(OTHER_RANGE)
                .withTimeout("timeoutx")
                .withValueType("valueTypex")
                .withTags(null)
                .withRelatedLinks(null);

        // ACT
        Aspect aspect = builder.build();

        // ASSERT
        assertThat(aspect.name(), not(equalTo("name")));
        assertThat(aspect.description(), not(equalTo("description")));
        assertThat(aspect.isPublished(), not(equalTo(true)));
        assertThat(aspect.criticalRange(), not(equalTo(CRIT_RANGE)));
        assertThat(aspect.warningRange(), not(equalTo(WARN_RANGE)));
        assertThat(aspect.infoRange(), not(equalTo(INFO_RANGE)));
        assertThat(aspect.okRange(), not(equalTo(OK_RANGE)));
        assertThat(aspect.timeout(), not(equalTo("timeout")));
        assertThat(aspect.valueType(), not(equalTo("valueType")));
        assertThat(aspect.tags(), not(equalTo(Collections.singletonList("tags"))));
        assertThat(aspect.relatedLinks(), not(equalTo(Collections.singletonList(LINK))));
    }

    /**
     * Initializes a default aspect builder
     * @return
     */
    public static AspectBuilder defaultAspectBuilder() {
        return new AspectBuilder()
                .withId("id")
                .withName("name")
                .withDescription("description")
                .withHelpEmail("helpEmail")
                .withPublished(true)
                .withCriticalRange(CRIT_RANGE)
                .withWarningRange(WARN_RANGE)
                .withInfoRange(INFO_RANGE)
                .withOkRange(OK_RANGE)
                .withTimeout("timeout")
                .withValueType("valueType")
                .withTags(Collections.singletonList("tags"))
                .withRelatedLinks(Collections.singletonList(LINK));
    }
}