/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import org.testng.annotations.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.fail;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class CollectionUtilsTest {
    @Test
    public void testNullableArrayCopy() throws Exception {
        // ARRANGE
        Integer[] input = {1, 2};
        Integer[] nullInput = null;

        // ACT
        Integer[] output = CollectionUtils.nullableArrayCopy(input);
        Integer[] nullOutput = CollectionUtils.nullableArrayCopy(nullInput);

        // ASSERT
        assertThat(output.length, equalTo(input.length));
        assertThat(output, not(sameInstance(input)));
        assertThat(nullOutput, nullValue());
    }

    @Test
    public void testNullableDoubleArrayCopy() throws Exception {
        // ARRANGE
        double[] input = {1.0d, 2.0d};
        double[] nullInput = null;

        // ACT
        double[] output = CollectionUtils.nullableArrayCopy(input);
        double[] nullOutput = CollectionUtils.nullableArrayCopy(nullInput);

        // ASSERT
        assertThat(output.length, equalTo(input.length));
        assertThat(output, not(sameInstance(input)));
        assertThat(nullOutput, nullValue());
    }

    @Test
    public void testNullableLongArrayCopy() throws Exception {
        // ARRANGE
        long[] input = {1L, 2L};
        long[] nullInput = null;

        // ACT
        long[] output = CollectionUtils.nullableArrayCopy(input);
        long[] nullOutput = CollectionUtils.nullableArrayCopy(nullInput);

        // ASSERT
        assertThat(output.length, equalTo(input.length));
        assertThat(output, not(sameInstance(input)));
        assertThat(nullOutput, nullValue());
    }

    @Test
    public void testNullableByteArrayCopy() throws Exception {
        // ARRANGE
        byte[] input = {1, 2};
        byte[] nullInput = null;

        // ACT
        byte[] output = CollectionUtils.nullableArrayCopy(input);
        byte[] nullOutput = CollectionUtils.nullableArrayCopy(nullInput);

        // ASSERT
        assertThat(output.length, equalTo(input.length));
        assertThat(output, not(sameInstance(input)));
        assertThat(nullOutput, nullValue());
    }

    @Test
    public void testImmutableOrEmptyList() throws Exception {
        // ARRANGE
        List<String> nullInput = null;
        List<String> input = Arrays.asList("name1", "name2");

        // ACT
        List<String> emptyOutput = CollectionUtils.immutableOrEmptyList(nullInput);
        List<String> output = CollectionUtils.immutableOrEmptyList(input);

        // ASSERT
        assertThat(emptyOutput, hasSize(0));
        assertThat(output.size(), equalTo(input.size()));
        assertThat(output, not(sameInstance(input)));
        attemptModifyListAndExpectFailure(output, "nope");
    }

    @Test
    public void testImmutableListOrNull() throws Exception {
        // ARRANGE
        List<String> nullInput = null;
        List<String> input = Arrays.asList("name1", "name2");

        // ACT
        List<String> nullOutput = CollectionUtils.immutableListOrNull(nullInput);
        List<String> output = CollectionUtils.immutableListOrNull(input);

        // ASSERT
        assertThat(nullOutput, nullValue());
        assertThat(output.size(), equalTo(input.size()));
        assertThat(output, not(sameInstance(input)));
        attemptModifyListAndExpectFailure(output, "nope");
    }


    @Test
    public void testMutableListCopyOrNull() throws Exception {
        // ARRANGE
        List<String> nullInput = null;
        List<String> input = Arrays.asList("name1", "name2");

        // ACT
        List<String> nullOutput = CollectionUtils.mutableListCopyOrNull(nullInput);
        List<String> output = CollectionUtils.mutableListCopyOrNull(input);

        // ASSERT
        assertThat(nullOutput, nullValue());
        assertThat(output.size(), equalTo(input.size()));
        assertThat(output, not(sameInstance(input)));
        attemptModifyListAndExpectSuccess(output, "yup");
    }

    @Test
    public void testImmutableOrEmptyMap() throws Exception {
        // ARRANGE
        Map<String, String> nullInput = null;
        Map<String, String> input = Collections.singletonMap("key", "val");

        // ACT
        Map<String, String> emptyOutput = CollectionUtils.immutableOrEmptyMap(nullInput);
        Map<String, String> output = CollectionUtils.immutableOrEmptyMap(input);

        // ASSERT
        assertThat(emptyOutput.size(), is(0));
        assertThat(output.size(), equalTo(input.size()));
        assertThat(output, not(sameInstance(input)));
        attemptModifyMapAndExpectFailure(output, "key2", "val2");
    }

    @Test
    public void testImmutableMapOrNull() throws Exception {
        // ARRANGE
        Map<String, String> nullInput = null;
        Map<String, String> input = Collections.singletonMap("key", "val");

        // ACT
        Map<String, String> nullOutput = CollectionUtils.immutableMapOrNull(nullInput);
        Map<String, String> output = CollectionUtils.immutableMapOrNull(input);

        // ASSERT
        assertThat(nullOutput, nullValue());
        assertThat(output.size(), equalTo(input.size()));
        assertThat(output, not(sameInstance(input)));
        attemptModifyMapAndExpectFailure(output, "key2", "val2");
    }

    @Test
    public void testImmutableSortedOrEmptyMap() throws Exception {
        // ARRANGE
        SortedMap<String, String> nullInput = null;
        SortedMap<String, String> input = new TreeMap<>(Collections.singletonMap("key", "val"));

        // ACT
        SortedMap<String, String> emptyOutput = CollectionUtils.immutableSortedOrEmptyMap(nullInput);
        SortedMap<String, String> output = CollectionUtils.immutableSortedOrEmptyMap(input);

        // ASSERT
        assertThat(emptyOutput.size(), is(0));
        assertThat(output.size(), equalTo(input.size()));
        assertThat(output, not(sameInstance(input)));
        attemptModifyMapAndExpectFailure(output, "key2", "val2");
    }

    @Test
    public void testImmutableSortedMapOrNull() throws Exception {
        // ARRANGE
        SortedMap<String, String> nullInput = null;
        SortedMap<String, String> input = new TreeMap<>(Collections.singletonMap("key", "val"));

        // ACT
        SortedMap<String, String> nullOutput = CollectionUtils.immutableSortedMapOrNull(nullInput);
        SortedMap<String, String> output = CollectionUtils.immutableSortedMapOrNull(input);

        // ASSERT
        assertThat(nullOutput, nullValue());
        assertThat(output.size(), equalTo(input.size()));
        assertThat(output, not(sameInstance(input)));
        attemptModifyMapAndExpectFailure(output, "key2", "val2");
    }


    /**
     * Adds an element in an unodifiable list and expects it to fail
     */
    private <T> void attemptModifyListAndExpectFailure(List<T> list, T elem) {
        try {
            list.add(elem);
            fail("List should not be modifiable");

        } catch (UnsupportedOperationException e) {
            // expecting this to be thrown
        }
    }


    /**
     * Adds an element in a list and expects it to succeed
     */
    private <T> void attemptModifyListAndExpectSuccess(List<T> list, T elem) {
        try {
            list.add(elem);

        } catch (UnsupportedOperationException e) {
            fail("List should be modifiable");
        }
    }


    /**
     * Puts an element in an unodifiable list and expects it to fail
     */
    private <K, V> void attemptModifyMapAndExpectFailure(Map<K, V> map, K key, V val) {
        try {
            map.put(key, val);
            fail("Map should not be modifiable");

        } catch (UnsupportedOperationException e) {
            // expecting this to be thrown
        }
    }
}