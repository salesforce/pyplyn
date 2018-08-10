/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package io.pyplyn.influxdb;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.salesforce.pyplyn.client.AbstractRemoteClient;
import com.salesforce.pyplyn.client.UnauthorizedException;
import com.salesforce.pyplyn.configuration.EndpointConnector;

import io.pyplyn.influxdb.model.Point;
import io.pyplyn.influxdb.model.Results;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;

/**
 * Refocus API implementation
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 1.0
 */
public class InfluxDbClient extends AbstractRemoteClient<InfluxDbService> {


    /**
     * Max number of {@link Point}s that can be written to InfluxDB in one batch
     */
    public final static int MAX_BATCH_SIZE = 5_000;

    /**
     * Text plain type to use for writing data to InfluxDB
     */
    private final static MediaType TEXT_PLAIN = MediaType.parse("text/plain");

    /**
     * Default simplified constructor that uses the specified connection defaults
     *
     * @param connector The InfluxDB API endpoint to use in calls
     */
    public InfluxDbClient(EndpointConnector connector) {
        super(connector, InfluxDbService.class);
    }

    public <T> void writePoint(String db, Point<T> point) throws UnauthorizedException {
        writePoints(db, singletonList(point));
    }

    public <T> void writePoints(String db, List<Point<T>> points) throws UnauthorizedException {
        Preconditions.checkArgument(points.size() < MAX_BATCH_SIZE,
                "Cannot write %d points (> %d) in one batch!",
                points.size(),
                MAX_BATCH_SIZE
        );
        String data = points.stream().map(Object::toString).collect(Collectors.joining("\n"));
        Call<Void> voidCall = svc().writePoints(db, RequestBody.create(TEXT_PLAIN, data));
        executeAndRetrieveBody(voidCall, null);
    }

    public Results query(String db, String query) throws UnauthorizedException {
        return executeAndRetrieveBody(svc().query(db, query), null);
    }

    /**
     * @return true if the current client is authenticated against its endpoint
     */
    @Override
    public boolean isAuthenticated() {
        return true;
    }

    /**
     * Not implemented
     */
    @Override
    protected boolean auth() throws UnauthorizedException {
        return true;
    }

    /**
     * Not implemented
     */
    @Override
    protected void resetAuth() {

    }

}