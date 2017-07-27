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
import com.salesforce.pyplyn.duct.etl.load.refocus.ImmutableRefocus;
import com.salesforce.pyplyn.duct.etl.load.refocus.Refocus;
import com.salesforce.pyplyn.duct.etl.load.refocus.RefocusLoadProcessor;
import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.Transmutation;
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

public class DuctLoadTest {
    private static final String ACTUAL_NAME = "TransformationName";

    @Mock
    private ShutdownHook shutdownHook;

    private AppBootstrapFixtures fixtures;
    private Transmutation result;

    @BeforeMethod
    public void setUp() throws Exception {
        //ARRANGE
        MockitoAnnotations.initMocks(this);
        Number num = 1;

        Transmutation.Metadata metadata = ImmutableTransmutation.Metadata.builder().build();
        result = ImmutableTransmutation.of(ZonedDateTime.now(),  ACTUAL_NAME, num, num, metadata);

        fixtures = new AppBootstrapFixtures().initializeFixtures();
    }

    @Test
    public void processRefocus() throws Exception {
        //ARRANGE
        @SuppressWarnings("unchecked")
        RefocusLoadProcessor refocusLoadProcessor = spy(new RefocusLoadProcessor(fixtures.appConnectors(), shutdownHook));
        Refocus refocus = ImmutableRefocus.of("endpoint", "subject", "aspect",
                "defaultMessageCode", "defaultMessageBody", Collections.emptyList());
        Boolean boolVal = Boolean.TRUE;

        //ACT
        doReturn(Collections.singletonList(boolVal)).when(refocusLoadProcessor).process(Collections.singletonList(any()), Collections.singletonList(any()));
        List<Boolean> results = refocusLoadProcessor.process(Collections.singletonList(result), Collections.singletonList(refocus));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), is(true));
    }
}
