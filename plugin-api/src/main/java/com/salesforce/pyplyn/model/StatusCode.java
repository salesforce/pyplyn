package com.salesforce.pyplyn.model;

/**
 * Predefined codes for OK/INFO/WARN/ERR statuses
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
public enum StatusCode {
    OK(0, "OK"),
    INFO(1, "INFO"),
    WARN(2, "WARN"),
    ERR(3, "ERR");

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
