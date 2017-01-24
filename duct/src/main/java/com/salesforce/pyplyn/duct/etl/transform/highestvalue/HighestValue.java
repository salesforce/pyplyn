/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.highestvalue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumberFiveCharLimit;
import static java.util.Objects.nonNull;

/**
 * Returns the most critical (highest value) result by value
 * <p/>
 * <p/>This transformation is generally used in combination with the {@link com.salesforce.pyplyn.duct.etl.transform.threshold.Threshold}
 *   plugin which buckets values according to their severity (most severe have a higher value).
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class HighestValue implements Transform, Serializable {
    private static final long serialVersionUID = 5858149783326921054L;

    @JsonProperty
    private Display messageCodeSource;


    @Override
    public List<List<TransformationResult>> apply(List<List<TransformationResult>> stageInput) {
        final List<TransformationResult> stageResult = new ArrayList<>();

        // find highest value and if present, return, also setting the message code according to specified rules
        stageInput.stream().flatMap(Collection::stream)
                .max(Comparator.comparing(result -> new BigDecimal(result.value().toString())))
                .ifPresent(transformResultStage -> stageResult.add(setMessageCode(transformResultStage)));

        return Collections.singletonList(stageResult);
    }

    /**
     * Sets the message code according to the rule specified in the <b>messageCodeSource</b> parameter
     *   or returns the unchanged result if no rule is specified
     */
    private TransformationResult setMessageCode(TransformationResult result) {
        if (messageCodeSource == Display.ORIGINAL_VALUE) {
            return createCodeFromOriginalValue(result);
        }

        return result;
    }

    /**
     * Holds different options for setting the <b>defaultMessageCode</b> on the {@link TransformationResult}
     */
    private enum Display{
        ORIGINAL_VALUE
    }

    /**
     * Creates a five character message code and sets it in the results's metadata
     */
    private TransformationResult createCodeFromOriginalValue(TransformationResult input) {
        final String messageCode = formatNumberFiveCharLimit(input.originalValue());
        return new TransformationResultBuilder(input)
                .metadata((metadata) -> metadata.setMessageCode(messageCode))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HighestValue that = (HighestValue) o;

        return messageCodeSource == that.messageCodeSource;
    }

    @Override
    public int hashCode() {
        return nonNull(messageCodeSource) ? messageCodeSource.hashCode() : 0;
    }
}
