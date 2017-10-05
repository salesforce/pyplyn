/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.standard;

import static com.salesforce.pyplyn.model.StatusCode.*;
import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.*;

/**
 * Applies the specified thresholds on the {@link Transmutation}'s value, and returns
 *   a new result with the value corresponding to the ones defined in {@link StatusCode}:
 * <p/>- 0=OK
 * <p/>- 1=INFO
 * <p/>- 2=WARN
 * <p/>- 3=CRIT (critical)
 * <p/>
 * <p/>Note: values are evaluated from most to least critical and the first match will be returned
 *   (in other words, the comparisons are always evaluated with "greater or lower than or equal to")
 * <p/>
 * <p/>Note 2: The Critical/Warning/Info thresholds can be left null, in which case the plugin will return 0 by default (OK).
 * <p/>Use this functionality when you want to disable a specific metric, but still want to keep displaying its value,
 *    with the help of the {@link HighestValue} plugin.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableThreshold.class)
@JsonSerialize(as = ImmutableThreshold.class)
@JsonTypeName("Threshold")
public abstract class Threshold implements Transform {
    private static final long serialVersionUID = 1883668176362666986L;
    private static final String MESSAGE_TEMPLATE = "%s threshold hit by %s, with value=%s %s %.2f";

    /**
     * Leave null/unspecified to apply to all results
     */
    @Nullable
    public abstract String applyToMetricName();

    /**
     * Critical threshold
     */
    @Nullable
    public abstract Double criticalThreshold();

    /**
     * Warning threshold
     */
    @Nullable
    public abstract Double warningThreshold();

    /**
     * Info threshold
     */
    @Nullable
    public abstract Double infoThreshold();

    /**
     * Decides if the values matched against the specified thresholds, should be greater or lower
     */
    @Nullable
    public abstract ThresholdType type();

    /**
     * Applies this transformation and returns a new {@link Transmutation} matrix
     */
    @Override
    public List<List<Transmutation>> apply(List<List<Transmutation>> input) {
        return input.stream()
                .map(metrics -> metrics.stream()

                        // applies threshold on all ExtractResult objects
                        .map(this::applyThreshold)

                        .collect(Collectors.toList())
                ).collect(Collectors.toList());
    }

    /**
     * Buckets the passed value according to the correct threshold
     *   returns an updated result object
     *
     * @param result the result object to analyze
     * @return
     */
    Transmutation applyThreshold(Transmutation result) {
        // if this transform should apply only to a specific id,
        //   check against passed transform result and return unchanged if it doesn't matches
        if (nonNull(applyToMetricName()) && !applyToMetricName().equals(result.name())) {
            return result;
        }

        // if type is not specified, transform to OK
        if (isNull(type())) {
            return changeValue(result, OK.value());
        }

        // otherwise compare value to all thresholds and determine it's status
        //   if any threshold is not specified (null), it will be ignored
        //   if all thresholds are not specified, the result will be transformed to OK
        Number value = result.value();
        if (type().matches(value, criticalThreshold())) {
            return appendMessage(changeValue(result, CRIT.value()), CRIT.code(), criticalThreshold());

        } else if (type().matches(value, warningThreshold())) {
            return appendMessage(changeValue(result, WARN.value()), WARN.code(), warningThreshold());

        } else if (type().matches(value, infoThreshold())) {
            return appendMessage(changeValue(result, INFO.value()), INFO.code(), infoThreshold());

        } else {
            return changeValue(result, OK.value());
        }
    }


    /**
     * Changes the result's value
     */
    public static Transmutation changeValue(Transmutation result, Double value) {
        return ImmutableTransmutation.builder().from(result).value(value).build();
    }

    /**
     * Appends a message with the explanation of what threshold was hit
     */
    Transmutation appendMessage(Transmutation result, String code, Double threshold) {
        String thresholdHitAlert = String.format(MESSAGE_TEMPLATE,
                code,
                result.name(),
                formatNumber(result.originalValue()),
                type().name(),
                threshold);

        return ImmutableTransmutation.builder().from(result)
                .metadata(ImmutableTransmutation.Metadata.builder()
                        .from(result.metadata())
                        .addMessages(thresholdHitAlert)
                        .build())
                .build();
    }
}
