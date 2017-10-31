/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.AssertJUnit.fail;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class FormatUtilsTest {
    @Test
    public void testFormattingPositiveNumbersDoesNotExceedSize() throws Exception {
        // ARRANGE
        Map<String, Number> expectedResultsForValues = new HashMap<>(23);
        expectedResultsForValues.put("0",     0);
        expectedResultsForValues.put("0",     0L);
        expectedResultsForValues.put("0",     0.0);
        expectedResultsForValues.put("0",     0.001);
        expectedResultsForValues.put("1.2",   1.2);
        expectedResultsForValues.put("123",   123L);
        expectedResultsForValues.put("1.2K",  1234L);
        expectedResultsForValues.put("12.3K", 12345L);
        expectedResultsForValues.put("123K",  123456L);
        expectedResultsForValues.put("1.2M",  1234567L);
        expectedResultsForValues.put("12.3M", 12345678L);
        expectedResultsForValues.put("123M",  123456789L);
        expectedResultsForValues.put("1.2G",  1234567890L);
        expectedResultsForValues.put("12.3G", 12345678901L);
        expectedResultsForValues.put("123G",  123456789012L);
        expectedResultsForValues.put("1.2T",  1234567890123L);
        expectedResultsForValues.put("12.3T", 12345678901234L);
        expectedResultsForValues.put("123T",  123456789012345L);
        expectedResultsForValues.put("1.2P",  1234567890123456L);
        expectedResultsForValues.put("12.3P", 12345678901234567L);
        expectedResultsForValues.put("123P",  123456789012345678L);
        expectedResultsForValues.put("1.2E",  1234567890123456789L);
        expectedResultsForValues.put("9.2E",  9223372036854775807L);

        // ACT/ASSERT
        expectedResultsForValues.entrySet().forEach(FormatUtilsTest::formatterAssertions);
    }


    @Test
    public void testFormattingNegativeNumbersDoesNotExceedSize() throws Exception {
        // ARRANGE
        Map<String, Number> expectedResultsForValues = new HashMap<>(23);
        expectedResultsForValues.put("0",      -0);
        expectedResultsForValues.put("0",      -0L);
        expectedResultsForValues.put("0",      -0.0);
        expectedResultsForValues.put("0",      -0.001);
        expectedResultsForValues.put("-1.2",   -1.2);
        expectedResultsForValues.put("-123",   -123L);
        expectedResultsForValues.put("-1.2K",  -1234L);
        expectedResultsForValues.put("-12K",   -12345L);
        expectedResultsForValues.put("-123K",  -123456L);
        expectedResultsForValues.put("-1.2M",  -1234567L);
        expectedResultsForValues.put("-12M",   -12345678L);
        expectedResultsForValues.put("-123M",  -123456789L);
        expectedResultsForValues.put("-1.2G",  -1234567890L);
        expectedResultsForValues.put("-12G",   -12345678901L);
        expectedResultsForValues.put("-123G",  -123456789012L);
        expectedResultsForValues.put("-1.2T",  -1234567890123L);
        expectedResultsForValues.put("-12T",   -12345678901234L);
        expectedResultsForValues.put("-123T",  -123456789012345L);
        expectedResultsForValues.put("-1.2P",  -1234567890123456L);
        expectedResultsForValues.put("-12P",   -12345678901234567L);
        expectedResultsForValues.put("-123P",  -123456789012345678L);
        expectedResultsForValues.put("-1.2E",  -1234567890123456789L);
        expectedResultsForValues.put("-9.2E",  -9223372036854775800L);

        // ACT/ASSERT
        expectedResultsForValues.entrySet().forEach(FormatUtilsTest::formatterAssertions);
    }


    @Test
    public void testParseCurrentTimeMillis() throws Exception {
        // ARRANGE
        long now = System.currentTimeMillis();

        // ACT
        ZonedDateTime dateTime = FormatUtils.parseUTCTime(Long.valueOf(now).toString());
        long parsedMillis = dateTime.toInstant().toEpochMilli();

        // ASSERT
        assertThat(parsedMillis, equalTo(now));
    }


    @Test
    public void testParseUTCDateTime() throws Exception {
        // ARRANGE
        String date = "2016-12-31T12:13:14Z";

        // ACT
        ZonedDateTime dateTime = FormatUtils.parseUTCTime(date);
        int year = dateTime.getYear();
        int dayOfMonth = dateTime.getDayOfMonth();
        int hour = dateTime.getHour();

        // ASSERT
        assertThat(year, equalTo(2016));
        assertThat(dayOfMonth, equalTo(31));
        assertThat(hour, equalTo(12));
    }

    @Test
    public void testParseNonGMTDateTime() throws Exception {
        // ARRANGE
        String date = "2016-12-31T12:13:14-07:00";

        // ACT
        ZonedDateTime dateTime = FormatUtils.parseUTCTime(date);
        int year = dateTime.getYear();
        int dayOfMonth = dateTime.getDayOfMonth();
        int hour = dateTime.getHour();

        // ASSERT
        assertThat(year, equalTo(2016));
        assertThat(dayOfMonth, equalTo(31));
        assertThat(hour, equalTo(19));
    }

    @Test(expectedExceptions = DateTimeParseException.class)
    public void testParseInvalidDateTimeFail() throws Exception {
        // ARRANGE
        String date = "100-12-31T12:13:14-07:00";

        // ACT/ASSERT
        FormatUtils.parseUTCTime(date);
    }

    @Test
    public void testParseNumber() throws Exception {
        // ARRANGE

        // ACT
        Number dblVal = FormatUtils.parseNumber("1.23");
        Number intVal = FormatUtils.parseNumber("1");
        try {
            FormatUtils.parseNumber("invalid");
            fail("Should be unable to parse string value 'invalid'");

        } catch (ParseException e) {
            // expecting this to happen
        }

        // ASSERT
        assertThat(dblVal, instanceOf(Double.class));
        assertThat("Expecting parsed value to be equal to 1.23", Double.compare((Double) dblVal, 1.23d), is(0));
        assertThat(intVal, instanceOf(Long.class));
        assertThat("Expecting parsed value to be equal to 1L", Long.compare((Long) intVal, 1L), is(0)); // Findbugs: PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS - IGNORE
    }

    @Test
    public void testFormatNumber() throws Exception {
        // ACT
        String decimalString = FormatUtils.formatNumber(new Double("1.234599"));
        String integerString = FormatUtils.formatNumber(1);

        // ASSERT
        assertThat(decimalString, equalTo("1.2346"));
        assertThat(integerString, equalTo("1.00"));
    }

    @Test
    public void testFormatMillis() throws Exception {
        // ARRANGE
        Double input = 500.00d;

        // ACT
        String output = FormatUtils.formatMillisOrSeconds(input);

        // ASSERT
        assertThat(output, equalTo("500ms"));
    }

    @Test
    public void testFormatMillisWithDecimal() throws Exception {
        // ARRANGE
        Double input = 500.1d;

        // ACT
        String output = FormatUtils.formatMillisOrSeconds(input);

        // ASSERT
        assertThat(output, equalTo("500.1ms"));
    }

    @Test
    public void testFormatSeconds() throws Exception {
        // ARRANGE
        Double input = 1500.1d;

        // ACT
        String output = FormatUtils.formatMillisOrSeconds(input);

        // ASSERT
        assertThat(output, equalTo("1.5s"));
    }

    @Test
    public void testFormatSecondsDoubleDigits() throws Exception {
        // ARRANGE
        Double input = 12345.67d;

        // ACT
        String output = FormatUtils.formatMillisOrSeconds(input);

        // ASSERT
        assertThat(output, equalTo("12.35s"));
    }

    @Test
    public void testFormattingPercentageDoesNotExceedSize() throws Exception {
        // ARRANGE
        Map<String, Number> expectedResultsForValues = new HashMap<>(23);
        expectedResultsForValues.put("0%",     0);
        expectedResultsForValues.put("0%",     0.0001f);
        expectedResultsForValues.put("0.1%",   0.001f);
        expectedResultsForValues.put("1%",     0.01f);
        expectedResultsForValues.put("10%",    0.1f);
        expectedResultsForValues.put("100%",   1f);
        expectedResultsForValues.put("98.7%",  0.987f);
        expectedResultsForValues.put("98.8%",  0.9876f);

        // ACT/ASSERT
        for (Map.Entry<String, Number> entry : expectedResultsForValues.entrySet()) {
            String result = FormatUtils.formatPercentage(entry.getValue());

            // ASSERT
            assertThat(result, equalTo(entry.getKey()));
            assertThat("String length larger than 5 (\"" + result + "\")", result.length(), lessThanOrEqualTo(5));
        }
    }

    @Test
    public void testGenerateDefaultValueMessage() throws Exception {
        // ACT
        String defaultMessage = FormatUtils.generateDefaultValueMessage("metric", 1234);

        // ASSERT
        assertThat(defaultMessage, allOf(containsString("metric"), containsString("1234")));
    }

    /**
     * Runs the expected assertions
     * @param entry
     * @return
     */
    private static Void formatterAssertions(Map.Entry<String, Number> entry) {
        // ACT
        String result = FormatUtils.formatNumberFiveCharLimit(entry.getValue());

        // ASSERT
        assertThat(entry.getKey(), equalTo(result));
        assertThat("String length larger than 5 (\"" + result + "\")", result.length(), lessThanOrEqualTo(5));

        return null;
    }
}