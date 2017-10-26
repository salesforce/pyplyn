/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.virtualinstruments.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.List;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;

/**
 * Model for the {@link com.virtualinstruments.VirtualInstrumentsService#reportPoll(String)} call
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 10.0.0
 */
@Value.Immutable
@Value.Enclosing
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableReportResponse.class)
@JsonSerialize(as = ImmutableReportResponse.class)
@JsonInclude(NON_EMPTY)
public abstract class ReportResponse {

    /**
     * @return The report's UUID
     */
    public abstract String uuid();

    public abstract String status();

    public abstract boolean finished();

    public abstract Result result();


    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableReportResponse.Result.class)
    @JsonSerialize(as = ImmutableReportResponse.Result.class)
    public static abstract class Result {
        public abstract List<Chart> charts();
    }

    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableReportResponse.Chart.class)
    @JsonSerialize(as = ImmutableReportResponse.Chart.class)
    public static abstract class Chart {

        public abstract String uuid();

        public abstract String status();

        public abstract boolean paginating();

        public abstract List<ChartData> chartData();

    }

    @Value.Immutable
    @PyplynImmutableStyle
    @JsonDeserialize(as = ImmutableReportResponse.ChartData.class)
    @JsonSerialize(as = ImmutableReportResponse.ChartData.class)
    public static abstract class ChartData {

        public abstract String entityId();

        public abstract String entityName();

        public abstract List<Number[]> data();

    }

}
