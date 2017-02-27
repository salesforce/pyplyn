/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct;

import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.duct.etl.load.refocus.Refocus;
import com.salesforce.pyplyn.duct.etl.load.refocus.RefocusLoadProcessor;
import com.salesforce.pyplyn.duct.providers.client.RemoteClientFactory;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.refocus.RefocusClient;
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
	
	private TransformationResult result;
	private static final String ACTUAL_NAME = "TransformationName";

	@Mock
	private RemoteClientFactory<RefocusClient> remoteClientFactory;

	@Mock
	private ShutdownHook shutdownHook;

	@BeforeMethod
    public void setUp() throws Exception {
        //ARRANGE
		MockitoAnnotations.initMocks(this);
		Number num = 1;
        result = new TransformationResult(ZonedDateTime.now(), ACTUAL_NAME, num, num);
    }
	
	@Test
	public void processRefocus() throws Exception {
		//ARRANGE
		@SuppressWarnings("unchecked")
		RefocusLoadProcessor refocusLoadProcessor = spy(new RefocusLoadProcessor(remoteClientFactory, shutdownHook));
		Refocus refocus = new Refocus("endpoint", "subject", "aspect",
				"defaultMessageCode", "defaultMessageBody", null);
		Boolean boolVal = Boolean.TRUE;
		
		//ACT
        doReturn(Collections.singletonList(boolVal)).when(refocusLoadProcessor).process(Collections.singletonList(any()), Collections.singletonList(any()));
        List<Boolean> results = refocusLoadProcessor.process(Collections.singletonList(result), Collections.singletonList(refocus));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), is(true));
	}
}
