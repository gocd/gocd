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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.GoConfigRepoConfigDataSource;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestingMaterialInstance;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginPostLoadHook;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.File;
import java.util.HashMap;

import static com.thoughtworks.go.helper.ModificationsMother.oneModifiedFile;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.CONFIG_REPO_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class ConfigRepositoryInitializerImplTest {
    @Mock
    private PluginManager pluginManager;

    @Mock
    private ConfigRepoService configRepoService;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private GoConfigRepoConfigDataSource goConfigRepoConfigDataSource;

    private ConfigRepositoryInitializerImpl configRepositoryInitializer;

    private GoPluginDescriptor goPluginDescriptor;
    private String PLUGIN_ID = "yaml.config.plugin";


    @BeforeEach
    void setUp() {
        initMocks(this);
        goPluginDescriptor = GoPluginDescriptor.builder().id(PLUGIN_ID).build();
        goPluginDescriptor.setBundleDescriptor(new GoPluginBundleDescriptor(goPluginDescriptor));
        configRepositoryInitializer = new ConfigRepositoryInitializerImpl(pluginManager, configRepoService, materialRepository, goConfigRepoConfigDataSource);
    }

    @Test
    void shouldDoNothingWhenPluginIsNotOfConfigRepoType() {
        when(pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, PLUGIN_ID)).thenReturn(false);

        PluginPostLoadHook.Result result = configRepositoryInitializer.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isFalse();
        assertThat(result.getMessage()).isEqualTo("success");

        verifyNoInteractions(configRepoService);
        verifyNoInteractions(materialRepository);
    }

    @Test
    void shouldInitializeConfigRepositoryWhenMaterialRepositoryExists() {
        GitMaterialConfig gitMaterialConfig = MaterialConfigsMother.git("git-repo");
        Material gitMaterial = new Materials(new MaterialConfigs(gitMaterialConfig)).first();
        TestingMaterialInstance gitMaterialInstance = new TestingMaterialInstance("git-repo", "flyweight");
        File folder = new File("repo-folder");
        MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(gitMaterial, oneModifiedFile("revision1")));
        Modification modification = materialRevisions.firstModifiedMaterialRevision().getLatestModification();

        ConfigReposConfig repoConfigs = new ConfigReposConfig();
        ConfigRepoConfig repoConfig = new ConfigRepoConfig();
        repoConfig.setId("repo1");
        repoConfig.setPluginId(PLUGIN_ID);
        repoConfig.setRepo(gitMaterialConfig);
        repoConfigs.add(repoConfig);

        when(pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(configRepoService.getConfigRepos()).thenReturn(repoConfigs);
        when(materialRepository.findMaterialInstance(gitMaterialConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.folderFor(gitMaterial)).thenReturn(folder);
        when(materialRepository.findLatestModification(gitMaterial)).thenReturn(materialRevisions);

        PluginPostLoadHook.Result result = configRepositoryInitializer.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isFalse();
        assertThat(result.getMessage()).isEqualTo("success");

        verify(goConfigRepoConfigDataSource, times(1)).onCheckoutComplete(gitMaterialConfig, folder, modification);
    }

    @Test
    void shouldNotInitializeConfigRepositoryWhenMaterialRepositoryDoesNotExistsUnderFlyweightFolder() {
        GitMaterialConfig gitMaterialConfig = MaterialConfigsMother.git("git-repo");

        ConfigReposConfig repoConfigs = new ConfigReposConfig();
        ConfigRepoConfig repoConfig = new ConfigRepoConfig();
        repoConfig.setId("repo1");
        repoConfig.setPluginId(PLUGIN_ID);
        repoConfig.setRepo(gitMaterialConfig);
        repoConfigs.add(repoConfig);

        when(pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(configRepoService.getConfigRepos()).thenReturn(repoConfigs);
        when(materialRepository.findMaterialInstance(gitMaterialConfig)).thenReturn(null);

        PluginPostLoadHook.Result result = configRepositoryInitializer.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isFalse();
        assertThat(result.getMessage()).isEqualTo("success");

        verifyNoMoreInteractions(goConfigRepoConfigDataSource);
    }

    @Test
    void shouldReturnPluginPostLoadHookAsSuccessEvenWhenItFailsToInitializeConfigRepository() {
        GitMaterialConfig gitMaterialConfig = MaterialConfigsMother.git("git-repo");
        Material gitMaterial = new Materials(new MaterialConfigs(gitMaterialConfig)).first();
        TestingMaterialInstance gitMaterialInstance = new TestingMaterialInstance("git-repo", "flyweight");
        File folder = new File("repo-folder");
        MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(gitMaterial, oneModifiedFile("revision1")));
        Modification modification = materialRevisions.firstModifiedMaterialRevision().getLatestModification();

        ConfigReposConfig repoConfigs = new ConfigReposConfig();
        ConfigRepoConfig repoConfig = new ConfigRepoConfig();
        repoConfig.setId("repo1");
        repoConfig.setPluginId(PLUGIN_ID);
        repoConfig.setRepo(gitMaterialConfig);
        repoConfigs.add(repoConfig);

        when(pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(configRepoService.getConfigRepos()).thenReturn(repoConfigs);
        when(materialRepository.findMaterialInstance(gitMaterialConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.folderFor(gitMaterial)).thenReturn(folder);
        when(materialRepository.findLatestModification(gitMaterial)).thenReturn(materialRevisions);

        doThrow(new RuntimeException("failed to initialize config repository due to config errors")).when(goConfigRepoConfigDataSource).onCheckoutComplete(gitMaterialConfig, folder, modification);

        PluginPostLoadHook.Result result = configRepositoryInitializer.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isFalse();
        assertThat(result.getMessage()).isEqualTo("success");

        verify(goConfigRepoConfigDataSource, times(1)).onCheckoutComplete(gitMaterialConfig, folder, modification);
    }
}
