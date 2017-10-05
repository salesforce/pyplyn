/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.appconfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.testng.annotations.Test;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class AppConfigFileLoaderTest {

    @Test
    public void testLoadConfigParameter() throws Exception {
        // ARRANGE
        String[] args = new String[]{"--config", "config"};

        // ACT
        String configName = AppConfigFileLoader.loadFromCLI("programName", args);

        // ASSERT
        assertThat(configName, equalTo("config"));
    }

    @Test(expectedExceptions = ConfigParseException.class)
    public void testFailWithoutCorrectParam() throws Exception {
        // ARRANGE
        String[] args = new String[]{"--invalid"};

        // ACT/ASSERT
        AppConfigFileLoader.loadFromCLI("programName", args);
    }
}