/*
 * Copyright (c) 2016-2017, Salesforce.com, Inc. All rights reserved. Licensed under the BSD 3-Clause license. For full
 * license text, see the LICENSE.txt file in repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
        /**
         * Jackson Long deserializer implementation
         *
         * @throws IOException
         *             if the bytes could not be serialized/written
         */
        @Override
        public Long deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            final String timeString = jsonParser.getValueAsString();
            final long timeLimit = Instant.now().toEpochMilli();
            long result = -1;

            try {
                // attempt to read the string as a Long value
                result = Long.valueOf(timeString);

            } catch (NumberFormatException e) {
                // valid relative time strings should start with a minus
                if (timeString.charAt(0) == '-') {
                    long value = 0;

                    int i = 0;
                    while (++i < timeString.length()) {
                        final char digit = timeString.charAt(i);
                        // stop if we've reached the time specifier
                        if (digit < '0' || digit > '9') break;

                        if (value > 0) {
                            value = 10 * value + digit - '0';
                        } else {
                            value += digit - '0';
                        }

                        // stop if going beyond the start of the current epoch (1970-01-01) assuming millis are used
                        if (value > timeLimit) break;
                    }

                    // last character
                    if (i == timeString.length() - 1) {
                        char digit = timeString.charAt(i);
                        switch (digit) {
                        case 'd':
                            result = relativeTime(TimeUnit.DAYS.toMillis(value));
                            break;

                        case 'h':
                            result = relativeTime(TimeUnit.HOURS.toMillis(value));
                            break;

                        case 'm':
                            result = relativeTime(TimeUnit.MINUTES.toMillis(value));
                            break;

                        case 's':
                            result = relativeTime(TimeUnit.SECONDS.toMillis(value));
                            break;
                        }

                    // two characters left
                    } else if (i == timeString.length() - 2) {
                        // value is already in milliseconds
                        if (timeString.charAt(i) == 'm'
                                && timeString.charAt(i + 1) == 's') {
                            result = relativeTime(value);
                        }
                    }
                }
            }

            // return in an errored state if we cannot parse a Long input (milliseconds), or the time specifiers
            // are not one of (h, m, s, ms) or the result is less that the epoch's start
            if (result < 0 || result > timeLimit) {
                throw new IOException(timeString + " could not converted to a time value (milliseconds from epoch)");
            }

            return result;
        }

        /**
         * @return the computed relative time, from the current point in time
         * @throws IOException
         *             if the result is invalid
         */
        public Long relativeTime(Long offsetMillis) {
            return Instant.now().toEpochMilli() - offsetMillis;
        }
    }

}
