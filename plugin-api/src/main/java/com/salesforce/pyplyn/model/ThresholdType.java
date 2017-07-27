package com.salesforce.pyplyn.model;

import static java.util.Objects.nonNull;

/**
 * Possible threshold trigger types
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public enum ThresholdType {
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
