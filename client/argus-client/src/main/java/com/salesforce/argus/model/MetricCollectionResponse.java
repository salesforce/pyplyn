/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.argus.model;


import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;

@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableMetricCollectionResponse.class)
@JsonSerialize(as = ImmutableMetricCollectionResponse.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public abstract class MetricCollectionResponse {

    @JsonProperty("Error Messages")
    public abstract List<Object> errorMessages();

    @Nullable
    @JsonProperty("Error")
    public abstract String error();

    @Nullable
    @JsonProperty("Success")
    public abstract String success();

}
