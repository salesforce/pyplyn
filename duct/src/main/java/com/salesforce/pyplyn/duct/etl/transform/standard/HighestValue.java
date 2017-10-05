/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.standard;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumberFiveCharLimit;

import java.math.BigDecimal;
import java.util.*;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.Transmutation;

/**
 * Returns the most critical (highest value) result by value
 * <p/>
 * <p/>This transformation is generally used in combination with the {@link Threshold}
 *   plugin which buckets values according to their severity (most severe have a higher value).
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableHighestValue.class)
@JsonSerialize(as = ImmutableHighestValue.class)
@JsonTypeName("HighestValue")
public abstract class HighestValue implements Transform {
    private static final long serialVersionUID = 5858149783326921054L;
    private static final String ORIGINAL_TIME_TEMPLATE = "Original time: %s";

    /**
     * if specified, will decide what to set in {@link com.salesforce.pyplyn.model.Transmutation.Metadata}
     */
    @Nullable
    @JsonProperty("messageCodeSource")
    public abstract Display tagMessageCode();

    @Nullable
    public abstract Display tagMessageBody();

    /**
     * Applies this transformation and returns a new {@link com.salesforce.pyplyn.model.Transmutation} matrix
     */
    @Override
    public List<List<Transmutation>> apply(List<List<Transmutation>> stageInput) {
        final List<Transmutation> stageResult = new ArrayList<>();

        // find highest value and if present, return, also setting the message code according to specified rules
        stageInput.stream().flatMap(Collection::stream)
                .max(Comparator.comparing(result -> new BigDecimal(result.value().toString())))
                .ifPresent(transformResultStage -> stageResult.add(processMetadata(transformResultStage)));

        return Collections.singletonList(stageResult);
    }

    /**
     * Sets the message code according to the rule specified in the <strong>messageCodeSource</strong> parameter
     *   or returns the unchanged result if no rule is specified
     */
    private Transmutation processMetadata(Transmutation result) {
        ImmutableTransmutation.Builder response = ImmutableTransmutation.builder().from(result);
        ImmutableTransmutation.Metadata.Builder metadata = ImmutableTransmutation.Metadata.builder().from(result.metadata());

        // Creates a five character message code and sets it in the results's metadata
        if (tagMessageCode() == Display.ORIGINAL_VALUE) {
            metadata.messageCode(formatNumberFiveCharLimit(result.originalValue()));
        }

        // Tags the result with the original metric's timestamp
        if (tagMessageBody() == Display.ORIGINAL_TIMESTAMP) {
            metadata.addMessages(String.format(ORIGINAL_TIME_TEMPLATE, result.time().toString()));
        }

        return response.metadata(metadata.build()).build();
    }

    /**
     * Holds different options for setting {@link Transmutation} metadata
     */
    public enum Display{
        ORIGINAL_VALUE,
        ORIGINAL_TIMESTAMP
    }
}
