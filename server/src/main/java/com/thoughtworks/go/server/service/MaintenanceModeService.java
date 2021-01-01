/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.service;

import com.google.gson.internal.bind.util.ISO8601Utils;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.domain.ServerMaintenanceMode;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.util.DateUtils.UTC;

@Service
public class MaintenanceModeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceModeService.class);
    private static ConcurrentHashMap<String, MaterialPerformingMDU> runningMDUs = new ConcurrentHashMap<>();
    private ServerMaintenanceMode serverMaintenanceMode;
    private TimeProvider timeProvider;

    @Autowired
    public MaintenanceModeService(TimeProvider timeProvider, SystemEnvironment systemEnvironment) {
        this.timeProvider = timeProvider;
        if (systemEnvironment.shouldStartServerInMaintenanceMode()) {
            this.serverMaintenanceMode = new ServerMaintenanceMode(true, "GoCD", timeProvider.currentTime());
        } else {
            this.serverMaintenanceMode = new ServerMaintenanceMode();
        }
    }

    public ServerMaintenanceMode get() {
        return serverMaintenanceMode;
    }

    public void update(ServerMaintenanceMode fromRequest) {
        LOGGER.info("[Maintenance Mode] Server maintenance mode state updated to 'isMaintenanceMode={}' by '{}' at '{}'.", fromRequest.isMaintenanceMode(), fromRequest.updatedBy(), fromRequest.updatedOn());
        this.serverMaintenanceMode = fromRequest;
    }

    public boolean isMaintenanceMode() {
        return get().isMaintenanceMode();
    }

    private Timestamp updatedOnTimeStamp() {
        if (get().isMaintenanceMode()) {
            return get().updatedOn();
        }
        throw new IllegalStateException("GoCD server is not in maintenance mode!");
    }

    public String updatedOn() {
        return ISO8601Utils.format(updatedOnTimeStamp(), false, UTC);
    }

    public String updatedBy() {
        if (get().isMaintenanceMode()) {
            return get().updatedBy();
        }
        throw new IllegalStateException("GoCD server is not in maintenance mode!");
    }

    public Collection<MaterialPerformingMDU> getRunningMDUs() {
        return runningMDUs.values();
    }

    public void mduStartedForMaterial(Material material) {
        runningMDUs.put(material.getFingerprint(), new MaterialPerformingMDU(material, new Timestamp(timeProvider.currentTimeMillis())));
    }

    public void mduFinishedForMaterial(Material material) {
        runningMDUs.remove(material.getFingerprint());
    }

    public class MaterialPerformingMDU {
        private final Material material;
        private final Timestamp timestamp;

        public MaterialPerformingMDU(Material material, Timestamp timestamp) {
            this.material = material;
            this.timestamp = timestamp;
        }

        public Material getMaterial() {
            return material;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }
    }
}
