/*
 * Copyright (c) 2016-2017, Salesforce.com, Inc. All rights reserved. Licensed under the BSD 3-Clause license. For full
 * license text, see the LICENSE.txt file in repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Jackson serializer/deserializer implementation for working with absolute and relative time formats
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.1.0
 */
public class AbsoluteOrRelativeTime {

    /**
     * Deserializes a {@link String} which can contain absolute or relative time strings into their 'millisecond from
     * epoch' representation
     * <p/>
     * <p/>
     * Relative time formats:
     * <p/>
     * -1d (days)
     * <p/>
     * -24h (hours)
     * <p/>
     * -1m (minute)
     * <p/>
     * -1s (seconds)
     * <p/>
     * -1ms (milliseconds)
     * <p/>
     * <p/>
     * Absolute time formats:
     * <p/>
     * 1509545598000 (milliseconds from epoch)
     */
    public static class Deserializer extends JsonDeserializer<Long> {

        private Supplier<Long> currentTimeSupplier = () -> Instant.now().toEpochMilli();

        /**
         * Default constructor, assumes the use of the current time to
         */
        public Deserializer() {
        }

        /**
         * Constructor used for testing; allows fixing the time to a specified value
         */
        Deserializer(Supplier<Long> currentTimeSupplier) {
            this.currentTimeSupplier = currentTimeSupplier;
        }

        /**
         * Jackson Long deserializer implementation
         *
         * @throws IOException
         *             if the bytes could not be serialized/written
         */
        @Override
        public Long deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            final String timeString = jsonParser.getValueAsString();
            final long timeLimit = currentTimeSupplier.get();

            long result;
            try {
                // attempt to read the string as a Long value
                result = Long.valueOf(timeString);

            } catch (NumberFormatException e) {
                // otherwise treat as relative string
                result = parseRelativeTimeSpecifier(timeString, timeLimit);
            }

            // throw an exception if we cannot parse a Long input (milliseconds), or the time specifiers
            // are not one of (d, h, m, s, ms) or the result is less that the epoch's start
            if (result < 0 || result > timeLimit) {
                throw new IOException(timeString + " could not converted to a time value (milliseconds from epoch)");
            }

            return result;
        }

    }

    /**
     * Parses a relative time and returns it's corresponding timestamp, substracted from the present time (<code>presentTimeInMillis</code>)
     *
     * @param relativeTime The time string you want to process (e.g.: -7d, -1h, -100ms, etc.)
     * @param presentTimeInMillis The value (in millis) of the current epoch time (usually <code>Instant.now().toEpochMilli()</code>)
     * @return -1 if the <code>relativeTime</code> is in an invalid format
     */
    public static long parseRelativeTimeSpecifier(String relativeTime, long presentTimeInMillis) {
        // valid relative time strings should start with a minus
        if (relativeTime.charAt(0) == '-') {
            long value = 0;

            int i = 0;
            while (++i < relativeTime.length()) {
                final char digit = relativeTime.charAt(i);
                // stop if we've reached the time specifier
                if (digit < '0' || digit > '9') break;

                if (value > 0) {
                    value = 10 * value + digit - '0';
                } else {
                    value += digit - '0';
                }

                // stop if the current time is in the future (the computation overflows past Long.MIN_VALUE
                if (value > presentTimeInMillis) break;
            }

            // when we only have one character left, check the single character specifiers
            if (i == relativeTime.length() - 1) {
                char timeSpecifier = relativeTime.charAt(i);
                switch (timeSpecifier) {
                    case 'd':
                        return presentTimeInMillis - TimeUnit.DAYS.toMillis(value);

                    case 'h':
                        return presentTimeInMillis - TimeUnit.HOURS.toMillis(value);

                    case 'm':
                        return presentTimeInMillis - TimeUnit.MINUTES.toMillis(value);

                    case 's':
                        return presentTimeInMillis - TimeUnit.SECONDS.toMillis(value);
                }

            // only two characters left (check for 'ms')
            } else if (i == relativeTime.length() - 2) {
                // value is already in milliseconds
                if (relativeTime.charAt(i) == 'm' && relativeTime.charAt(i + 1) == 's') {
                    return presentTimeInMillis - value;
                }
            }
        }

        // invalid response
        return -1L;
    }

}
