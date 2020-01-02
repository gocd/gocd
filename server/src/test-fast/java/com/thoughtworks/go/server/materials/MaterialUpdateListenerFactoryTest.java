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
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MaterialUpdateListenerFactoryTest {
    private static final int NUMBER_OF_CONSUMERS = 5;
    private static final int NUMBER_OF_CONFIG_CONSUMERS = 1;

    @Mock private MaterialRepository materialRepository;
    @Mock private SystemEnvironment systemEnvironment;
    @Mock private GoDiskSpaceMonitor diskSpaceMonitor;
    @Mock private ServerHealthService healthService;
    @Mock private MaterialUpdateCompletedTopic topic;
    @Mock private MaterialUpdateQueue queue;
    @Mock private ConfigMaterialUpdateQueue configQueue;
    @Mock private DependencyMaterialUpdater dependencyMaterialUpdater;
    @Mock private ScmMaterialUpdater scmMaterialUpdater;
    @Mock private PackageMaterialUpdater packageMaterialUpdater;
    @Mock private PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private MaterialExpansionService materialExpansionService;
    @Mock private MDUPerformanceLogger mduPerformanceLogger;
    @Mock private DependencyMaterialUpdateQueue dependencyMaterialQueue;
    @Mock private MaintenanceModeService maintenanceModeService;
    @Mock ConfigMaterialPostUpdateQueue configMaterialPostUpdateQueue;
    @Mock private GoConfigService goConfigService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldCreateCompetingConsumersForSuppliedQueue() {
        when(systemEnvironment.getNumberOfMaterialCheckListener()).thenReturn(NUMBER_OF_CONSUMERS);

        MaterialUpdateListenerFactory factory = new MaterialUpdateListenerFactory(topic, queue, configQueue,
                materialRepository, systemEnvironment, healthService, diskSpaceMonitor,
                transactionTemplate, dependencyMaterialUpdater, scmMaterialUpdater,
                packageMaterialUpdater, pluggableSCMMaterialUpdater, materialExpansionService, mduPerformanceLogger,
                dependencyMaterialQueue, maintenanceModeService, configMaterialPostUpdateQueue, goConfigService);
        factory.init();

        verify(queue, new Times(NUMBER_OF_CONSUMERS)).addListener(any(GoMessageListener.class));
    }

    @Test
    public void shouldCreateCompetingConsumersForSuppliedConfigQueue() {
        when(systemEnvironment.getNumberOfConfigMaterialCheckListener()).thenReturn(NUMBER_OF_CONFIG_CONSUMERS);

        MaterialUpdateListenerFactory factory = new MaterialUpdateListenerFactory(topic, queue, configQueue,
                materialRepository, systemEnvironment, healthService, diskSpaceMonitor,
                transactionTemplate, dependencyMaterialUpdater, scmMaterialUpdater,
                packageMaterialUpdater, pluggableSCMMaterialUpdater, materialExpansionService, mduPerformanceLogger,
                dependencyMaterialQueue, maintenanceModeService, configMaterialPostUpdateQueue, goConfigService);
        factory.init();

        verify(configQueue, new Times(NUMBER_OF_CONFIG_CONSUMERS)).addListener(any(GoMessageListener.class));
    }

    @Test
    public void shouldCreateCompetingConsumersForSuppliedDependencyMaterialQueue() {
        int noOfDependencyMaterialCheckListeners = 3;

        when(systemEnvironment.getNumberOfDependencyMaterialUpdateListeners()).thenReturn(noOfDependencyMaterialCheckListeners);

        MaterialUpdateListenerFactory factory = new MaterialUpdateListenerFactory(topic, queue, configQueue,
                materialRepository, systemEnvironment, healthService, diskSpaceMonitor,
                transactionTemplate, dependencyMaterialUpdater, scmMaterialUpdater,
                packageMaterialUpdater, pluggableSCMMaterialUpdater, materialExpansionService, mduPerformanceLogger,
                dependencyMaterialQueue, maintenanceModeService, configMaterialPostUpdateQueue, goConfigService);
        factory.init();

        verify(dependencyMaterialQueue, new Times(noOfDependencyMaterialCheckListeners)).addListener(any(GoMessageListener.class));
    }
}
