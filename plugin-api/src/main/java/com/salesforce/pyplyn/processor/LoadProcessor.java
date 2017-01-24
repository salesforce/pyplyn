/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.processor;

import com.salesforce.pyplyn.model.Load;
import com.salesforce.pyplyn.model.TransformationResult;

import java.util.Collections;
import java.util.List;

/**
 * Load processor interface
 * <p/>
 * <p/>Used to generify the different extract processor types and define common logic.
 *
 * @see ExtractProcessor similar implementation for {@link com.salesforce.pyplyn.model.Extract} types
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public interface LoadProcessor<T extends Load> extends Filterable<T, Load> {

    /**
     * Implementations should override this method and write the processing logic
     *
     * @param data dataset that should be processed
     * @param destinations where the data should be loaded to
     */
    List<Boolean> process(List<TransformationResult> data, List<T> destinations);

    /**
     * Default processor logic that will first filter the required data then process all valid entries
     *
     * @return list of results or empty list when nothing was processed
     */
    default List<Boolean> execute(List<TransformationResult> data, Load... destinations) {
        List<T> filtered = filter(destinations);
        if (!filtered.isEmpty()) {
            return process(data, filtered);
        }

        return Collections.emptyList();
    }
}
