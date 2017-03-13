/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.duct.etl.transform.highestvalue.HighestValue;
import com.salesforce.pyplyn.duct.etl.transform.inertiathreshold.InertiaThreshold;
import com.salesforce.pyplyn.duct.etl.transform.infostatus.InfoStatus;
import com.salesforce.pyplyn.duct.etl.transform.lastdatapoint.LastDatapoint;
import com.salesforce.pyplyn.duct.etl.transform.savemetricmetadata.SaveMetricMetadata;
import com.salesforce.pyplyn.duct.etl.transform.threshold.Threshold;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;

public class DuctTransformTest {

	private TransformationResult result;
	private static final String ACTUAL_NAME = "TransformationName";
	
	@BeforeMethod
    public void setUp() throws Exception {
        //ARRANGE
		MockitoAnnotations.initMocks(this);
		Number num = 1;
        result = new TransformationResult(ZonedDateTime.now(), ACTUAL_NAME, num, num);
    }
	
	@Test
	public void applyHighestValue() throws Exception {
		//ARRANGE
		HighestValue highestValue = spy(new HighestValue());
		TransformationResult transformationResult = new TransformationResultBuilder(result).withName(ACTUAL_NAME).build();
		
		//ACT
        doReturn(Collections.singletonList(Collections.singletonList(result))).when(highestValue).apply(Collections.singletonList(Collections.singletonList(any())));
        List<List<TransformationResult>> results = highestValue.apply(Collections.singletonList(Collections.singletonList(transformationResult)));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), hasSize(1));
        assertThat(results.get(0).get(0).name(), is(ACTUAL_NAME));
	}
	
	@Test
	public void applyInfoStatus() throws Exception {
		//ARRANGE
		InfoStatus infoStatus = spy(new InfoStatus());
		TransformationResult transformationResult = new TransformationResultBuilder(result).withName(ACTUAL_NAME).build();
		
		//ACT
		doReturn(Collections.singletonList(Collections.singletonList(result))).when(infoStatus).apply(Collections.singletonList(Collections.singletonList(any())));
        List<List<TransformationResult>> results = infoStatus.apply(Collections.singletonList(Collections.singletonList(transformationResult)));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), hasSize(1));
        assertThat(results.get(0).get(0).name(), is(ACTUAL_NAME));
		
	}
	
	@Test
	public void applyLastDataPoint() throws Exception {
		//ARRANGE
		LastDatapoint lastDatapoint = spy(new LastDatapoint());
		TransformationResult transformationResult = new TransformationResultBuilder(result).withName(ACTUAL_NAME).build();
		
		//ACT
		doReturn(Collections.singletonList(Collections.singletonList(result))).when(lastDatapoint).apply(Collections.singletonList(Collections.singletonList(any())));
        List<List<TransformationResult>> results = lastDatapoint.apply(Collections.singletonList(Collections.singletonList(transformationResult)));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), hasSize(1));
        assertThat(results.get(0).get(0).name(), is(ACTUAL_NAME));
		
	}
	
	@Test
	public void applySaveMetricMetadata() throws Exception {
		//ARRANGE
		SaveMetricMetadata saveMetricMedata = spy(new SaveMetricMetadata());
		TransformationResult transformationResult = new TransformationResultBuilder(result).withName(ACTUAL_NAME).build();
		
		//ACT
		doReturn(Collections.singletonList(Collections.singletonList(result))).when(saveMetricMedata).apply(Collections.singletonList(Collections.singletonList(any())));
        List<List<TransformationResult>> results = saveMetricMedata.apply(Collections.singletonList(Collections.singletonList(transformationResult)));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), hasSize(1));
        assertThat(results.get(0).get(0).name(), is(ACTUAL_NAME));
		
	}
	
	@Test
	public void applyThreshold() throws Exception {
		//ARRANGE
		Threshold threshold = spy(new Threshold());
		TransformationResult transformationResult = new TransformationResultBuilder(result).withName(ACTUAL_NAME).build();
		
		//ACT
		doReturn(Collections.singletonList(Collections.singletonList(result))).when(threshold).apply(Collections.singletonList(Collections.singletonList(any())));
        List<List<TransformationResult>> results = threshold.apply(Collections.singletonList(Collections.singletonList(transformationResult)));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), hasSize(1));
        assertThat(results.get(0).get(0).name(), is(ACTUAL_NAME));
		
	}
	
	@Test
	public void applyInertiaThreshold() throws Exception{
		InertiaThreshold inertiaThreshold = spy(new InertiaThreshold());
		TransformationResult transformationResult = new TransformationResultBuilder(result).withName(ACTUAL_NAME).build();
		
		//ACT
		doReturn(Collections.singletonList(Collections.singletonList(result))).when(inertiaThreshold).apply(Collections.singletonList(Collections.singletonList(any())));
        List<List<TransformationResult>> results = inertiaThreshold.apply(Collections.singletonList(Collections.singletonList(transformationResult)));

        //ASSERT
        assertThat(results, hasSize(1));
        assertThat(results.get(0), hasSize(1));
        assertThat(results.get(0).get(0).name(), is(ACTUAL_NAME));
		
	}	
}
