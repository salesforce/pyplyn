/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class AbsoluteOrRelativeTimeTest {

    @Mock
    JsonParser parser;

    @Mock
    DeserializationContext context;

    final long now = Instant.now().toEpochMilli();

    AbsoluteOrRelativeTime.Deserializer deserializer;


    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // ARRANGE
        deserializer = spy(new AbsoluteOrRelativeTime.Deserializer());

        // fix the instant, so we can test it
        doAnswer(a -> now - (Long)a.getArgument(0)).when(deserializer).relativeTime(anyLong());
    }


    @Test
    public void deserializeLong() throws Exception {
        // ARRANGE
        doReturn("100").when(parser).getValueAsString();

        // ACT
        Long result = deserializer.deserialize(parser, context);

        // ASSERT

        // tests the fact that the password is serialized as byte array and not converted to an immutable String object
        assertThat(result, equalTo(100L));

    }

    @Test
    public void deserializeDays() throws Exception {
        // ARRANGE
        doReturn("-3d").when(parser).getValueAsString();

        // ACT
        Long result = deserializer.deserialize(parser, context);

        // ASSERT

        // tests the fact that the password is serialized as byte array and not converted to an immutable String object
        assertThat(result, equalTo(now - TimeUnit.DAYS.toMillis(3)));
    }

    @Test
    public void deserializeHours() throws Exception {
        // ARRANGE
        doReturn("-24h").when(parser).getValueAsString();


        // ACT
        Long result = deserializer.deserialize(parser, context);

        // ASSERT

        // tests the fact that the password is serialized as byte array and not converted to an immutable String object
        assertThat(result, equalTo(now - TimeUnit.HOURS.toMillis(24)));
    }

    @Test
    public void deserializeSeconds() throws Exception {
        // ARRANGE
        doReturn("-100s").when(parser).getValueAsString();

        // ACT
        Long result = deserializer.deserialize(parser, context);

        // ASSERT

        // tests the fact that the password is serialized as byte array and not converted to an immutable String object
        assertThat(result, equalTo(now - TimeUnit.SECONDS.toMillis(100)));
    }

    @Test
    public void deserializeMilliseconds() throws Exception {
        // ARRANGE
        doReturn("-1000ms").when(parser).getValueAsString();

        // ACT
        Long result = deserializer.deserialize(parser, context);

        // ASSERT

        // tests the fact that the password is serialized as byte array and not converted to an immutable String object
        assertThat(result, equalTo(now - 1000));
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldFailOnRandomString() throws Exception {
        // ARRANGE
        doReturn("fail").when(parser).getValueAsString();

        // ACT / ASSERT
        deserializer.deserialize(parser, context);
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldFailOnMissingSpecifier() throws Exception {
        // ARRANGE
        doReturn("-100").when(parser).getValueAsString();

        // ACT / ASSERT
        deserializer.deserialize(parser, context);
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldFailOnInvalidTimeSpecifier() throws Exception {
        // ARRANGE
        doReturn("-100x").when(parser).getValueAsString();

        // ACT / ASSERT
        deserializer.deserialize(parser, context);
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldFailOnTooLongTimeSpecifier() throws Exception {
        // ARRANGE
        doReturn("-100mss").when(parser).getValueAsString();

        // ACT / ASSERT
        deserializer.deserialize(parser, context);
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldFailOnInvalidInterval() throws Exception {
        // ARRANGE
        long daysSinceEpochPlusSome = now / 86_400_000 + 100;
        doReturn("-" + daysSinceEpochPlusSome + "d").when(parser).getValueAsString();

        // ACT / ASSERT
        deserializer.deserialize(parser, context);
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldFailOnTooManyMillis() throws Exception {
        // ARRANGE
        long millisSinceEpochPlusSome = now + 86_400_00;
        doReturn("" + millisSinceEpochPlusSome).when(parser).getValueAsString();

        // ACT / ASSERT
        deserializer.deserialize(parser, context);
    }

}