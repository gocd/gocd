/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthStates;
import com.thoughtworks.go.util.SystemEnvironment;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SCMMaterialSourceTest {
    private SCMMaterialSource source;
    private GoConfigService goConfigService;
    private ServerHealthService serverHealthService;
    private SystemEnvironment systemEnvironment;
    private MaterialConfigConverter materialConfigConverter;
    private MaterialUpdateService materialUpdateService;
    private Material svnMaterial = MaterialsMother.svnMaterial();
    private Material gitMaterial = MaterialsMother.gitMaterial("http://my.repo");

    @Before
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        systemEnvironment = new SystemEnvironment();
        serverHealthService = mock(ServerHealthService.class);
        materialConfigConverter = mock(MaterialConfigConverter.class);
        materialUpdateService = mock(MaterialUpdateService.class);

        source = new SCMMaterialSource(goConfigService, systemEnvironment, materialConfigConverter, materialUpdateService);
    }

    @After
    public void tearDown() throws Exception {
        systemEnvironment.reset(SystemEnvironment.MATERIAL_UPDATE_INACTIVE_TIMEOUT);
    }

    @Test
    public void shouldListAllSchedulableSCMMaterials_schedulableMaterials() {
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Collections.singleton(svnMaterial.config()));

        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(new HashSet<>(Collections.singleton(svnMaterial)));

        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size(), is(1));
        assertTrue(materials.contains(svnMaterial));
    }

    @Test
    public void shouldListMaterialsWhichHaveElapsedUpdateInterval_schedulableMaterials() {
        long minuteBack = DateTimeUtils.currentTimeMillis() - 60000;
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(svnMaterial.config(), gitMaterial.config()));

        systemEnvironment.setProperty(SystemEnvironment.MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, "60000");
        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(new HashSet<>(Arrays.asList(svnMaterial, gitMaterial)));

        freezeTime(minuteBack);
        source.onMaterialUpdate(gitMaterial);

        resetTime();
        source.onMaterialUpdate(svnMaterial);

        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size(), is(1));
        assertTrue(materials.contains(gitMaterial));
    }

    @Test
    public void shouldListenToConfigChange() {
        EntityConfigChangedListener entityConfigChangedListener = mock(EntityConfigChangedListener.class);
        source = spy(source);

        stub(source.pipelineConfigChangedListener()).toReturn(entityConfigChangedListener);

        source.initialize();

        verify(goConfigService).register(source);
        verify(goConfigService).register(entityConfigChangedListener);
    }

    @Test
    public void shouldListenToMaterialUpdateCompletedEvent() {
        source.initialize();

        verify(materialUpdateService).registerMaterialSources(source);
        verify(materialUpdateService).registerMaterialUpdateCompleteListener(source);
    }

    @Test
    public void shouldReloadSchedulableMaterialsOnConfigChange() {
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(svnMaterial.config()));

        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(new HashSet<>(Arrays.asList(svnMaterial)));

        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size(), is(1));
        assertTrue(materials.contains(svnMaterial));


        schedulableMaterialConfigs = new HashSet<>(Arrays.asList(svnMaterial.config(), gitMaterial.config()));
        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(new HashSet<>(Arrays.asList(svnMaterial, gitMaterial)));
        when(serverHealthService.getAllLogs()).thenReturn(new ServerHealthStates());

        source.onConfigChange(mock(CruiseConfig.class));
        materials = source.materialsForUpdate();

        assertThat(materials.size(), is(2));
        assertTrue(materials.contains(svnMaterial));
        assertTrue(materials.contains(gitMaterial));
    }

    @Test
    public void shouldReloadSchedulableMaterialsOnConfigRepoChange() {
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(svnMaterial.config(), gitMaterial.config()));
        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(new HashSet<>(Arrays.asList(svnMaterial, gitMaterial)));
        when(serverHealthService.getAllLogs()).thenReturn(new ServerHealthStates());

        source.onEntityConfigChange(mock(ConfigRepoConfig.class));
        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size(), is(2));
        assertTrue(materials.contains(svnMaterial));
        assertTrue(materials.contains(gitMaterial));
    }

    @Test
    public void shouldReloadSchedulableMaterialsOnPipelineConfigChange() {
        Set<MaterialConfig> schedulableMaterialConfigs = new HashSet<>(Arrays.asList(svnMaterial.config()));

        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(new HashSet<>(Arrays.asList(svnMaterial)));

        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size(), is(1));
        assertTrue(materials.contains(svnMaterial));


        schedulableMaterialConfigs = new HashSet<>(Arrays.asList(svnMaterial.config(), gitMaterial.config()));
        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(goConfigService.getCurrentConfig()).thenReturn(mock(CruiseConfig.class));
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(new HashSet<>(Arrays.asList(svnMaterial, gitMaterial)));
        when(serverHealthService.getAllLogs()).thenReturn(new ServerHealthStates());

        source.pipelineConfigChangedListener().onEntityConfigChange(mock(PipelineConfig.class));
        materials = source.materialsForUpdate();

        assertThat(materials.size(), is(2));
        assertTrue(materials.contains(svnMaterial));
        assertTrue(materials.contains(gitMaterial));
    }

    private void freezeTime(Long millis) {
        DateTimeUtils.setCurrentMillisFixed(millis);
    }

    private void resetTime() {
        DateTimeUtils.setCurrentMillisSystem();
    }
}
