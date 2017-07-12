/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct;


import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.etl.extract.argus.Argus;
import com.salesforce.pyplyn.duct.etl.extract.argus.ArgusExtractProcessor;
import com.salesforce.pyplyn.duct.etl.extract.refocus.Refocus;
import com.salesforce.pyplyn.duct.etl.extract.refocus.RefocusExtractProcessor;
import com.salesforce.pyplyn.model.TransformationResult;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class DuctExtractTest {
    private static final String ACTUAL_NAME = "TransformationName";

    @Mock
    private ShutdownHook shutdownHook;

    private AppBootstrapFixtures fixtures;
    private TransformationResult result;

    @BeforeMethod
    public void setUp() throws Exception {
        //ARRANGE
        MockitoAnnotations.initMocks(this);
        Number num = 1;
        result = new TransformationResult(ZonedDateTime.now(), ACTUAL_NAME, num, num);

        fixtures = new AppBootstrapFixtures().initializeFixtures();
    }

    @Test
    public void processArgus() throws Exception {
        //ARRANGE
        @SuppressWarnings("unchecked")
        ArgusExtractProcessor argusExtractprocessor = spy(new ArgusExtractProcessor(fixtures.appConnector(), fixtures.cacheFactory(), shutdownHook));
        Argus argus = new Argus("endpoint", "expression", "name", 1, 2d);

        //ACT
        doReturn(Collections.singletonList(Collections.singletonList(result))).when(argusExtractprocessor).process(Collections.singletonList(any()));
        List<List<TransformationResult>> results = argusExtractprocessor.process(Collections.singletonList(argus));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), hasSize(1));
        assertThat(results.get(0).get(0).name(), is(ACTUAL_NAME));
    }

    @Test
    public void processRefocus() throws Exception {
        //ARRANGE
        @SuppressWarnings("unchecked")
        RefocusExtractProcessor refocusExtractProcessor = spy(new RefocusExtractProcessor(fixtures.appConnector(), fixtures.cacheFactory(), shutdownHook));
        Refocus refocus = new Refocus("endpoint", "subject", null, "aspect", 1, 2d);

        //ACT
        doReturn(Collections.singletonList(Collections.singletonList(result))).when(refocusExtractProcessor).process(Collections.singletonList(any()));
        List<List<TransformationResult>> results = refocusExtractProcessor.process(Collections.singletonList(refocus));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), hasSize(1));
        assertThat(results.get(0).get(0).name(), is(ACTUAL_NAME));
    }
}
