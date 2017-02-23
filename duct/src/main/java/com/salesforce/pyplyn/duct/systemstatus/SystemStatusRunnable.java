/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.systemstatus;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.status.*;
import com.salesforce.pyplyn.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static com.salesforce.pyplyn.status.AlertType.GREATER_THAN;
import static com.salesforce.pyplyn.status.AlertType.LESS_THAN;

/**
 * System status monitor
 * <p/>Annotated as Singleton as there should only be one instance of this class in operation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
@Singleton
public class SystemStatusRunnable implements SystemStatus {
    private static final Logger logger = LoggerFactory.getLogger(SystemStatusRunnable.class);

    private static final String SYSTEM_STATUS = "System status";
    private static final String METER_TEMPLATE = "%s %s=%s/s";
    private static final String TIMER_TEMPLATE = "p95(%s)=%s";

    private final Map<String, Double> thresholds;
    private final MetricRegistry registry = new MetricRegistry();
    private final List<SystemStatusConsumer> consumers = new ArrayList<>();


    /**
     * Class constructor
     * <p/>If the alert section is not defined in the appConfig, the thresholds map will be empty
     *   and if the thread is run, it will always report status OK.
     * <p/>
     * <p/>However, since the runnable is only scheduled in {@link com.salesforce.pyplyn.duct.app.AppBootstrap}
     *   if {@link AppConfig.Alert#isEnabled()} is false, this should not happen
     *
     * @param appConfig {@link AppConfig} object, where the alert thresholds are retrieved from
     */
    @Inject
    public SystemStatusRunnable(AppConfig appConfig) {
        this.thresholds = Optional.ofNullable(appConfig.alert()).map(AppConfig.Alert::thresholds).orElse(Collections.emptyMap());
    }

    /**
     * @param name Metered name
     * @param type Type of meter
     * @return an initialized {@link Meter}
     */
    @Override
    public Meter meter(String name, MeterType type) {
        return registry.meter(buildMeterName(name, type));
    }

    /**
     * @param name the name of the timer
     * @param method the method being timed
     * @return an initialized {@link Timer}
     */
    @Override
    public Timer timer(String name, String method) {
        return registry.timer(name(name, method));
    }

    /**
     * Registers a consumer for later processing
     * @param statusConsumers list of consumers of status messages
     */
    @Inject
    public void register(Set<SystemStatusConsumer> statusConsumers) {
        statusConsumers.forEach(this.consumers::add);
    }

    /**
     * Collects rates of all registered meters
     *   and publish to all consumers
     */
    @Override
    public void run() {
        final List<StatusMessage> messages = new ArrayList<>();

        // iterate through all registered meters
        for (Map.Entry<String, Meter> entry : registry.getMeters().entrySet()) {
            String meterName = entry.getKey();
            double fiveMinuteRate = entry.getValue().getFiveMinuteRate();

            // define optionals for checking ERR/WARN
            Optional<StatusMessage> errMessage = checkRateOfMeter(meterName, AlertLevel.ERR, fiveMinuteRate);
            Optional<StatusMessage> warnMessage = checkRateOfMeter(meterName, AlertLevel.WARN, fiveMinuteRate);

            // add any messages to the list
            if (errMessage.isPresent()) {
                messages.add(errMessage.get());
            } else if (warnMessage.isPresent()) {
                messages.add(warnMessage.get());
            }
        }

        // if no messages have been logged, report status OK
        if (messages.isEmpty()) {
            messages.add(new StatusMessage(AlertLevel.OK, SYSTEM_STATUS + " " + AlertLevel.OK.name()));
        }

        // iterate through all registered timers
        for (Map.Entry<String, Timer> entry : registry.getTimers().entrySet()) {
            String timerName = entry.getKey();
            long percentileMillis = TimeUnit.NANOSECONDS.toMillis((long)entry.getValue().getSnapshot().get95thPercentile());

            // log each timer's 95th percentile
            logger.info(createTimerStatusMessage(timerName, percentileMillis).toString());
        }

        // send status to all consumers
        consumers.parallelStream().forEach(consumer -> consumer.accept(messages));
    }

    /**
     * Creates a meter name by combining the meter's name and type
     */
    private static String buildMeterName(String name, MeterType type) {
        return name + type.name();
    }


    /**
     * Checks the rate of a meter by reading it from {@link AppConfig} then comparing against the meter's rate
     *
     * @return WARN/ERR message or empty if not triggered
     */
    private Optional<StatusMessage> checkRateOfMeter(String meterName, AlertLevel level, double fiveMinuteRate) {
        // get meter type or stop if not found
        Optional<MeterType> meterType = getMeterType(meterName);
        if (!meterType.isPresent()) {
            return Optional.empty();
        }

        // get specified threshold, or stop if not found
        Optional<Double> thresholdFor = getThresholdFor(meterName, level);
        if (!thresholdFor.isPresent()) {
            return Optional.empty();
        }

        MeterType type = meterType.get();
        Double threshold = thresholdFor.get();

        // check greater-than threshhold; the alert is fired when threshold is hit (inclusive)
        if (type.alertType() == GREATER_THAN && fiveMinuteRate >= threshold) {
            return Optional.of(createMetricStatusMessage(meterName, level, fiveMinuteRate));

        // check less-than threshhold; the alert is fired when rate goes below threshold (exclusive)
        } else if (type.alertType() == LESS_THAN && fiveMinuteRate <= threshold) {
            return Optional.of(createMetricStatusMessage(meterName, level, fiveMinuteRate));
        }

        // log each metric's rate
        logger.info(createMetricStatusMessage(meterName, AlertLevel.OK, fiveMinuteRate).toString());

        return Optional.empty();
    }

    /**
     * @return a {@link StatusMessage} that reports system status based on {@link Metric} values
     *         using thresholds specified in {@link AppConfig.Alert}
     */
    private static StatusMessage createMetricStatusMessage(String meterName, AlertLevel level, double fiveMinuteRate) {
        return new StatusMessage(level, String.format(METER_TEMPLATE, level.name(), meterName, fiveMinuteRate));
    }

    /**
     * @return a {@link StatusMessage} that reports {@link Timer} values
     */
    private static StatusMessage createTimerStatusMessage(String timerName, long percentileMillis) {
        return new StatusMessage(AlertLevel.OK, String.format(TIMER_TEMPLATE, timerName, FormatUtils.formatMillisOrSeconds(percentileMillis)));
    }

    /**
     * Find the type of the specified meter
     */
    private Optional<MeterType> getMeterType(final String name) {
        return Arrays.stream(MeterType.values()).filter(type -> name.contains(type.name())).findAny();
    }


    /**
     * Returns the threshold for the specified meter name and level
     * <p/>If the threshold is not found, it will return an empty optional
     *
     * @param name concatenated string such as <i>MeterNameAlertName</i>
     * @param threshold name of the {@link AlertLevel} to retrieve the threshold for
     */
    private Optional<Double> getThresholdFor(String name, AlertLevel threshold) {
        return Optional.ofNullable(thresholds.get(name + threshold.name()));
    }
}
