/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.pyplyn.model.ETLMetadata;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumberFiveCharLimit;

/**
 * Returns the most critical (highest value) result by value
 * <p/>
 * <p/>This transformation is generally used in combination with the {@link Threshold}
 *   plugin which buckets values according to their severity (most severe have a higher value).
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class HighestValue implements Transform, Serializable {
    private static final long serialVersionUID = 5858149783326921054L;
    private static final String ORIGINAL_TIME_TEMPLATE = "Original time: %s";

    @JsonProperty("messageCodeSource")
    private Display tagMessageCode;

    @JsonProperty
    private Display tagMessageBody;


    /**
     * Default constructor
     * @param tagMessageCode if specified, will decide what to set in the {@link TransformationResult}'s
     *   {@link ETLMetadata}
     */
    @JsonCreator
    public HighestValue(@JsonProperty("tagMessageCode") Display tagMessageCode,
                        @JsonProperty("tagMessageBody") Display tagMessageBody) {
        this.tagMessageCode = tagMessageCode;
        this.tagMessageBody = tagMessageBody;
    }


    /**
     * Applies this transformation and returns a new {@link TransformationResult} matrix
     */
    @Override
    public List<List<TransformationResult>> apply(List<List<TransformationResult>> stageInput) {
        final List<TransformationResult> stageResult = new ArrayList<>();

        // find highest value and if present, return, also setting the message code according to specified rules
        stageInput.stream().flatMap(Collection::stream)
                .max(Comparator.comparing(result -> new BigDecimal(result.value().toString())))
                .ifPresent(transformResultStage -> stageResult.add(processMetadata(transformResultStage)));

        return Collections.singletonList(stageResult);
    }

    /**
     * Sets the message code according to the rule specified in the <b>messageCodeSource</b> parameter
     *   or returns the unchanged result if no rule is specified
     */
    private TransformationResult processMetadata(TransformationResult result) {
        TransformationResultBuilder builder = new TransformationResultBuilder(result);

        if (tagMessageCode == Display.ORIGINAL_VALUE) {
            applyOriginalValue(builder, result.originalValue());
        }

        if (tagMessageBody == Display.ORIGINAL_TIMESTAMP) {
            addDateToMessageBody(builder, result.time());
        }

        return builder.build();
    }

    /**
     * Holds different options for setting {@link TransformationResult} metadata
     */
    public enum Display{
        ORIGINAL_VALUE,
        ORIGINAL_TIMESTAMP;
    }

    /**
     * Creates a five character message code and sets it in the results's metadata
     */
    private void applyOriginalValue(TransformationResultBuilder builder, Number originalValue) {
        final String messageCode = formatNumberFiveCharLimit(originalValue);
        builder.metadata((metadata) -> metadata.setMessageCode(messageCode));
    }

    /**
     * Tags the result with the original metric's timestamp
     */
    private void addDateToMessageBody(TransformationResultBuilder builder, ZonedDateTime time) {
        String originalDateTime = String.format(ORIGINAL_TIME_TEMPLATE, time.toString());
        builder.metadata((metadata) -> metadata.addMessage(originalDateTime));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HighestValue that = (HighestValue) o;

        if (tagMessageCode != that.tagMessageCode) return false;
        return tagMessageBody == that.tagMessageBody;
    }

    @Override
    public int hashCode() {
        int result = tagMessageCode != null ? tagMessageCode.hashCode() : 0;
        result = 31 * result + (tagMessageBody != null ? tagMessageBody.hashCode() : 0);
        return result;
    }
}
