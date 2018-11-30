/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.domain.ServerDrainMode;
import com.thoughtworks.go.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DrainModeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrainModeService.class);
    private static ConcurrentHashMap<String, MaterialPerformingMDU> runningMDUs = new ConcurrentHashMap<>();
    private ServerDrainMode drainMode;
    private TimeProvider timeProvider;

    @Autowired
    public DrainModeService(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
        this.drainMode = new ServerDrainMode();
    }

    public ServerDrainMode get() {
        return drainMode;
    }

    public void update(ServerDrainMode fromRequest) {
        LOGGER.debug("[Drain Mode] Server drain mode state updated to 'isDrained={}' by '{}' at '{}'.", fromRequest.isDrainMode(), fromRequest.updatedBy(), fromRequest.updatedOn());
        this.drainMode = fromRequest;
    }

    public boolean isDrainMode() {
        return get().isDrainMode();
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
