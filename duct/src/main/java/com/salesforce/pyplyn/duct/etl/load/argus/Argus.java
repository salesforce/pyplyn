/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.load.argus;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.Load;

/**
 * Argus load destination model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.2.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableArgus.class)
@JsonSerialize(as = ImmutableArgus.class)
@JsonTypeName("Argus")
public abstract class Argus implements Load {

    private static final long serialVersionUID = -800889985629386204L;

    /**
     * Endpoint where the results should be published
     */
    public abstract String endpoint();

    /**
     * Scope to tie use for the published {@link com.salesforce.argus.model.MetricResponse}s
     */
    public abstract String scope();

}
