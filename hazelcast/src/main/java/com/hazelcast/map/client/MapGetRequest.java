/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.client;

import com.hazelcast.client.KeyBasedClientRequest;
import com.hazelcast.client.RetryableRequest;
import com.hazelcast.client.SecureRequest;
import com.hazelcast.map.MapContainer;
import com.hazelcast.map.MapPortableHook;
import com.hazelcast.map.MapService;
import com.hazelcast.map.operation.GetOperation;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.MapPermission;
import com.hazelcast.spi.Operation;
import java.io.IOException;

public class MapGetRequest extends KeyBasedClientRequest implements Portable, RetryableRequest, SecureRequest {

    private String name;
    private Data key;
    private boolean async;
    private transient long startTime;

    public MapGetRequest() {
    }

    public MapGetRequest(String name, Data key) {
        this.name = name;
        this.key = key;
    }

    protected Object getKey() {
        return key;
    }

    @Override
    protected Operation prepareOperation() {
        return new GetOperation(name, key);
    }

    @Override
    protected void beforeProcess() {
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void afterResponse() {
        final long latency = System.currentTimeMillis() - startTime;
        final MapService mapService = getService();
        MapContainer mapContainer = mapService.getMapServiceContext().getMapContainer(name);
        if (mapContainer.getMapConfig().isStatisticsEnabled()) {
            mapService.getMapServiceContext().getLocalMapStatsProvider()
                    .getLocalMapStatsImpl(name).incrementGets(latency);
        }
    }

    public void setAsAsync() {
        this.async = true;
    }

    public String getServiceName() {
        return MapService.SERVICE_NAME;
    }

    @Override
    public int getFactoryId() {
        return MapPortableHook.F_ID;
    }

    public int getClassId() {
        return MapPortableHook.GET;
    }

    public void write(PortableWriter writer) throws IOException {
        writer.writeUTF("n", name);
        writer.writeBoolean("a", async);
        final ObjectDataOutput out = writer.getRawDataOutput();
        key.writeData(out);
    }

    public void read(PortableReader reader) throws IOException {
        name = reader.readUTF("n");
        async = reader.readBoolean("a");
        final ObjectDataInput in = reader.getRawDataInput();
        key = new Data();
        key.readData(in);
    }

    public MapPermission getRequiredPermission() {
        return new MapPermission(name, ActionConstants.ACTION_READ);
    }

    @Override
    public String getMethodName() {
        if (async) {
            return "getAsync";
        }
        return "get";
    }

    @Override
    public Object[] getParameters() {
        return new Object[]{key};
    }
}
