/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.messaging.GoMessageChannel;
import com.thoughtworks.go.server.messaging.GoMessageQueue;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MaterialUpdateListenerFactory {
    private MaterialUpdateCompletedTopic topic;
    private final MaterialRepository materialRepository;
    private MaterialUpdateQueue queue;
    private ConfigMaterialUpdateQueue configQueue;
    private DependencyMaterialUpdateQueue dependencyMaterialQueue;
    private MaintenanceModeService maintenanceModeService;
    private ConfigMaterialPostUpdateQueue configMaterialPostUpdateQueue;
    private GoConfigService goConfigService;
    private SystemEnvironment systemEnvironment;
    private final ServerHealthService serverHealthService;
    private final GoDiskSpaceMonitor diskSpaceMonitor;
    private TransactionTemplate transactionTemplate;
    private final DependencyMaterialUpdater dependencyMaterialUpdater;
    private final ScmMaterialUpdater scmMaterialUpdater;
    private final PackageMaterialUpdater packageMaterialUpdater;
    private final PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater;
    private final MaterialExpansionService materialExpansionService;
    private final MDUPerformanceLogger mduPerformanceLogger;

    @Autowired
    public MaterialUpdateListenerFactory(MaterialUpdateCompletedTopic topic,
                                         MaterialUpdateQueue queue,
                                         ConfigMaterialUpdateQueue configQueue,
                                         MaterialRepository materialRepository,
                                         SystemEnvironment systemEnvironment,
                                         ServerHealthService serverHealthService,
                                         GoDiskSpaceMonitor diskSpaceMonitor,
                                         TransactionTemplate transactionTemplate,
                                         DependencyMaterialUpdater dependencyMaterialUpdater,
                                         ScmMaterialUpdater scmMaterialUpdater,
                                         PackageMaterialUpdater packageMaterialUpdater,
                                         PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater,
                                         MaterialExpansionService materialExpansionService,
                                         MDUPerformanceLogger mduPerformanceLogger,
                                         DependencyMaterialUpdateQueue dependencyMaterialQueue,
                                         MaintenanceModeService maintenanceModeService,
                                         ConfigMaterialPostUpdateQueue configMaterialPostUpdateQueue,
                                         GoConfigService goConfigService) {
        this.topic = topic;
        this.queue = queue;
        this.configQueue = configQueue;
        this.materialRepository = materialRepository;
        this.systemEnvironment = systemEnvironment;
        this.serverHealthService = serverHealthService;
        this.diskSpaceMonitor = diskSpaceMonitor;
        this.transactionTemplate = transactionTemplate;
        this.dependencyMaterialUpdater = dependencyMaterialUpdater;
        this.scmMaterialUpdater = scmMaterialUpdater;
        this.packageMaterialUpdater = packageMaterialUpdater;
        this.pluggableSCMMaterialUpdater = pluggableSCMMaterialUpdater;
        this.materialExpansionService = materialExpansionService;
        this.mduPerformanceLogger = mduPerformanceLogger;
        this.dependencyMaterialQueue = dependencyMaterialQueue;
        this.maintenanceModeService = maintenanceModeService;
        this.configMaterialPostUpdateQueue = configMaterialPostUpdateQueue;
        this.goConfigService = goConfigService;
    }

    public void init() {
        int numberOfStandardMaterialListeners = systemEnvironment.getNumberOfMaterialCheckListener();
        int numberOfConfigListeners = systemEnvironment.getNumberOfConfigMaterialCheckListener();
        int numberOfDependencyMaterialCheckListeners = systemEnvironment.getNumberOfDependencyMaterialUpdateListeners();

        for (int i = 0; i < numberOfStandardMaterialListeners; i++) {
            createWorker(this.queue, this.topic);
        }

        for (int i = 0; i < numberOfConfigListeners; i++) {
            createWorker(this.configQueue, this.configMaterialPostUpdateQueue);
        }

        for (int i = 0; i < numberOfDependencyMaterialCheckListeners; i++) {
            createWorker(this.dependencyMaterialQueue, this.topic);
        }
    }

    private void createWorker(GoMessageQueue<MaterialUpdateMessage> queue, GoMessageChannel<MaterialUpdateCompletedMessage> topic) {
        MaterialDatabaseUpdater updater = new MaterialDatabaseUpdater(materialRepository, serverHealthService, transactionTemplate, dependencyMaterialUpdater, scmMaterialUpdater,
                packageMaterialUpdater, pluggableSCMMaterialUpdater, materialExpansionService, goConfigService);
        queue.addListener(new MaterialUpdateListener(topic, updater, mduPerformanceLogger, diskSpaceMonitor, maintenanceModeService));
    }
}
