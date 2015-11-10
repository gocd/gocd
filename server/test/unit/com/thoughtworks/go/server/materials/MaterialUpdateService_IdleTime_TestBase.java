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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.go.config.GoConfigWatchList;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookMaterialTypeResolver;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.helper.MaterialUpdateMessageMatcher;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class MaterialUpdateService_IdleTime_TestBase {
    private MaterialUpdateQueue queue;
    private MaterialUpdateCompletedTopic completed;
    private GoConfigService goConfigService;
    private MaterialUpdateService service;
    private PostCommitHookMaterialTypeResolver postCommitHookMaterialType;
    private ServerHealthService serverHealthService;
    private SystemEnvironment systemEnvironment;
    private MaterialConfigConverter materialConfigConverter;
    private MDUPerformanceLogger mduPerformanceLogger;
    private ConfigMaterialUpdateQueue configQueue;
    private GoConfigWatchList watchList;

    @Before
    public void setUp() throws Exception {
        queue = mock(MaterialUpdateQueue.class);
        completed = mock(MaterialUpdateCompletedTopic.class);
        goConfigService = mock(GoConfigService.class);
        postCommitHookMaterialType = mock(PostCommitHookMaterialTypeResolver.class);
        serverHealthService = mock(ServerHealthService.class);
        systemEnvironment = new SystemEnvironment();
        materialConfigConverter = mock(MaterialConfigConverter.class);
        mduPerformanceLogger = mock(MDUPerformanceLogger.class);
        configQueue = mock(ConfigMaterialUpdateQueue.class);
        watchList = mock(GoConfigWatchList.class);
        service = initializeMaterialUpdateService();

        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<MaterialConfig>(Collections.singleton(material().config()));
        when(goConfigService.getSchedulableMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(new HashSet<Material>(Collections.singleton(material())));
    }

    private MaterialUpdateService initializeMaterialUpdateService() {
        return new MaterialUpdateService(queue,configQueue, completed, watchList, goConfigService, systemEnvironment, serverHealthService,
                postCommitHookMaterialType, mduPerformanceLogger, materialConfigConverter);
    }

    protected abstract Material material();

    @After
    public void tearDown() throws Exception {
        systemEnvironment.reset(SystemEnvironment.MATERIAL_UPDATE_INACTIVE_TIMEOUT);
    }

    @Test
    public void shouldReSendMaterialUpdateCheckMessageIfMaterialHasBeenChecked() throws Exception {
        service = spy(service);
        when(service.hasUpdateIntervalElapsedForScmMaterial(material())).thenReturn(true);
        service.onTimer();
        service.onMessage(new MaterialUpdateSuccessfulMessage(material(), 0));
        service.onTimer();
        Mockito.verify(queue, new Times(2)).post(MaterialUpdateMessageMatcher.matchMaterialUpdateMessage(material()));
    }

    @Test
    public void shouldUpdateMaterialOnlyAfterCompletionOfPreviousUpdatePlusIntervalTime() throws Exception {
        service = spy(service);
        when(service.hasUpdateIntervalElapsedForScmMaterial(material())).thenReturn(true);
        service.onTimer();
        verify(service).updateMaterial(material());
        verify(service).hasUpdateIntervalElapsedForScmMaterial(material());
    }

    @Test
    public void shouldNotUpdateMaterialIfUpdateIntervalTimeHasNotElapsedSinceLastUpdate() throws Exception {
        service = spy(service);
        when(service.hasUpdateIntervalElapsedForScmMaterial(material())).thenReturn(false);
        service.onTimer();
        verify(service, never()).updateMaterial(material());
        verify(service).hasUpdateIntervalElapsedForScmMaterial(material());
    }

    @Test
    public void shouldReturnTrueIfMaterialUpdateElapsedTimeIsEqualToOrGreaterThanUpdateInterval() throws Exception {
        systemEnvironment.setProperty(SystemEnvironment.MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, "2000");
        service = initializeMaterialUpdateService();

        Map<Material, Long> materialLastUpdateTimeMap = (Map<Material, Long>) ReflectionUtil.getField(service, "materialLastUpdateTimeMap");
        materialLastUpdateTimeMap.put(material(), System.currentTimeMillis() - 10000); //Assuming material completed update 10 secs ago.

        assertThat(service.hasUpdateIntervalElapsedForScmMaterial(material()), is(true));
    }

    @Test
    public void shouldReturnFalseIfMaterialUpdateElapsedTimeIsLessThanUpdateInterval() throws Exception {
        systemEnvironment.setProperty(SystemEnvironment.MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, "10000");
        service = initializeMaterialUpdateService();

        Map<Material, Long> materialLastUpdateTimeMap = (Map<Material, Long>) ReflectionUtil.getField(service, "materialLastUpdateTimeMap");
        materialLastUpdateTimeMap.put(material(), System.currentTimeMillis() - 2000); //Assuming material completed update 2 secs ago.

        assertThat(service.hasUpdateIntervalElapsedForScmMaterial(material()), is(false));
    }

    @Test
    public void shouldUpdateMaterialSecondTimeIfLastUpdateCompleteIntervalHasElapsed() throws Exception {
        systemEnvironment.setProperty(SystemEnvironment.MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, "1000");
        service = initializeMaterialUpdateService();
        service = spy(service);
        Set<MaterialConfig> schedulableMaterials = goConfigService.getSchedulableMaterials();
        Set<Material> materials = new HashSet<Material>();
        materials.add(material());
        when(materialConfigConverter.toMaterials(schedulableMaterials)).thenReturn(materials);

        service.onTimer();
        service.onMessage(new MaterialUpdateSuccessfulMessage(material(), 0));

        Thread.sleep(2000);
        service.onTimer();
        verify(service, times(2)).updateMaterial(material());
    }

    @Test
    public void shouldNotUpdateMaterialSecondTimeIfLastUpdateCompleteIntervalHasNotElapsed() throws Exception {
        systemEnvironment.setProperty(SystemEnvironment.MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, "10000");
        service = initializeMaterialUpdateService();
        service = spy(service);

        service.onTimer();
        service.onMessage(new MaterialUpdateSuccessfulMessage(material(), 0));

        Thread.sleep(1000);
        service.onTimer();
        verify(service).updateMaterial(material());
    }

    @Test
    public void shouldNotCheckElapsedLastMaterialUpdateIntervalForDependencyMaterial() throws Exception {
        DependencyMaterialConfig dependencyMaterialConfig = mock(DependencyMaterialConfig.class);
        DependencyMaterial dependencyMaterial = mock(DependencyMaterial.class);
        Set<MaterialConfig> materialConfigs = new HashSet<MaterialConfig>(Collections.singleton(dependencyMaterialConfig));
        Set<Material> materials = new HashSet<Material>(Collections.singleton(dependencyMaterial));
        when(goConfigService.getSchedulableMaterials()).thenReturn(materialConfigs);
        when(materialConfigConverter.toMaterials(materialConfigs)).thenReturn(materials);

        systemEnvironment.setProperty(SystemEnvironment.MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, "100000");
        service = initializeMaterialUpdateService();
        service = spy(service);

        service.onTimer();
        service.onTimer();
        verify(service, times(2)).updateMaterial(dependencyMaterial);
    }

}
