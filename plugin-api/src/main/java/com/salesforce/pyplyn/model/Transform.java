/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;

import java.io.Serializable;
import java.util.List;

/**
 * Transforms interface
 * <p/>
 * <p/>All types that represent "transforms" should inherit this interface.
 * <p/>
 * <p/>This type implies all JSON definitions that will be deserialized into Transform will have a "name" parameter
 *   which will contain the actual subtype's name (which in this case represents the transform function to be applied).
 * <p/>
 * <p/><b>All implementations should be {@link Serializable}!</b>
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@JsonIgnoreProperties("name")
public interface Transform extends Serializable {

    /**
     * This method should implement the desired transformation and return the processed results
     */
    List<List<TransformationResult>> apply(List<List<TransformationResult>> input);

    /**
     * Async transformation
     * <p/>
     * <p/> {@link Transform}s are observed on the specified {@link Scheduler}
     */
    default Flowable<List<List<TransformationResult>>> applyAsync(List<List<TransformationResult>> input, Scheduler scheduler) {
        return Flowable.just(input)
                .observeOn(scheduler)
                .map(this::apply);
    }
}
