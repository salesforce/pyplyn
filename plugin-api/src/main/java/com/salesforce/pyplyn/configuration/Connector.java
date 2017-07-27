/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import org.immutables.value.Value;

/**
 * Base connector class
 * <p/>
 * Provides endpoint, user, password, proxy host, and port values that are used by implementations of
 * {@link com.salesforce.pyplyn.client.AbstractRemoteClient} to authenticate against the endpoints specified
 * in {@link com.salesforce.pyplyn.model.Extract} and {@link com.salesforce.pyplyn.model.Load}
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableConnector.class)
@JsonSerialize(as = ImmutableConnector.class)
public abstract class Connector implements ConnectorInterface {
    @Override
    public boolean equals(Object another) {
        return delegateEquals(another);
    }

    @Override
    public int hashCode() {
        return delegateHashCode();
    }
}
