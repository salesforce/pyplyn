/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.threshold;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.pyplyn.duct.etl.transform.highestvalue.HighestValue;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import static com.salesforce.pyplyn.duct.etl.transform.threshold.Threshold.Value.*;
import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Applies the specified thresholds on the {@link TransformationResult}'s value, and returns
 *   a new result with the value between the [0-3] interval:
 * <p/>- 0=OK
 * <p/>- 1=INFO
 * <p/>- 2=WARN
 * <p/>- 3=CRIT
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
public class Threshold implements Transform, Serializable {
    private static final long serialVersionUID = 1883668176362666986L;
    private final static String MESSAGE_TEMPLATE = "%s threshold hit by %s, with value=%s %s %.2f";


    @JsonProperty
    String applyToMetricName;

    @JsonProperty
    Double criticalThreshold;

    @JsonProperty
    Double warningThreshold;

    @JsonProperty
    Double infoThreshold;

    @JsonProperty
    Type type;


    /**
     * Default constructor
     *
     * @param applyToMetricName Leave null/unspecified to apply to all results
     * @param criticalThreshold Critical threshold
     * @param warningThreshold Warning threshold
     * @param infoThreshold Info threshold
     * @param type Decides if the values matched against the specified thresholds, should be greater or lower
     */
    @JsonCreator
    public Threshold(@JsonProperty("applyToMetricName") String applyToMetricName,
                     @JsonProperty("criticalThreshold") Double criticalThreshold,
                     @JsonProperty("warningThreshold") Double warningThreshold,
                     @JsonProperty("infoThreshold") Double infoThreshold,
                     @JsonProperty("type") Type type) {
        this.applyToMetricName = applyToMetricName;
        this.criticalThreshold = criticalThreshold;
        this.warningThreshold = warningThreshold;
        this.infoThreshold = infoThreshold;
        this.type = type;
    }

    /**
     * Applies this transformation and returns a new {@link TransformationResult} matrix
     */
    @Override
    public List<List<TransformationResult>> apply(List<List<TransformationResult>> input) {
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
    TransformationResult applyThreshold(TransformationResult result) {
        // if this transform should apply only to a specific id,
        //   check against passed transform result and return unchanged if it doesn't matches
        if (nonNull(applyToMetricName) && !applyToMetricName.equals(result.name())) {
            return result;
        }

        // if type is not specified, transform to OK
        if (isNull(type)) {
            return changeValue(result, OK.value());
        }

        // otherwise compare value to all thresholds and determine it's status
        //   if any threshold is not specified (null), it will be ignored
        //   if all thresholds are not specified, the result will be transformed to OK
        Number value = result.value();
        if (type.matches(value, criticalThreshold)) {
            return appendMessage(changeValue(result, CRIT.value()), CRIT.code(), criticalThreshold);

        } else if (type.matches(value, warningThreshold)) {
            return appendMessage(changeValue(result, WARN.value()), WARN.code(), warningThreshold);

        } else if (type.matches(value, infoThreshold)) {
            return appendMessage(changeValue(result, INFO.value()), INFO.code(), infoThreshold);

        } else {
            return changeValue(result, OK.value());
        }
    }


    /**
     * Changes the result's value
     */
    public static TransformationResult changeValue(TransformationResult result, Double value) {
        return new TransformationResultBuilder(result)
                .withValue(value)
                .build();
    }

    /**
     * Appends a message with the explanation of what threshold was hit
     */
    TransformationResult appendMessage(TransformationResult result, String code, Double threshold) {
        String thresholdHitAlert = String.format(MESSAGE_TEMPLATE,
                code,
                result.name(),
                formatNumber(result.originalValue()),
                type.name(),
                threshold);

        return new TransformationResultBuilder(result)
                .metadata((metadata) -> metadata.addMessage(thresholdHitAlert))
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

        Threshold threshold = (Threshold) o;

        if (applyToMetricName != null ? !applyToMetricName.equals(threshold.applyToMetricName) : threshold.applyToMetricName != null)
            return false;
        if (criticalThreshold != null ? !criticalThreshold.equals(threshold.criticalThreshold) : threshold.criticalThreshold != null)
            return false;
        if (warningThreshold != null ? !warningThreshold.equals(threshold.warningThreshold) : threshold.warningThreshold != null)
            return false;
        if (infoThreshold != null ? !infoThreshold.equals(threshold.infoThreshold) : threshold.infoThreshold != null)
            return false;
        return type == threshold.type;
    }

    @Override
    public int hashCode() {
        int result = applyToMetricName != null ? applyToMetricName.hashCode() : 0;
        result = 31 * result + (criticalThreshold != null ? criticalThreshold.hashCode() : 0);
        result = 31 * result + (warningThreshold != null ? warningThreshold.hashCode() : 0);
        result = 31 * result + (infoThreshold != null ? infoThreshold.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }


    /**
     * Possible trigger types
     */
    public enum Type {
        GREATER_THAN, LESS_THAN;

        /**
         * Determines if a compared value is higher/lower than a specified threshold
         *
         * @return true if the comparison succeeds, or false if the passed threshold is null
         */
        public boolean matches(Number compared, Double threshold) {
            if (nonNull(threshold) && this == GREATER_THAN) {
                return compared.doubleValue() >= threshold;
            } else if (nonNull(threshold) && this == LESS_THAN) {
                return compared.doubleValue() <= threshold;
            }

            return false;
        }
    }

    /**
     * Predefined values for OK/INFO/WARN/ERR
     */
    public enum Value {
        OK(0d, "OK"),
        INFO(1d, "INFO"),
        WARN(2d, "WARN"),
        CRIT(3d, "CRIT");

        private final Double value;
        private final String code;

        Value(double val, String code) {
            this.value = val;
            this.code = code;
        }

        public Double value() {
            return value;
        }

        public String code() {
            return code;
        }
    }
}
