/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

/**
 * Transform function interface
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
    List<List<TransformationResult>> apply(List<List<TransformationResult>> input);
}
