/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.GoRepoConfigDataSource;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;

/**
 * @understands when to trigger updates for materials
 */
public class MaterialUpdateListener implements GoMessageListener<MaterialUpdateMessage> {
    private final MaterialUpdateCompletedTopic topic;
    private final MaterialDatabaseUpdater updater;
    private final MDUPerformanceLogger mduPerformanceLogger;
    private final GoDiskSpaceMonitor diskSpaceMonitor;
    private GoRepoConfigDataSource repoConfigDataSource;

    public MaterialUpdateListener(MaterialUpdateCompletedTopic topic, MaterialDatabaseUpdater updater, MDUPerformanceLogger mduPerformanceLogger, GoDiskSpaceMonitor diskSpaceMonitor) {
        this.topic = topic;
        this.updater = updater;
        this.mduPerformanceLogger = mduPerformanceLogger;
        this.diskSpaceMonitor = diskSpaceMonitor;
    }

    public void onMessage(MaterialUpdateMessage message) {
        final Material material = message.getMaterial();

        try {
            mduPerformanceLogger.pickedUpMaterialForMDU(message.trackingId(), material);
            bombIf(diskSpaceMonitor.isLowOnDisk(), "Cruise server is too low on disk to continue with material update");
            updater.updateMaterial(material);
            mduPerformanceLogger.postingMessageAboutMDUCompletion(message.trackingId(), material);
            topic.post(new MaterialUpdateSuccessfulMessage(material, message.trackingId())); //This should happen only if the transaction is committed.
        }
        catch (Exception e) {
            topic.post(new MaterialUpdateFailedMessage(material, message.trackingId(), e));
            mduPerformanceLogger.postingMessageAboutMDUFailure(message.trackingId(), material);
        }
    }
}
