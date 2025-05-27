/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthStates;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class SCMMaterialSourceTest {
    private final Material svnMaterial = MaterialsMother.svnMaterial();
    private final Material gitMaterial = MaterialsMother.gitMaterial("http://my.repo");
    private SCMMaterialSource source;
    private GoConfigService goConfigService;
    private ServerHealthService serverHealthService;
    private SystemEnvironment systemEnvironment;
    private MaterialConfigConverter materialConfigConverter;
    private MaterialUpdateService materialUpdateService;
    private TimeProvider timeProvider;

    @BeforeEach
    public void setUp() {
        goConfigService = mock(GoConfigService.class);
        systemEnvironment = new SystemEnvironment();
        serverHealthService = mock(ServerHealthService.class);
        materialConfigConverter = mock(MaterialConfigConverter.class);
        materialUpdateService = mock(MaterialUpdateService.class);
        timeProvider = mock(TimeProvider.class);

        source = new SCMMaterialSource(goConfigService, systemEnvironment, materialConfigConverter, materialUpdateService, timeProvider);
    }

    @AfterEach
    public void tearDown() {
        systemEnvironment.reset(SystemEnvironment.MATERIAL_UPDATE_INACTIVE_TIMEOUT_IN_MINUTES);
    }

    @Test
    public void shouldListAllSchedulableSCMMaterials_schedulableMaterials() {
        Set<MaterialConfig> schedulableMaterialConfigs = Set.of(svnMaterial.config());

        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(Set.of(svnMaterial));

        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size()).isEqualTo(1);
        assertTrue(materials.contains(svnMaterial));
    }

    @Test
    public void shouldListMaterialsWhichHaveElapsedUpdateInterval_schedulableMaterials() {
        long minuteBack = Instant.now().minusSeconds(60).toEpochMilli();
        Set<MaterialConfig> schedulableMaterialConfigs = Set.of(svnMaterial.config(), gitMaterial.config());

        systemEnvironment.setProperty(SystemEnvironment.MATERIAL_UPDATE_IDLE_INTERVAL_PROPERTY, "60000");
        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(Set.of(svnMaterial, gitMaterial));

        when(timeProvider.currentTimeMillis()).thenReturn(minuteBack);
        source.onMaterialUpdate(gitMaterial);

        when(timeProvider.currentTimeMillis()).thenReturn(Instant.now().toEpochMilli());
        source.onMaterialUpdate(svnMaterial);

        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size()).isEqualTo(1);
        assertTrue(materials.contains(gitMaterial));
    }

    @Test
    public void shouldListenToConfigChange() {
        source = spy(source);

        source.initialize();

        verify(goConfigService).register(source);
    }

    @Test
    public void shouldRefreshMaterialCacheOnPipelineConfigChange() {
        GitMaterialConfig gitMaterial = new GitMaterialConfig();
        gitMaterial.setUrl("http://github.com/gocd/gocd");
        @SuppressWarnings("unchecked") ArgumentCaptor<EntityConfigChangedListener<PipelineConfig>> captor = ArgumentCaptor.forClass(EntityConfigChangedListener.class);

        doNothing().when(goConfigService).register(captor.capture());
        when(goConfigService.getSchedulableSCMMaterials())
                .thenReturn(emptySet())
                .thenReturn(Set.of(gitMaterial));

        source = new SCMMaterialSource(goConfigService, systemEnvironment, new MaterialConfigConverter(), materialUpdateService, timeProvider);
        source.initialize();

        EntityConfigChangedListener<PipelineConfig> entityConfigChangedListener = captor.getAllValues().get(1);

        assertTrue(entityConfigChangedListener.shouldCareAbout(new PipelineConfig()));
        assertThat(source.materialsForUpdate().size()).isEqualTo(0);

        entityConfigChangedListener.onEntityConfigChange(new PipelineConfig());

        Set<Material> materials = source.materialsForUpdate();
        assertThat(materials.size()).isEqualTo(1);
        assertThat(materials.iterator().next().getFingerprint()).isEqualTo(gitMaterial.getFingerprint());
    }

    @Test
    public void shouldRefreshMaterialCacheOnPackageDefinitionChange() {
        GitMaterialConfig gitMaterial = new GitMaterialConfig();
        gitMaterial.setUrl("http://github.com/gocd/gocd");
        @SuppressWarnings("unchecked") ArgumentCaptor<EntityConfigChangedListener<PackageDefinition>> captor = ArgumentCaptor.forClass(EntityConfigChangedListener.class);

        doNothing().when(goConfigService).register(captor.capture());
        when(goConfigService.getSchedulableSCMMaterials())
                .thenReturn(emptySet())
                .thenReturn(Set.of(gitMaterial));


        source = new SCMMaterialSource(goConfigService, systemEnvironment, new MaterialConfigConverter(), materialUpdateService, timeProvider);
        source.initialize();

        EntityConfigChangedListener<PackageDefinition> entityConfigChangedListener = captor.getAllValues().get(1);

        assertTrue(entityConfigChangedListener.shouldCareAbout(new PackageDefinition()));
        assertThat(source.materialsForUpdate().size()).isEqualTo(0);

        entityConfigChangedListener.onEntityConfigChange(new PackageDefinition());

        Set<Material> materials = source.materialsForUpdate();
        assertThat(materials.size()).isEqualTo(1);
        assertThat(materials.iterator().next().getFingerprint()).isEqualTo(gitMaterial.getFingerprint());
    }

    @Test
    public void shouldRefreshMaterialCacheOnPackageRepositoryChange() {
        GitMaterialConfig gitMaterial = new GitMaterialConfig();
        gitMaterial.setUrl("http://github.com/gocd/gocd");
        @SuppressWarnings("unchecked") ArgumentCaptor<EntityConfigChangedListener<PackageRepository>> captor = ArgumentCaptor.forClass(EntityConfigChangedListener.class);

        doNothing().when(goConfigService).register(captor.capture());
        when(goConfigService.getSchedulableSCMMaterials())
                .thenReturn(emptySet())
                .thenReturn(Set.of(gitMaterial));

        source = new SCMMaterialSource(goConfigService, systemEnvironment, new MaterialConfigConverter(), materialUpdateService, timeProvider);
        source.initialize();

        EntityConfigChangedListener<PackageRepository> entityConfigChangedListener = captor.getAllValues().get(1);

        assertTrue(entityConfigChangedListener.shouldCareAbout(new PackageRepository()));
        assertThat(source.materialsForUpdate().size()).isEqualTo(0);

        entityConfigChangedListener.onEntityConfigChange(new PackageRepository());

        Set<Material> materials = source.materialsForUpdate();
        assertThat(materials.size()).isEqualTo(1);
        assertThat(materials.iterator().next().getFingerprint()).isEqualTo(gitMaterial.getFingerprint());
    }

    @Test
    public void shouldRefreshMaterialCacheOnSCMChange() {
        GitMaterialConfig gitMaterial = new GitMaterialConfig();
        gitMaterial.setUrl("http://github.com/gocd/gocd");
        @SuppressWarnings("unchecked") ArgumentCaptor<EntityConfigChangedListener<SCM>> captor = ArgumentCaptor.forClass(EntityConfigChangedListener.class);

        doNothing().when(goConfigService).register(captor.capture());
        when(goConfigService.getSchedulableSCMMaterials())
                .thenReturn(emptySet())
                .thenReturn(Set.of(gitMaterial));

        source = new SCMMaterialSource(goConfigService, systemEnvironment, new MaterialConfigConverter(), materialUpdateService, timeProvider);
        source.initialize();

        EntityConfigChangedListener<SCM> entityConfigChangedListener = captor.getAllValues().get(1);

        assertTrue(entityConfigChangedListener.shouldCareAbout(new SCM()));
        assertThat(source.materialsForUpdate().size()).isEqualTo(0);

        entityConfigChangedListener.onEntityConfigChange(new SCM());

        Set<Material> materials = source.materialsForUpdate();
        assertThat(materials.size()).isEqualTo(1);
        assertThat(materials.iterator().next().getFingerprint()).isEqualTo(gitMaterial.getFingerprint());
    }

    @Test
    public void shouldListenToMaterialUpdateCompletedEvent() {
        source.initialize();

        verify(materialUpdateService).registerMaterialSources(source);
        verify(materialUpdateService).registerMaterialUpdateCompleteListener(source);
    }

    @Test
    public void shouldReloadSchedulableMaterialsOnConfigChange() {
        Set<MaterialConfig> schedulableMaterialConfigs = Set.of(svnMaterial.config());

        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(Set.of(svnMaterial));

        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size()).isEqualTo(1);
        assertTrue(materials.contains(svnMaterial));


        schedulableMaterialConfigs = Set.of(svnMaterial.config(), gitMaterial.config());
        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(Set.of(svnMaterial, gitMaterial));
        when(serverHealthService.logsSorted()).thenReturn(new ServerHealthStates());

        source.onConfigChange(mock(CruiseConfig.class));
        materials = source.materialsForUpdate();

        assertThat(materials.size()).isEqualTo(2);
        assertTrue(materials.contains(svnMaterial));
        assertTrue(materials.contains(gitMaterial));
    }

    @Test
    public void shouldReloadSchedulableMaterialsOnConfigRepoChange() {
        Set<MaterialConfig> schedulableMaterialConfigs = Set.of(svnMaterial.config(), gitMaterial.config());
        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(Set.of(svnMaterial, gitMaterial));
        when(serverHealthService.logsSorted()).thenReturn(new ServerHealthStates());

        source.onEntityConfigChange(mock(ConfigRepoConfig.class));
        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size()).isEqualTo(2);
        assertTrue(materials.contains(svnMaterial));
        assertTrue(materials.contains(gitMaterial));
    }

    @Test
    public void shouldReloadSchedulableMaterialsOnPipelineConfigChange() {
        Set<MaterialConfig> schedulableMaterialConfigs = Set.of(svnMaterial.config());

        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(Set.of(svnMaterial));

        Set<Material> materials = source.materialsForUpdate();

        assertThat(materials.size()).isEqualTo(1);
        assertTrue(materials.contains(svnMaterial));


        schedulableMaterialConfigs = Set.of(svnMaterial.config(), gitMaterial.config());
        when(goConfigService.getSchedulableSCMMaterials()).thenReturn(schedulableMaterialConfigs);
        when(goConfigService.getCurrentConfig()).thenReturn(mock(CruiseConfig.class));
        when(materialConfigConverter.toMaterials(schedulableMaterialConfigs)).thenReturn(Set.of(svnMaterial, gitMaterial));
        when(serverHealthService.logsSorted()).thenReturn(new ServerHealthStates());

        source.pipelineConfigChangedListener().onEntityConfigChange(mock(PipelineConfig.class));
        materials = source.materialsForUpdate();

        assertThat(materials.size()).isEqualTo(2);
        assertTrue(materials.contains(svnMaterial));
        assertTrue(materials.contains(gitMaterial));
    }
}
