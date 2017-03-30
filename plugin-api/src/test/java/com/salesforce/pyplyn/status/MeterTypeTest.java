/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class MeterTypeTest {
    @Test
    public void testMeterHasSetAlertLevel() throws Exception {
        // ARRANGE
        MeterType success = MeterType.ExtractSuccess;
        MeterType failure = MeterType.ExtractFailure;


        // ACT
        AlertType successAlertType = success.alertType();
        AlertType failureAlertType = failure.alertType();

        // ASSERT
        assertThat(successAlertType, equalTo(AlertType.LESS_THAN));
        assertThat(failureAlertType, equalTo(AlertType.GREATER_THAN));
    }
}