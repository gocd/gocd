/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.perf;

import com.thoughtworks.go.domain.materials.Material;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MDUPerformanceLogger {
    private PerformanceLogger performanceLogger;
    private static long currentTrackingId = 0;

    @Autowired
    public MDUPerformanceLogger(PerformanceLogger performanceLogger) {
        this.performanceLogger = performanceLogger;
    }

    public long materialSentToUpdateQueue(Material material) {
        long trackingId = currentTrackingId++;

        performanceLogger.log("MDU-QUEUE-PUT {} {} {}", trackingId, material.getFingerprint(), material.getDisplayName());
        return trackingId;
    }

    public void pickedUpMaterialForMDU(long trackingId, Material material) {
        performanceLogger.log("MDU-START {} {} {}", trackingId, material.getFingerprint(), material.getDisplayName());
    }

    public void postingMessageAboutMDUCompletion(long trackingId, Material material) {
        performanceLogger.log("MDU-DONE {} {} {}", trackingId, material.getFingerprint(), material.getDisplayName());
    }

    public void postingMessageAboutMDUFailure(long trackingId, Material material) {
        performanceLogger.log("MDU-FAIL {} {} {}", trackingId, material.getFingerprint(), material.getDisplayName());
    }

    public void completionMessageForMaterialReceived(long trackingId, Material material) {
        performanceLogger.log("MDU-QUEUE-REMOVE {} {} {}", trackingId, material.getFingerprint(), material.getDisplayName());
    }

}
