/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.processor;

import com.salesforce.pyplyn.model.Extract;
import com.salesforce.pyplyn.model.TransformationResult;

import java.util.Collections;
import java.util.List;

/**
 * Extract processor interface
 * <p/>
 * <p/>Used to generify the different extract processor types and define common logic.
 *
 * @see LoadProcessor similar implementation for {@link com.salesforce.pyplyn.model.Load} types
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface ExtractProcessor<T extends Extract> extends Filterable<T, Extract> {

    /**
     * Implementations should override this method and write the processing logic
     *
     * @param datasource dataset that should be processed
     */
    List<List<TransformationResult>> process(List<T> datasource);

    /**
     * Default processor logic that will first filter the required data then process all valid entries
     *
     * @return list of results or empty list when nothing was processed
     */
    default List<List<TransformationResult>> execute(Extract... data) {
        List<T> filtered = filter(data);
        if (!filtered.isEmpty()) {
            return process(filtered);
        }

        return Collections.emptyList();
    }
}
