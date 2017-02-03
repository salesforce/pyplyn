/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.infostatus;

import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Forces at least a status of INFO, if the status is currently OK
 * <p/>Predefined statuses are 0=OK, 1=INFO, 2=WARN, 3=CRIT
 * <p/>
 * <p/>Note: be careful to either apply this transform after a {@link com.salesforce.pyplyn.duct.etl.transform.threshold.Threshold},
 *     or when you are sure that the input values are already in a [0-3] range
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class InfoStatus implements Transform, Serializable {
    private static final long serialVersionUID = -1927779729819920375L;


    @Override
    public List<List<TransformationResult>> apply(List<List<TransformationResult>> input) {
        // for all rows and columns
        return input.stream()
                .map(rows -> rows.stream()
                        .map(point -> {
                            // if the value indicates a status of OK, remap to a status of 1 (INFO)
                            if (point.value().intValue() == 0) {
                                return new TransformationResultBuilder(point).withValue(1).build();
                            }

                            // otherwise, return the point as is
                            return point;
                        })
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        return !(o == null || getClass() != o.getClass());
    }

    @Override
    public int hashCode() {
        return InfoStatus.class.hashCode();
    }
}
