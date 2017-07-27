/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import com.salesforce.pyplyn.model.ThresholdType;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
        ThresholdType successAlertType = success.alertType();
        ThresholdType failureAlertType = failure.alertType();

        // ASSERT
        assertThat(successAlertType, equalTo(ThresholdType.LESS_THAN));
        assertThat(failureAlertType, equalTo(ThresholdType.GREATER_THAN));
    }
}