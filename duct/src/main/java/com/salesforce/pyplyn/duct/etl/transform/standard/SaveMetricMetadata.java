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
import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;

/**
 * Tags the result stage with each input's name and value
 * <p/>
 * This transformation should be applied after a {@link LastDatapoint}.
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class SaveMetricMetadata implements Transform, Serializable {
    private static final long serialVersionUID = 2589981196065459798L;


    /**
     * This method assumes only one value per source {@link com.salesforce.pyplyn.model.Extract} exists and
     *   it will throw an {@link IllegalArgumentException} if more than one {@link TransformationResult}s per row
     *   is passed
     */
    @Override
    public List<List<TransformationResult>> apply(List<List<TransformationResult>> input) {
        // store all name, value pairs in a map
        final List<String> nameValuePairMessages = input.stream()
                .map((iterable) -> Iterables.getOnlyElement(iterable, null))
                .filter(Objects::nonNull)
                .map(result -> result.name() + "=" + formatNumber(result.value()))
                .collect(Collectors.toList());


        // construct new objects with tags and return
        return input.stream()
                .map(metrics -> metrics.stream()
                        // tags all ExtractResult objects, as we have no guarantee which will be selected at the end
                        .map(stage -> new TransformationResultBuilder(stage)
                                .metadata((metadata) -> metadata
                                        .addMessages(nameValuePairMessages))
                                .build())

                        .collect(Collectors.toList())
                ).collect(Collectors.toList());
    }


    @Override
    public boolean equals(Object o) {
        return !(o == null || getClass() != o.getClass());
    }

    @Override
    public int hashCode() {
        return SaveMetricMetadata.class.hashCode();
    }
}
