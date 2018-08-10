/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.extract.trust1;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.Extract;
import org.immutables.value.Value;

/**
 * Trust1 data source model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.1.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableTrust1.class)
@JsonSerialize(as = ImmutableTrust1.class)
@JsonTypeName("Trust1")
public abstract class Trust1 implements Extract {

    private static final long serialVersionUID = 3272985988188568032L;

    /**
     * Endpoint where the query should be executed on
     */
    public abstract String endpoint();

    /**
     * How long to cache this call's results
     */
    @Value.Default
    @Value.Auxiliary
    public int cacheMillis() {
        return 0;
    }

    /**
     * Instance to load the status for
     */
    public abstract String instance();
}
