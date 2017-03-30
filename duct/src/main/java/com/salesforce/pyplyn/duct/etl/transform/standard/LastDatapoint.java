/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.standard;

import com.google.common.collect.Iterables;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

/**
 * Filters out all but the last data point
 * <p/>
 * Applying this transformation effectively transforms a matrix of E {@link com.salesforce.pyplyn.model.Extract}s
 *   by N data points (i.e.: when Extracts return time-series data for more than one time)
 *   into a matrix of Ex1 (where E is the number of Extracts defined in the {@link com.salesforce.pyplyn.configuration.Configuration})
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class LastDatapoint implements Transform, Serializable {
    private static final long serialVersionUID = -2187464148729449576L;


    /**
     * Applies this transformation and returns a new {@link TransformationResult} matrix
     */
    @Override
    public List<List<TransformationResult>> apply(List<List<TransformationResult>> input) {
        return input.stream()
                .map(points -> {
                    // load last result point and return a list containing a single element
                    TransformationResult result = Iterables.getLast(points, null);
                    if (nonNull(result)) {
                        return Collections.singletonList(result);
                    }

                    // or return null if no result was found
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    @Override
    public boolean equals(Object o) {
        return !(o == null || getClass() != o.getClass());
    }

    @Override
    public int hashCode() {
        return LastDatapoint.class.hashCode();
    }
}
