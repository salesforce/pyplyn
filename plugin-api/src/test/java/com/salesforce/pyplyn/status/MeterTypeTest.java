/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.testng.annotations.Test;

import com.salesforce.pyplyn.model.ThresholdType;

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
        MeterType success = new MeterType(ThresholdType.GREATER_THAN, ProcessStatus.ExtractSuccess);
        MeterType failure = new MeterType(ThresholdType.LESS_THAN, ProcessStatus.ExtractFailure);


        // ACT
        ThresholdType successAlertType = success.alertType();
        ThresholdType failureAlertType = failure.alertType();

        // ASSERT
        assertThat(successAlertType, equalTo(ThresholdType.GREATER_THAN));
        assertThat(failureAlertType, equalTo(ThresholdType.LESS_THAN));
    }
}