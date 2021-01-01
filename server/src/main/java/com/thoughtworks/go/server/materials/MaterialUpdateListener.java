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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.messaging.GoMessageChannel;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;

/**
 * @understands when to trigger updates for materials
 */
public class MaterialUpdateListener implements GoMessageListener<MaterialUpdateMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialUpdateListener.class);

    private final GoMessageChannel<MaterialUpdateCompletedMessage> channel;
    private final MaterialDatabaseUpdater updater;
    private final MDUPerformanceLogger mduPerformanceLogger;
    private final GoDiskSpaceMonitor diskSpaceMonitor;
    private MaintenanceModeService maintenanceModeService;

    public MaterialUpdateListener(GoMessageChannel<MaterialUpdateCompletedMessage> channel, MaterialDatabaseUpdater updater,
                                  MDUPerformanceLogger mduPerformanceLogger, GoDiskSpaceMonitor diskSpaceMonitor, MaintenanceModeService maintenanceModeService) {
        this.channel = channel;
        this.updater = updater;
        this.mduPerformanceLogger = mduPerformanceLogger;
        this.diskSpaceMonitor = diskSpaceMonitor;
        this.maintenanceModeService = maintenanceModeService;
    }

    @Override
    public void onMessage(MaterialUpdateMessage message) {
        final Material material = message.getMaterial();

        if (maintenanceModeService.isMaintenanceMode()) {
            LOGGER.debug("[Maintenance Mode] GoCD server is in 'maintenance' mode, skip performing MDU for material {}.", material);
            channel.post(new MaterialUpdateSkippedMessage(material, message.trackingId()));
            return;
        }

        try {
            maintenanceModeService.mduStartedForMaterial(material);
            mduPerformanceLogger.pickedUpMaterialForMDU(message.trackingId(), material);
            bombIf(diskSpaceMonitor.isLowOnDisk(), "GoCD server is too low on disk to continue with material update");
            updater.updateMaterial(material);
            mduPerformanceLogger.postingMessageAboutMDUCompletion(message.trackingId(), material);
            channel.post(new MaterialUpdateSuccessfulMessage(material, message.trackingId())); //This should happen only if the transaction is committed.
        } catch (Exception e) {
            channel.post(new MaterialUpdateFailedMessage(material, message.trackingId(), e));
            mduPerformanceLogger.postingMessageAboutMDUFailure(message.trackingId(), material);
        } finally {
            maintenanceModeService.mduFinishedForMaterial(material);
        }
    }
}
