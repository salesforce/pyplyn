/*
 * Copyright, 1999-2018, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */
package com.salesforce.pyplyn.duct.etl.load.refocus;


import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.duct.app.ShutdownHook;
import com.salesforce.pyplyn.status.SystemStatus;
import com.salesforce.refocus.RefocusClient;
import com.salesforce.refocus.model.Sample;
import com.salesforce.refocus.model.UpsertResponse;

/**
 * RefocusBatcher
 *
 * @author Chris Coraggio &lt;chris.coraggio@salesforce.com&gt;
 * @since 11.0.0
 */

public class RefocusBatcher implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RefocusBatcher.class);

    private final RefocusClient client;
    private final SystemStatus status;
    private final ShutdownHook shutdownHook;
    private final String endpointId;

    private final int BATCH_SIZE = 1000; // how big to make each batch of samples

    private final LinkedBlockingDeque<Sample> queue = new LinkedBlockingDeque<>();

    protected RefocusBatcher(RefocusClient client, String endpointId, SystemStatus status, ShutdownHook shutdownHook) {
        this.client = client;
        this.endpointId = endpointId;
        this.status = status;
        this.shutdownHook = shutdownHook;
    }

    public void enqueue(List<Sample> samples) {
        samples.forEach(s -> {
            boolean result = queue.offer(s);
            if(!result){
                logger.error("Failed to send sample: {}", s);
            }
        });
    }

    @Override
    public void run() {
        // Skip if process has been shutdown.
        if (!this.shutdownHook.isShutdown()) {

            // when run, drain all samples from the queue into a collection
            final List<Sample> allSamples = new ArrayList<>();
            queue.drainTo(allSamples);

            // nothing to do
            if (allSamples.size() == 0) {
                return;
            }

            // Split the potentially huge list of samples into smaller batches.
            List<List<Sample>> batchedSamples = Lists.partition(allSamples, BATCH_SIZE);

            logger.info("Sending {} samples in {} batches of {}, samples: {}", allSamples.size(), batchedSamples.size(), BATCH_SIZE, allSamples);

            // Iterate over each batch and send.
            for (List<Sample> batch : batchedSamples) {
                try (Timer.Context context = status.timer(meterName(), "upsert-samples-bulk." + endpointId).time()) {
                    client.authenticate();
                    UpsertResponse upsertResponse = client.upsertSamplesBulk(batch);
                    if (isNull(upsertResponse) || !Objects.equals(upsertResponse.status(), "OK")) {
                        logger.error("Unknown error upserting samples: {}", upsertResponse);
                    } else {
                        logger.info("Successfully sent {} samples...", batch.size());
                    }
                } catch (UnauthorizedException e) {
                    logger.error("Upserting samples failed due to auth error.", e);
                }
            }
        }
    }

    public String meterName() {
        return "Refocus";
    }
}
