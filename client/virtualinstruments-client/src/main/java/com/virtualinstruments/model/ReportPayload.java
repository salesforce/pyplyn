/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.virtualinstruments.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import org.immutables.value.Value;

import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Payload for the {@link com.virtualinstruments.VirtualInstrumentsService#reportBatch(String, ReportPayload)} call
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.2.0
 */
@Value.Immutable
@Value.Enclosing
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableReportPayload.class)
@JsonSerialize(as = ImmutableReportPayload.class)
@JsonInclude(NON_EMPTY)
public abstract class ReportPayload {

    /**
     * @return The report's UUID
     */
    @Value.Default
    public String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * @return Start timestamp in millis
     */
    public abstract Long startTimestamp();

    /**
     * @return End timestamp in millis
     */
    public abstract Long endTimestamp();

    /**
     * @return List of charts to generate
     */
    public abstract List<Chart> charts();


    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableReportPayload.Chart.class)
    @JsonSerialize(as = ImmutableReportPayload.Chart.class)
    public static abstract class Chart {

        @Value.Default
        public String chartUuid() {
            return UUID.randomUUID().toString();
        }

        @Value.Default
        public String chartType() {
            return "topxtrend";
        }

        public abstract List<ChartQueryParam> chartQueryParams();

    }

    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableReportPayload.ChartQueryParam.class)
    @JsonSerialize(as = ImmutableReportPayload.ChartQueryParam.class)
    @JsonInclude(NON_NULL)
    public static abstract class ChartQueryParam {

        @Value.Default
        public int desiredPoints() {
            return 1;
        }

        public abstract String entityType();

        public abstract String metricName();

        @Value.Default
        public boolean isAscending() {
            return false;
        }

        @Value.Default
        public String entity() {
            return "";
        }

        @Value.Default
        public int limit() {
            return 5;
        }

    }

}
