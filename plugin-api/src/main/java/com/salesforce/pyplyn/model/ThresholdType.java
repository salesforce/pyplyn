package com.salesforce.pyplyn.model;

import static java.util.Objects.nonNull;

/**
 * Possible threshold trigger types
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 *
 */
public enum ThresholdType {
    GREATER_THAN, LESS_THAN, EQUAL_TO, GREATER_THAN_OR_EQ, LESS_THAN_OR_EQ;

    /**
     * A small tolerance by which the difference between two numbers (doubles) is negligible
     */
    public final double EPSILON = 0.01;

    /**
     * Determines if a compared value is higher than, lower than, or equal to a specified threshold
     *
     * @return true if the comparison succeeds, or false if the passed threshold is null
     */
    public boolean matches(Number compared, Double threshold) {
        if (nonNull(threshold) && this == GREATER_THAN) {
            return compared.doubleValue() > threshold;
        } else if (nonNull(threshold) && this == LESS_THAN) {
            return compared.doubleValue() < threshold;
        } else if (nonNull(threshold) && this == EQUAL_TO) {
            return Math.abs(compared.doubleValue() - threshold) < EPSILON;
        } else if (nonNull(threshold) && this == GREATER_THAN_OR_EQ){
            return compared.doubleValue() > threshold || Math.abs(compared.doubleValue() - threshold) < EPSILON;
        } else if (nonNull(threshold) && this == LESS_THAN_OR_EQ){
            return compared.doubleValue() < threshold || Math.abs(compared.doubleValue() - threshold) < EPSILON;
        }

        return false;
    }
}
