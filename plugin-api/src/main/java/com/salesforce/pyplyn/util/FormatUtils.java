/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import com.salesforce.pyplyn.model.TransformationResult;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

/**
 * Date and value format class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public final class FormatUtils {

    /**
     * Used to tag {@link TransformationResult}s, when provided values are defaults
     *   and not actual values retrieved from the specified endpoints
     */
    private static String DEFAULT_VALUE_MESSAGE_TEMPLATE = "Default value %s=%s";

    /**
     * Standard decimal formatter used where numbers (mostly doubles) need to be transformed to String (without all their decimals)
     */
    private static ThreadLocal<DecimalFormat> decimalFormatDefault = ThreadLocal.withInitial(() -> new DecimalFormat("0.00##"));

    /**
     * Used to format shorted numbers, in case the output has space constraints
     */
    private static ThreadLocal<DecimalFormat> decimalFormatShort = ThreadLocal.withInitial(() -> {
        DecimalFormat formatter = new DecimalFormat("0.#");
        formatter.setMaximumIntegerDigits(3);
        formatter.setMaximumFractionDigits(1);
        return formatter;
    });


    /**
     * Utilities classes should not be instantiated
     */
    private FormatUtils() { }

    /**
     * Parses string as time
     *   accepted formats are milliseconds from epoch and valid date string
     *
     * @throws DateTimeParseException if value cannot be parsed as valid datetime
     */
    public static ZonedDateTime parseUTCTime(String value) {
        try {
            // parses ms unix time and returns at UTC offset
            Instant instant = Instant.ofEpochMilli(Long.parseLong(value));
            return instant.atZone(ZoneOffset.UTC);

        } catch (NumberFormatException e) {
            // parses date as string and returns at UTC offset
            return ZonedDateTime.of(ZonedDateTime.parse(value).toLocalDateTime(), ZoneOffset.UTC);
        }
    }

    /**
     * Parses a String as a Number
     *
     * @throws ParseException if number is invalid / cannot be parsed
     */
    public static Number parseNumber(String value) throws ParseException {
        return NumberFormat.getInstance().parse(value);
    }

    /**
     * Formats value as decimal with two digits and return as string
     */
    public static String formatNumber(Number value) {
        return decimalFormatDefault.get().format(value);
    }

    /**
     * Formats numbers to only occupy 5 chars max, including a size suffix
     * <p/>
     * <p/>Negative numbers are also considered and will generally result in the fraction part being dropped to
     *   accommodate the requirements.
     * <p/>
     * <p/><b>NOTE: this method does not handle very small numbers; anything smaller than 0.1 will return "0"</b>
     */
    public static String formatNumberFiveCharLimit(Number value) {
        // return 0 early
        if (value.intValue() == 0) {
            return "0";
        }

        // compute the divisor coefficient to apply (groups of thousands)
        int divisor = (int)Math.floor(Math.log10(Math.abs(value.doubleValue())) / 3);

        // divide value to obtain a number with the dot in the right place
        double newValue = value.doubleValue() / Math.pow(1000, divisor);

        // format string
        String formatted = decimalFormatShort.get().format(newValue);

        // if length is larger than 5 (taking into account suffix), remove fractional part
        int maxLength = 5 - (divisor>0?1:0);
        if (formatted.length() > maxLength) {
            formatted = decimalFormatShort.get().format((int)newValue);
        }

        // append suffix, if necessary
        StringBuilder result = new StringBuilder(formatted);
        if (divisor>0) {
            result.append(" KMGTPE".charAt(divisor));
        }

        return result.toString();
    }

    /**
     * Provides a standard message to be used when needing to denote a default value having been provided
     */
    public static String generateDefaultValueMessage(String metric, Number value) {
        return String.format(DEFAULT_VALUE_MESSAGE_TEMPLATE, metric, formatNumber(value));
    }
}
