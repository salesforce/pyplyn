/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.util;

import java.util.*;

import static java.util.Objects.nonNull;

/**
 * Various utils used for manipulating collection and array data structures
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public final class CollectionUtils {

    /**
     * Utilities classes should not be instantiated
     */
    private CollectionUtils() { }

    /**
     * Copies all elements from the input array and returns a new array
     *   or null, if null was passed for input
     */
    public static <T> T[] nullableArrayCopy(T... input) {
        return Optional.ofNullable(input).map(s -> Arrays.copyOf(s, s.length)).orElse(null);
    }

    /**
     * Copies all elements from the input array and returns a new double[] array
     *   or null, if null was passed for input
     */
    public static double[] nullableArrayCopy(double... input) {
        return Optional.ofNullable(input).map(s -> Arrays.copyOf(s, s.length)).orElse(null);
    }

    /**
     * Copies all elements from the input array and returns a new long[] array
     *   or null, if null was passed for input
     */
    public static long[] nullableArrayCopy(long... input) {
        return Optional.ofNullable(input).map(s -> Arrays.copyOf(s, s.length)).orElse(null);
    }

    /**
     * Copies all elements from the input array and returns a new byte[] array
     *   or null, if null was passed for input
     */
    public static byte[] nullableArrayCopy(byte... input) {
        return Optional.ofNullable(input).map(s -> Arrays.copyOf(s, s.length)).orElse(null);
    }

    /**
     * Sets all the byte array's elements to zero
     *   use this method to null-out passwords
     */
    public static void nullOutByteArray(byte... input) {
        if (nonNull(input)) {
            Arrays.fill(input, (byte) 0);
        }
    }

    /**
     * Creates an unmodifiable copy of the input list, iterated using its implementation specific order
     *   guarantees the return to be a {@link List} (empty if the input was null)
     */
    public static <T> List<T> immutableOrEmptyList(List<T> input) {
        return Optional.ofNullable(input).map(ArrayList::new).map(Collections::unmodifiableList).orElse(Collections.emptyList());
    }

    /**
     * Creates an unmodifiable copy of the input list, iterated using its implementation specific order
     *   or returns null if input was null
     */
    public static <T> List<T> immutableListOrNull(List<T> input) {
        return Optional.ofNullable(input).map(ArrayList::new).map(Collections::unmodifiableList).orElse(null);
    }

    /**
     * Creates a modifiable copy of the input list
     *   by adding all the input elements into an {@link ArrayList}
     *   or returns null if input was null
     */
    public static <T> List<T> mutableListCopyOrNull(List<T> input) {
        return Optional.ofNullable(input).map(ArrayList::new).map(l -> (List<T>)l).orElse(null);
    }

    /**
     * Creates an unmodifiable copy of the input map
     *   guarantees the return to be a {@link Map} (empty if the input was null)
     */
    public static <K, V> Map<K, V> immutableOrEmptyMap(Map<K, V> input) {
        return Optional.ofNullable(input).map(HashMap::new).map(Collections::unmodifiableMap).orElse(Collections.emptyMap());
    }

    /**
     * Creates an unmodifiable copy of the input map
     *   or returns null if input was null
     */
    public static <K, V> Map<K, V> immutableMapOrNull(Map<K, V> input) {
        return Optional.ofNullable(input).map(HashMap::new).map(Collections::unmodifiableMap).orElse(null);
    }

    /**
     * Creates an unmodifiable copy of the input sorted map
     *   guarantees the return to be a {@link SortedMap} (empty if the input was null)
     */
    public static <K, V> SortedMap<K, V> immutableSortedOrEmptyMap(SortedMap<K, V> input) {
        return Optional.ofNullable(input).map(TreeMap::new).map(Collections::unmodifiableSortedMap).orElse(Collections.emptySortedMap());
    }

    /**
     * Creates an unmodifiable copy of the input sorted map
     *   or returns null if input was null
     */
    public static <K, V> SortedMap<K, V> immutableSortedMapOrNull(SortedMap<K, V> input) {
        return Optional.ofNullable(input).map(TreeMap::new).map(Collections::unmodifiableSortedMap).orElse(null);
    }

    /**
     * Creates an unmodifiable copy of the input list, iterated using its implementation specific order
     *   guarantees the return to be a {@link List} (empty if the input was null)
     */
    public static <T> Set<T> immutableOrEmptySet(Set<T> input) {
        return Optional.ofNullable(input).map(HashSet::new).map(Collections::unmodifiableSet).orElse(Collections.emptySet());
    }

    /**
     * Creates an unmodifiable copy of the input list, iterated using its implementation specific order
     *   or returns null if input was null
     */
    public static <T> Set<T> immutableSetOrNull(Set<T> input) {
        return Optional.ofNullable(input).map(HashSet::new).map(Collections::unmodifiableSet).orElse(null);
    }
}
