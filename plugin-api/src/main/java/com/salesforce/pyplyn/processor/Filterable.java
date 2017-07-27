/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.processor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contract implemented by all Processors which can expect an array of the <b>S</b> supertype
 *   and require to filter out all subtypes that are not <b>T</b>
 *
 * @param <T> Type that the implementer is interested in
 * @param <S> Supertype of T
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface Filterable<T extends S, S> {

    /**
     * Should return the class object used to determine who <b>T</b> is at runtime
     */
    Class<T> filteredType();


    /**
     * Filters out objects that are not of type <b>T</b>
     *
     * @param objects array that might contain elements of subtypes of <b>S</b> other than <b>T</b>
     * @return guaranteed to return List&lt;<b>T</b>&gt;
     */
    default List<T> filter(List<S> objects) {
        Class<T> cls = filteredType();

        return objects.stream()
                .filter(cls::isInstance)
                .map(cls::cast)
                .collect(Collectors.toList());
    }
}
