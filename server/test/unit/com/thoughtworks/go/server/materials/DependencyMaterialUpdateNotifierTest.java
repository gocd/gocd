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

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class DependencyMaterialUpdateNotifierTest {

    private DependencyMaterialUpdateNotifier notifier;
    private GoConfigService goConfigService;
    private MaterialConfigConverter materialConfigConverter;
    private MaterialUpdateService materialUpdateService;
    private Material dependencyMaterial = MaterialsMother.dependencyMaterial();

    @Before
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        materialConfigConverter = mock(MaterialConfigConverter.class);
        materialUpdateService = mock(MaterialUpdateService.class);
    }

    @Test
    public void shouldListenToConfigChange() {
        EntityConfigChangedListener entityConfigChangedListener = mock(EntityConfigChangedListener.class);
        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);
        notifier = spy(notifier);

        stub(notifier.pipelineConfigChangedListener()).toReturn(entityConfigChangedListener);

        notifier.initialize();

        verify(goConfigService).register(notifier);
        verify(goConfigService).register(entityConfigChangedListener);
    }

    @Test
    public void shouldListenToMaterialUpdateMessage() {
        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);

        notifier.initialize();

        verify(materialUpdateService).registerMaterialUpdateCompleteListener(notifier);
    }

    @Test
    public void configLoadShouldScheduleAllDependencyMaterialsForUpdateThrough_onConfigChangeCallback() {
        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);
        notifier.initialize();

        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(dependencyMaterial.config()));
        when(goConfigService.getSchedulableDependencyMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterial(dependencyMaterial.config())).thenReturn(dependencyMaterial);

        notifier.onConfigChange(mock(CruiseConfig.class));

        verify(materialUpdateService).updateMaterial(dependencyMaterial);
    }

    @Test
    public void shouldScheduleOnlyNewDepenedencyMaterialsForUpdateOnSubsequentConfigChanges() {
        DependencyMaterial dependencyMaterialForP1S1 = MaterialsMother.dependencyMaterial("p1", "s1");
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(dependencyMaterialForP1S1.config()));
        when(goConfigService.getSchedulableDependencyMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterial(dependencyMaterialForP1S1.config())).thenReturn(dependencyMaterialForP1S1);

        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);
        notifier.initialize();

        DependencyMaterial dependencyMaterialForP2S2 = MaterialsMother.dependencyMaterial("p2", "s2");
        schedulableMaterialConfigs = new HashSet<>(Arrays.asList(dependencyMaterialForP2S2.config()));
        when(goConfigService.getSchedulableDependencyMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterial(dependencyMaterialForP2S2.config())).thenReturn(dependencyMaterialForP2S2);

        notifier.onConfigChange(mock(CruiseConfig.class));
        verify(materialUpdateService).updateMaterial(dependencyMaterialForP2S2);
    }

    @Test
    public void shouldDoNothingIfOnConfigChangeIfNewDependencyMaterialsAreNotAdded() {
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(dependencyMaterial.config()));

        when(goConfigService.getSchedulableDependencyMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterial(dependencyMaterial.config())).thenReturn(dependencyMaterial);

        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);
        notifier.initialize();

        notifier.onConfigChange(mock(CruiseConfig.class));

//        updated only once during initialization
        verify(materialUpdateService, times(1)).updateMaterial(dependencyMaterial);
    }

    @Test
    public void shouldUpdateMaterialOnStageChange() {
        Stage stage = StageMother.passedStageInstance("Stage1", "plan", "Pipeline1");
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial(stage.getIdentifier().getPipelineName(), stage.getName());
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(dependencyMaterial.config()));

        when(goConfigService.getSchedulableDependencyMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterial(dependencyMaterial.config())).thenReturn(dependencyMaterial);

        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);
        notifier.initialize();

        notifier.stageStatusChanged(stage);

        verify(materialUpdateService, times(2)).updateMaterial(dependencyMaterial);
    }

    @Test
    public void shouldDoNothingOnStageChangeIfStageDoesNotRepresentADependencyMaterial() {
        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);
        notifier.initialize();

        Stage pipeline2Stage2 = StageMother.passedStageInstance("Stage2", "plan", "Pipeline2");
        notifier.stageStatusChanged(pipeline2Stage2);

        verify(materialUpdateService, never()).updateMaterial(any(DependencyMaterial.class));
    }

    @Test
    public void shouldNotUpdateMaterialUnlessStageResultIsPassedOnStageChange() {
        Stage stage = StageMother.scheduledStage("Pipeline1", 1, "Stage1", 1, "buildName");
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial(stage.getIdentifier().getPipelineName(), stage.getName());
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(dependencyMaterial.config()));

        when(goConfigService.getSchedulableDependencyMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterial(dependencyMaterial.config())).thenReturn(dependencyMaterial);

        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);
        notifier.initialize();

        notifier.stageStatusChanged(stage);

        //updated only during initialization
        verify(materialUpdateService, times(1)).updateMaterial(dependencyMaterial);
    }

    @Test
    public void shouldRetryUpdatingMaterialsPreviouslyInProgress_OnMaterialUpdate() {
        Stage stage = StageMother.passedStageInstance("Stage1", "plan", "Pipeline1");
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial(stage.getIdentifier().getPipelineName(), stage.getName());
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(dependencyMaterial.config()));

        when(goConfigService.getSchedulableDependencyMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterial(dependencyMaterial.config())).thenReturn(dependencyMaterial);
        when(materialUpdateService.updateMaterial(dependencyMaterial)).thenReturn(true, false);

        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);
        notifier.initialize();

        notifier.stageStatusChanged(stage);

        notifier.onMaterialUpdate(dependencyMaterial);

        verify(materialUpdateService, atMost(3)).updateMaterial(dependencyMaterial);
    }

    @Test
    public void shouldRetryUpdatingMaterialsIfPreviouslyUpdatesFailed_OnMaterialUpdate() {
        Stage stage = StageMother.passedStageInstance("Stage1", "plan", "Pipeline1");
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial(stage.getIdentifier().getPipelineName(), stage.getName());
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(dependencyMaterial.config()));

        when(goConfigService.getSchedulableDependencyMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterial(dependencyMaterial.config())).thenReturn(dependencyMaterial);
        when(materialUpdateService.updateMaterial(dependencyMaterial)).thenThrow(Exception.class).thenReturn(true);

        notifier = new DependencyMaterialUpdateNotifier(goConfigService, materialConfigConverter, materialUpdateService);
        notifier.initialize();

        notifier.onMaterialUpdate(dependencyMaterial);

        verify(materialUpdateService, atMost(2)).updateMaterial(dependencyMaterial);
    }
}
