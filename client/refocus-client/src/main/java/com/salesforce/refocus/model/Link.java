/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.refocus.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;

/**
 * Link model
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableLink.class)
@JsonSerialize(as = ImmutableLink.class)
@JsonInclude(NON_EMPTY)
public abstract class Link implements Serializable {
    private static final long serialVersionUID = -6786252096633818767L;

    public abstract String name();

    public abstract String url();
}
