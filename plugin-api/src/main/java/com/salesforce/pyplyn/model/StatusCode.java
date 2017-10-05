package com.salesforce.pyplyn.model;

/**
 * Predefined codes for OK/INFO/WARN/CRIT statuses
 * <p/>
 * <p/> Use <code>UNKNOWN</code> for any situations where a corresponding status code could not be found
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public enum StatusCode {
    OK(0, "OK"),
    INFO(1, "INFO"),
    WARN(2, "WARN"),
    CRIT(3, "CRIT"),
    UNKNOWN(999, "UNKNOWN");

    private final long value;
    private final String code;

    StatusCode(long val, String code) {
        this.value = val;
        this.code = code;
    }

    public double value() {
        return value;
    }

    public String code() {
        return code;
    }
}
