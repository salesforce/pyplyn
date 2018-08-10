/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.systemstatus;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.Objects.isNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.salesforce.pyplyn.duct.appconfig.AppConfig;
import com.salesforce.pyplyn.model.StatusCode;
import com.salesforce.pyplyn.model.ThresholdType;
import com.salesforce.pyplyn.status.*;
import com.salesforce.pyplyn.util.FormatUtils;

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
    private static final String METER_TEMPLATE = "%s %s=%s/interval";
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
     *   if {@link com.salesforce.pyplyn.duct.appconfig.AppConfig.Alert#isEnabled()} is false, this should not happen
     *
     * @param appConfig {@link com.salesforce.pyplyn.duct.appconfig.AppConfig} object, where the alert thresholds are retrieved from
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
        this.consumers.addAll(statusConsumers);
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
            double meterValue = entry.getValue().getCount();

            // removes the current meter, so that it is re-initialized
            registry.remove(meterName);

            // define optionals for checking CRIT/WARN
            Optional<StatusMessage> errMessage = checkRateOfMeter(meterName, StatusCode.CRIT, meterValue);
            Optional<StatusMessage> warnMessage = checkRateOfMeter(meterName, StatusCode.WARN, meterValue);

            // add any messages to the list
            if (errMessage.isPresent()) {
                messages.add(errMessage.get());
            } else if (warnMessage.isPresent()) {
                messages.add(warnMessage.get());
            }
        }

        // if no messages have been logged, report status OK
        if (messages.isEmpty()) {
            messages.add(new StatusMessage(StatusCode.OK, SYSTEM_STATUS + " " + StatusCode.OK.name()));
        }

        // iterate through all registered timers
        for (Map.Entry<String, Timer> entry : registry.getTimers().entrySet()) {
            String timerName = entry.getKey();
            long percentileMillis = TimeUnit.NANOSECONDS.toMillis((long)entry.getValue().getSnapshot().get95thPercentile());

            // log each timer's 95th percentile
            logStatusMessage(createTimerStatusMessage(SYSTEM_STATUS + " " + timerName, percentileMillis));
        }

        // send status to all consumers
        consumers.parallelStream().forEach(consumer -> consumer.accept(messages));
    }


    /**
     * Logs {@link StatusMessage}s via the slf4j logger
     * <p/>Implemented as a separate method for testing purposes
     */
    void logStatusMessage(StatusMessage statusMessage) {
        logger.info(statusMessage.toString());
    }

    /**
     * Creates a meter name by combining the meter's name and type
     */
    private static String buildMeterName(String name, MeterType type) {
        return name + type.alertType() + type.processStatus();
    }


    /**
     * Checks the specified value of a meter against its WARN/CRIT thresholds defined in {@link AppConfig}
     *
     * @return WARN/CRIT message or empty if not triggered
     */
    private Optional<StatusMessage> checkRateOfMeter(String meterName, StatusCode level, double meterValue) {
        // get meter type or stop if not found
        Optional<MeterType> meterType = getMeterType(meterName);
        if (isNull(meterType)) {
            return Optional.empty();
        }

        // get specified threshold, or stop if not found
        Optional<Double> thresholdFor = getThresholdFor(meterName, level);
        if (isNull(thresholdFor)) {
            return Optional.empty();
        }

        MeterType type = meterType.get();
        Double threshold = thresholdFor.get();


        // check threshold; the alert is fired when the threshold condition is satisfied
        if(type.alertType().matches(meterValue, threshold)){
            return Optional.of(createMeterStatusMessage(meterName, level, meterValue));
        }

        // log each metric's current value
        logStatusMessage(createMeterStatusMessage(SYSTEM_STATUS + " " + meterName, StatusCode.OK, meterValue));

        return Optional.empty();
    }

    /**
     * @return a {@link StatusMessage} that reports system status based on {@link Meter} values
     *         using thresholds specified in {@link AppConfig.Alert}
     */
    private static StatusMessage createMeterStatusMessage(String meterName, StatusCode level, double fiveMinuteRate) {
        return new StatusMessage(level, String.format(METER_TEMPLATE, level.name(), meterName, fiveMinuteRate));
    }

    /**
     * @return a {@link StatusMessage} that reports {@link Timer} values
     */
    private static StatusMessage createTimerStatusMessage(String timerName, long percentileMillis) {
        return new StatusMessage(StatusCode.OK, String.format(TIMER_TEMPLATE, timerName, FormatUtils.formatMillisOrSeconds(percentileMillis)));
    }

    /**
     * Find the type of the specified meter
     */
    private Optional<MeterType> getMeterType(final String name) {
        Optional<MeterType> optionalMeterType = null;
        // name contains alertType and ProcessStatus
        // if both are valid, make a new MeterType and return
        Optional<ProcessStatus> status = Arrays.stream(ProcessStatus.values()).filter(processStatus -> name.contains(processStatus.name())).findFirst();
        if(status.isPresent()){
            Optional<ThresholdType> type = Arrays.stream(ThresholdType.values()).filter(thresholdType -> name.contains(thresholdType.name())).findFirst();
            if(type.isPresent()){
                optionalMeterType = Optional.of(new MeterType(type.get(), status.get()));
            }
        }
        return optionalMeterType;
    }


    /**
     * Returns the threshold for the specified meter name and level
     * <p/>If the threshold is not found, it will return an empty optional
     *
     * @param name concatenated string such as <i>MeterNameAlertName</i>
     * @param threshold name of the {@link StatusCode} to retrieve the threshold for
     */
    private Optional<Double> getThresholdFor(String name, StatusCode threshold) {
        return Optional.ofNullable(thresholds.get(name + threshold.name()));
    }
}
