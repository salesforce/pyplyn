/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.etl.transform.standard;

import static com.salesforce.pyplyn.duct.etl.transform.standard.Threshold.changeValue;
import static com.salesforce.pyplyn.model.StatusCode.*;
import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Iterables;
import com.salesforce.pyplyn.annotations.PyplynImmutableStyle;
import com.salesforce.pyplyn.model.ImmutableTransmutation;
import com.salesforce.pyplyn.model.ThresholdType;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.Transmutation;

/**
 * Determines if the input time series data has matched the threshold
 * for the specified critical/warn/info duration, and return
 * critical/warning/info/ok status values respectively
 * 
 * <p/>
 * Applying this transformation effectively transforms a matrix of E
 * {@link com.salesforce.pyplyn.model.Extract}s by N data points (i.e.: when
 * Extracts return time-series data for more than one time) into a matrix of Ex1
 * (where E is the number of Extracts defined in the
 * {@link com.salesforce.pyplyn.configuration.Configuration}) where each of the
 * element is reduced to critical-3, warn-2 or ok-0 based on the duration
 * threshold testing
 *
 * @author Jing Qian &lt;jqian@salesforce.com&gt;
 *
 * @since 6.0
 */
@Value.Immutable
@PyplynImmutableStyle
@JsonDeserialize(as = ImmutableThresholdMetForDuration.class)
@JsonSerialize(as = ImmutableThresholdMetForDuration.class)
@JsonTypeName("ThresholdMetForDuration")
public abstract class ThresholdMetForDuration implements Transform {
    private static final long serialVersionUID = -4594665847342745577L;
    private static final String MESSAGE_TEMPLATE = "%s threshold hit by %s, with value=%s %s %.2f, duration longer than %s";

    public abstract Double threshold();

    public abstract ThresholdType type();

    public abstract Long criticalDurationMillis();

    public abstract Long warnDurationMillis();

    public abstract Long infoDurationMillis();

    /**
     * Applies this transformation and returns a new {@link Transmutation} matrix
     */
    @Override
    public List<List<Transmutation>> apply(List<List<Transmutation>> input) {
        return input.stream()
                .map(this::applyThreshold)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());        
    }

    /**
     * Check for the input points, compare each with threshold, if it continue
     *   to pass the threshold for the critical/warn/info duration time period, change
     *   its the value to be critical/warn/info, otherwise, it's ok
     */
    List<Transmutation> applyThreshold(List<Transmutation> points) {
        // nothing to do
        if (points.isEmpty()) {
            return null;
        }

        Transmutation lastPoint = Iterables.getLast(points);
        ZonedDateTime lastPointTS = lastPoint.time();

        // get the timestamp for the critical, warning, info durations
        //   if the millisecond unit is not supported, it will throw UnsupportedTemporalTypeException
        ZonedDateTime infoDurationTS = lastPointTS.minus(infoDurationMillis(), ChronoUnit.MILLIS);
        ZonedDateTime warnDurationTS = lastPointTS.minus(warnDurationMillis(), ChronoUnit.MILLIS);
        ZonedDateTime criticalDurationTS = lastPointTS.minus(criticalDurationMillis(), ChronoUnit.MILLIS);

        ListIterator<Transmutation> iter = points.listIterator(points.size());
        boolean matchThreshold = true;
        boolean atWarningLevel = false;
        boolean atInfoLevel = false;

        while (iter.hasPrevious() && matchThreshold) {
            Transmutation result = iter.previous();
            ZonedDateTime pointTS = result.time();

            Number value = result.value();
            matchThreshold = type().matches(value, threshold());

            if (matchThreshold) {
                if (pointTS.compareTo(criticalDurationTS) <= 0) {
                    return Collections.singletonList(appendMessage(changeValue(result, CRIT.value()), CRIT.code(), threshold(), criticalDurationMillis()));

                } else if (pointTS.compareTo(warnDurationTS) <= 0) {
                    atWarningLevel = true;

                } else if (pointTS.compareTo(infoDurationTS) <= 0) {
                    atInfoLevel = true;
                }

            } else {
                if (pointTS.compareTo(warnDurationTS) <= 0) {
                    return Collections.singletonList(appendMessage(changeValue(result, WARN.value()), WARN.code(), threshold(), warnDurationMillis()));

                } else if (pointTS.compareTo(infoDurationTS) <= 0) {
                    return Collections.singletonList(appendMessage(changeValue(result, INFO.value()), INFO.code(), threshold(), warnDurationMillis()));

                } else {
                    return Collections.singletonList(changeValue(result, OK.value())); // OK status
                }
            }
        }

        // critical, warning or info duration value is longer than available input time series
        return atWarningLevel
                ? Collections.singletonList(appendMessage(changeValue(lastPoint, WARN.value()), WARN.code(), threshold(), warnDurationMillis()))
                : (atInfoLevel
                        ? Collections.singletonList(appendMessage(changeValue(lastPoint, INFO.value()), INFO.code(), threshold(), warnDurationMillis()))
                        : Collections.singletonList(changeValue(lastPoint, OK.value())));
    }

    /**
     * Appends a message with the explanation of what threshold was hit
     */
    Transmutation appendMessage(Transmutation result, String code, Double threshold, long durationMillis) {
        String thresholdHitAlert = String.format(MESSAGE_TEMPLATE, code, result.name(),
                formatNumber(result.originalValue()), type().name(), threshold, convertToTimeDuration(durationMillis));

        return ImmutableTransmutation.builder().from(result)
                .metadata(ImmutableTransmutation.Metadata.builder()
                        .addMessages(thresholdHitAlert)
                        .build())
                .build();
    }

    /**
     * Convert duration milli to day:hour:min:second dd:hh:mm:ss
     * <p/>omitted any duration that's less than 1 second
     * 
     * @return if less than 1 day, xxh:xxm:xxs
     *         <p/>if more than 1 day, xx days xxh:xxm:xxs
     */
    private String convertToTimeDuration(long milliseconds) {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        String hms = String.format("%02dh:%02dm:%02ds",
                TimeUnit.MILLISECONDS.toHours(milliseconds) % TimeUnit.DAYS.toHours(1),
                TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1));
        return (days > 0 ? String.format("%02ddays ", days) : "") + hms;
    }
}

