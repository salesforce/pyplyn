/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.processor;

import com.codahale.metrics.Meter;
import com.salesforce.pyplyn.model.ExtractImpl;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.status.SystemStatus;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AbstractMeteredExtractProcessorTest {
    @Test
    public void testProcessOneElement() throws Exception {
        // ARRANGE
        AbstractMeteredExtractProcessorImpl processor = new AbstractMeteredExtractProcessorImpl();
        ExtractImpl oneElementToProcess = new ExtractImpl("id");

        // ACT
        List<List<TransformationResult>> processed = processor.execute(oneElementToProcess);
        Optional<TransformationResult> result = processed.stream().flatMap(Collection::stream).findAny();

        // ASSERT
        assertThat("We should have one result", result.isPresent(), is(true));
        assertThat("The result should have its name set to the input value", result.get().name(), is("id"));
    }

    @Test
    public void testProcessNoElementsShouldReturnEmptyList() throws Exception {
        // ARRANGE
        AbstractMeteredExtractProcessorImpl processor = new AbstractMeteredExtractProcessorImpl();

        // ACT
        List<List<TransformationResult>> processed = processor.execute();
        Optional<TransformationResult> result = processed.stream().flatMap(Collection::stream).findAny();

        // ASSERT
        assertThat("Expecting no results", result.isPresent(), is(false));
    }

    @Test
    public void testMetersRetrievedAndMarkIsDelegated() throws Exception {
        // ARRANGE
        SystemStatus systemStatus = mock(SystemStatus.class);

        Meter meter = mock(Meter.class);
        doReturn(meter).when(systemStatus).meter(anyString(), any());

        AbstractMeteredExtractProcessorImpl processor = spy(new AbstractMeteredExtractProcessorImpl());
        processor.setSystemStatus(systemStatus);


        // ACT
        processor.succeeded();
        processor.failed();
        processor.noData();
        processor.authenticationFailure();

        // ASSERT
        verify(processor, times(4)).meterName(); // FindBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT - IGNORE
        verify(systemStatus, times(4)).meter(anyString(), any());
        verify(meter, times(4)).mark();
    }

    /**
     * Implementation class
     */
    private static class AbstractMeteredExtractProcessorImpl extends AbstractMeteredExtractProcessor<ExtractImpl> {
        @Override
        public List<List<TransformationResult>> process(List<ExtractImpl> datasource) {
            return Collections.singletonList(
                    datasource.stream()
                            .map((src) -> {
                                TransformationResult result = mock(TransformationResult.class);
                                doReturn(src.id()).when(result).name();
                                return result;
                            })
                            .collect(Collectors.toList())
            );
        }

        @Override
        protected String meterName() {
            return AbstractMeteredExtractProcessorImpl.class.getSimpleName();
        }

        @Override
        public Class<ExtractImpl> filteredType() {
            return ExtractImpl.class;
        }
    }
}