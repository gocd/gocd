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

import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.messaging.GoMessageQueue;
import com.thoughtworks.go.server.messaging.GoMessageTopic;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MaterialUpdateListenerFactory {
    private MaterialUpdateCompletedTopic topic;
    private ConfigMaterialUpdateCompletedTopic configTopic;
    private final MaterialRepository materialRepository;
    private MaterialUpdateQueue queue;
    private ConfigMaterialUpdateQueue configQueue;
    private SystemEnvironment systemEnvironment;
    private final ServerHealthService serverHealthService;
    private final GoDiskSpaceMonitor diskSpaceMonitor;
    private TransactionTemplate transactionTemplate;
    private GoCache goCache;
    private final DependencyMaterialUpdater dependencyMaterialUpdater;
    private final ScmMaterialUpdater scmMaterialUpdater;
    private final PackageMaterialUpdater packageMaterialUpdater;
    private final PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater;
    private final MaterialExpansionService materialExpansionService;
    private final MDUPerformanceLogger mduPerformanceLogger;

    @Autowired
    public MaterialUpdateListenerFactory(MaterialUpdateCompletedTopic topic,
                                         ConfigMaterialUpdateCompletedTopic configTopic,
                                         MaterialUpdateQueue queue,
                                         ConfigMaterialUpdateQueue configQueue,
                                         MaterialRepository materialRepository,
                                         SystemEnvironment systemEnvironment,
                                         ServerHealthService serverHealthService,
                                         GoDiskSpaceMonitor diskSpaceMonitor,
                                         TransactionTemplate transactionTemplate,
                                         GoCache goCache,
                                         DependencyMaterialUpdater dependencyMaterialUpdater,
                                         ScmMaterialUpdater scmMaterialUpdater,
                                         PackageMaterialUpdater packageMaterialUpdater, PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater, MaterialExpansionService materialExpansionService, MDUPerformanceLogger mduPerformanceLogger) {
        this.topic = topic;
        this.configTopic = configTopic;
        this.queue = queue;
        this.configQueue = configQueue;
        this.materialRepository = materialRepository;
        this.systemEnvironment = systemEnvironment;
        this.serverHealthService = serverHealthService;
        this.diskSpaceMonitor = diskSpaceMonitor;
        this.transactionTemplate = transactionTemplate;
        this.goCache = goCache;
        this.dependencyMaterialUpdater = dependencyMaterialUpdater;
        this.scmMaterialUpdater = scmMaterialUpdater;
        this.packageMaterialUpdater = packageMaterialUpdater;
        this.pluggableSCMMaterialUpdater = pluggableSCMMaterialUpdater;
        this.materialExpansionService = materialExpansionService;
        this.mduPerformanceLogger = mduPerformanceLogger;
    }

    public void init(){
        int numberOfStandardMaterialListeners = systemEnvironment.getNumberOfMaterialCheckListener();
        int numberOfConfigListeners = systemEnvironment.getNumberOfConfigMaterialCheckListener();

        for (int i = 0; i < numberOfStandardMaterialListeners; i++) {
            createWorker(this.queue, this.topic);
        }
        for (int i = 0; i < numberOfConfigListeners; i++) {
            createWorker(this.configQueue,this.configTopic);
        }
    }

    private void createWorker(GoMessageQueue<MaterialUpdateMessage> queue, GoMessageTopic<MaterialUpdateCompletedMessage> topic) {
        MaterialDatabaseUpdater updater = new MaterialDatabaseUpdater(materialRepository, serverHealthService, transactionTemplate, goCache, dependencyMaterialUpdater, scmMaterialUpdater,
                packageMaterialUpdater, pluggableSCMMaterialUpdater, materialExpansionService);
        queue.addListener(new MaterialUpdateListener(topic, updater, mduPerformanceLogger, diskSpaceMonitor));
    }
}