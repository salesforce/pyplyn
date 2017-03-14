package com.salesforce.pyplyn.duct.etl.transform.thresholdmetforduration;

import static com.salesforce.pyplyn.util.FormatUtils.formatNumber;
import static com.salesforce.pyplyn.duct.etl.transform.threshold.Threshold.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Iterables;
import com.salesforce.pyplyn.duct.etl.transform.threshold.Threshold.Type;
import com.salesforce.pyplyn.model.Transform;
import com.salesforce.pyplyn.model.TransformationResult;
import com.salesforce.pyplyn.model.builder.TransformationResultBuilder;

/**
 * Determine if the input time series data has matched the specified threshold
 * for the specified critial/warn/info duration, and return
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
 * @since 5.1
 */
public class ThresholdMetForDuration implements Transform, Serializable {
    private static final long serialVersionUID = -4594665847342745577L;
    private final static String MESSAGE_TEMPLATE = "%s threshold hit by %s, with value=%s %s %.2f, duration longer than %s";

    @JsonProperty(required=true)
    Double threshold;

    @JsonProperty
    Type type;

    @JsonProperty(defaultValue = "0")
    Long critialDurationMillis;

    @JsonProperty(defaultValue = "0")
    Long warnDurationMillis;

    @JsonProperty(defaultValue = "0")
    Long infoDurationMillis;

    @Override
    public List<List<TransformationResult>> apply(List<List<TransformationResult>> input) {
        return input.stream()
                .map(this::applyThreshold)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());        
    }

    /**
     * check for the input points, compare each with threshold, if it continue
     * to pass the threshold for the critial/warn/info duration time period, change
     * its the value to be critial/warn/info, otherwise, it's ok
     * 
     * @param points
     * @return
     */
    List<TransformationResult> applyThreshold(List<TransformationResult> points) {
        if (!points.isEmpty()) {
            TransformationResult lastPoint = Iterables.getLast(points, null);
            ZonedDateTime lastPointTS = lastPoint.time();

            // get the timestamp for the critial, warning, info duration timestamp
            ZonedDateTime infoDurationTS = lastPointTS;
            ZonedDateTime warnDurationTS = lastPointTS;
            ZonedDateTime critialDurationTS = lastPointTS;
            // if the milisecond unit is not supported, it would throw
            // UnsupportedTemporalTypeException, which is what we want
            infoDurationTS = lastPointTS.minus(infoDurationMillis, ChronoUnit.MILLIS);
            warnDurationTS = lastPointTS.minus(warnDurationMillis, ChronoUnit.MILLIS);
            critialDurationTS = lastPointTS.minus(critialDurationMillis, ChronoUnit.MILLIS);

            ListIterator<TransformationResult> iter = points.listIterator(points.size());
            boolean matchThreshold = true;
            boolean atWarningLevel = false;
            boolean atInfoLevel = false;
            
            while (iter.hasPrevious() && matchThreshold) {
                TransformationResult result = iter.previous();
                ZonedDateTime pointTS = result.time();

                Number value = result.value();
                matchThreshold = type.matches(value, threshold);

                if (matchThreshold) {
                    if (pointTS.compareTo(critialDurationTS) <= 0) {
                        return Collections.singletonList(
                                appendMessage(changeValue(result, 3d), "CRIT", threshold, critialDurationMillis));
                    } else if (pointTS.compareTo(warnDurationTS) <= 0) {
                        atWarningLevel = true;
                    } else if (pointTS.compareTo(infoDurationTS) <= 0) {
                        atInfoLevel = true;
                    }
                } else {
                    if (pointTS.compareTo(warnDurationTS) <= 0) {
                        return Collections.singletonList(
                                appendMessage(changeValue(result, 2d), "WARN", threshold, warnDurationMillis));
                    } else if (pointTS.compareTo(infoDurationTS) <= 0) {
                        return Collections.singletonList(
                                appendMessage(changeValue(result, 1d), "INFO", threshold, warnDurationMillis));
                    } else {
                        return Collections.singletonList(changeValue(result, 0d)); // OK status
                    }
                }
            }

            // critical, warning or info duration value is longer than available input time series
            return atWarningLevel
                    ? Collections.singletonList(appendMessage(changeValue(lastPoint, 2d), "WARN", threshold, warnDurationMillis))
                    : (atInfoLevel 
                            ? Collections.singletonList(appendMessage(changeValue(lastPoint, 2d), "WARN", threshold, warnDurationMillis)) 
                            : Collections.singletonList(changeValue(lastPoint, 0d)));
        } else {
            return null;
        }
    }

    /**
     * Appends a message with the explanation of what threshold was hit
     */
    TransformationResult appendMessage(TransformationResult result, String code, Double threshold, long durationMillis) {
        String thresholdHitAlert = String.format(MESSAGE_TEMPLATE, code, result.name(),
                formatNumber(result.originalValue()), type.name(), threshold, converToTimeDuration(durationMillis));

        return new TransformationResultBuilder(result).metadata((metadata) -> metadata.addMessage(thresholdHitAlert))
                .build();
    }

    /**
     * convert duration milli to day:hour:min:second dd:hh:mm:ss
     * omitted any duration that's less than 1 second
     * 
     * @param milliseconds
     * @return if less than 1 day, xxh:xxm:xxs
     *         if more than 1 day, xx days xxh:xxm:xxs
     */
    private String converToTimeDuration(long milliseconds) {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        String hms = String.format("%02dh:%02dm:%02ds",
                TimeUnit.MILLISECONDS.toHours(milliseconds) % TimeUnit.DAYS.toHours(1),
                TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1));
        return (days > 0 ? String.format("%02ddays ", days) : "") + hms;
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        int result = 1;
        result = prime * result + ((critialDurationMillis == null) ? 0 : critialDurationMillis.hashCode());
        result = prime * result + ((threshold == null) ? 0 : threshold.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((warnDurationMillis == null) ? 0 : warnDurationMillis.hashCode());
        result = prime * result + ((infoDurationMillis == null) ? 0 : infoDurationMillis.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ThresholdMetForDuration other = (ThresholdMetForDuration) obj;

        if (critialDurationMillis != null ? !critialDurationMillis.equals(other.critialDurationMillis)
                : other.critialDurationMillis != null)
            return false;
        if (warnDurationMillis != null ? !warnDurationMillis.equals(other.warnDurationMillis)
                : other.warnDurationMillis != null)
            return false;
        if (infoDurationMillis != null ? !infoDurationMillis.equals(other.warnDurationMillis)
                : other.infoDurationMillis != null)
            return false;        
        if (threshold != null ? !threshold.equals(other.threshold) : other.threshold != null)
            return false;
        return type == other.type;
    }

}
