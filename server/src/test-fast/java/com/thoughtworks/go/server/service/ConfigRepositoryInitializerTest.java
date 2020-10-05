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

import com.thoughtworks.go.config.BasicCruiseConfig;
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
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.File;

import static com.thoughtworks.go.helper.ModificationsMother.oneModifiedFile;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.CONFIG_REPO_EXTENSION;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class ConfigRepositoryInitializerTest {
    @Mock
    private PluginManager pluginManager;

    @Mock
    private ConfigRepoService configRepoService;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private GoConfigRepoConfigDataSource goConfigRepoConfigDataSource;

    @Mock
    private GoConfigService goConfigService;

    @Mock
    private SystemEnvironment systemEnvironment;

    private ConfigRepositoryInitializer configRepositoryInitializer;

    private GoPluginDescriptor yamlPluginDescriptor;
    private GoPluginDescriptor jsonPluginDescriptor;
    private GoPluginDescriptor groovyPluginDescriptor;
    private String YAML_PLUGIN_ID = "yaml.config.plugin";
    private String JSON_PLUGIN_ID = "json.config.plugin";
    private String GROOVY_PLUGIN_ID = "groovy.config.plugin";
    private ConfigReposConfig repoConfigs;

    @BeforeEach
    void setUp() {
        initMocks(this);
        yamlPluginDescriptor = GoPluginDescriptor.builder().id(YAML_PLUGIN_ID).build();
        yamlPluginDescriptor.setBundleDescriptor(new GoPluginBundleDescriptor(yamlPluginDescriptor));

        jsonPluginDescriptor = GoPluginDescriptor.builder().id(JSON_PLUGIN_ID).build();
        jsonPluginDescriptor.setBundleDescriptor(new GoPluginBundleDescriptor(jsonPluginDescriptor));

        groovyPluginDescriptor = GoPluginDescriptor.builder().id(GROOVY_PLUGIN_ID).build();
        groovyPluginDescriptor.setBundleDescriptor(new GoPluginBundleDescriptor(groovyPluginDescriptor));

        configRepositoryInitializer = new ConfigRepositoryInitializer(pluginManager, configRepoService, materialRepository, goConfigRepoConfigDataSource, goConfigService, systemEnvironment);

        repoConfigs = new ConfigReposConfig();
        ConfigRepoConfig repoConfig1 = new ConfigRepoConfig();
        repoConfig1.setId("repo1");
        repoConfig1.setPluginId(YAML_PLUGIN_ID);
        repoConfig1.setRepo(MaterialConfigsMother.git("git-repo"));

        repoConfigs.add(repoConfig1);

        when(configRepoService.getConfigRepos()).thenReturn(repoConfigs);

        when(pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, YAML_PLUGIN_ID)).thenReturn(true);
        when(pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, JSON_PLUGIN_ID)).thenReturn(true);
    }

    @Test
    void shouldDoNothingWhenPluginIsLoadedButTheConfigIsNotLoadedYet() {
        configRepositoryInitializer.pluginLoaded(yamlPluginDescriptor);

        verifyNoInteractions(configRepoService);
        verifyNoInteractions(materialRepository);
        verifyNoInteractions(goConfigRepoConfigDataSource);
    }

    @Test
    void shouldDoNothingWhenConfigIsLoadedButThePluginIsNotLoadedYet() {
        configRepositoryInitializer.onConfigChange(new BasicCruiseConfig());

        verifyNoInteractions(materialRepository);
        verifyNoInteractions(goConfigRepoConfigDataSource);
    }

    @Test
    void shouldInitializeConfigRepositoryAsAndWhenPluginsAreLoaded() {
        // add another config repo using another plugin id
        ConfigRepoConfig repoConfig2 = new ConfigRepoConfig();
        repoConfig2.setId("repo2");
        repoConfig2.setPluginId(JSON_PLUGIN_ID);
        repoConfig2.setRepo(MaterialConfigsMother.git("git-repo"));
        repoConfigs.add(repoConfig2);

        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig) repoConfigs.get(0).getRepo();
        Material gitMaterial = new Materials(new MaterialConfigs(gitMaterialConfig)).first();
        TestingMaterialInstance gitMaterialInstance = new TestingMaterialInstance("git-repo", "flyweight");
        File folder = new File("repo-folder");
        MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(gitMaterial, oneModifiedFile("revision1")));
        Modification modification = materialRevisions.firstModifiedMaterialRevision().getLatestModification();

        when(materialRepository.findMaterialInstance(gitMaterialConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.folderFor(gitMaterial)).thenReturn(folder);
        when(materialRepository.findLatestModification(gitMaterial)).thenReturn(materialRevisions);

        // initialize config
        configRepositoryInitializer.onConfigChange(new BasicCruiseConfig());

        // load yaml plugin
        configRepositoryInitializer.pluginLoaded(yamlPluginDescriptor);

        // verify the config repo is initialized once all the in use plugins are loaded
        verify(goConfigRepoConfigDataSource, times(1)).onCheckoutComplete(gitMaterialConfig, folder, modification);

        // load json plugin
        configRepositoryInitializer.pluginLoaded(jsonPluginDescriptor);

        // verify the config repo is initialized once all the in use plugins are loaded
        verify(goConfigRepoConfigDataSource, times(2)).onCheckoutComplete(gitMaterialConfig, folder, modification);

        // load groovy plugin
        configRepositoryInitializer.pluginLoaded(groovyPluginDescriptor);

        // verify nothing happens on more plugin load
        verifyNoMoreInteractions(goConfigRepoConfigDataSource);
    }

    @Test
    void shouldInitializeConfigRepositoriesWhenCruiseConfigAndAllInUsePluginsAreLoaded() {
        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig) repoConfigs.get(0).getRepo();
        Material gitMaterial = new Materials(new MaterialConfigs(gitMaterialConfig)).first();
        TestingMaterialInstance gitMaterialInstance = new TestingMaterialInstance("git-repo", "flyweight");
        File folder = new File("repo-folder");
        MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(gitMaterial, oneModifiedFile("revision1")));
        Modification modification = materialRevisions.firstModifiedMaterialRevision().getLatestModification();

        when(materialRepository.findMaterialInstance(gitMaterialConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.folderFor(gitMaterial)).thenReturn(folder);
        when(materialRepository.findLatestModification(gitMaterial)).thenReturn(materialRevisions);

        configRepositoryInitializer.onConfigChange(new BasicCruiseConfig());
        configRepositoryInitializer.pluginLoaded(yamlPluginDescriptor);

        verify(goConfigRepoConfigDataSource, times(1)).onCheckoutComplete(gitMaterialConfig, folder, modification);
    }

    @Test
    void shouldNotInitializeConfigRepositoryWhenTheRepositoryIsNotAvailableUnderFlyweightFolder() {
        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig) repoConfigs.get(0).getRepo();
        when(materialRepository.findMaterialInstance(gitMaterialConfig)).thenReturn(null);

        configRepositoryInitializer.onConfigChange(new BasicCruiseConfig());
        configRepositoryInitializer.pluginLoaded(yamlPluginDescriptor);

        verifyNoInteractions(goConfigRepoConfigDataSource);
    }

    @Test
    void shouldNotReInitializeConfigRepositoriesWhenCruiseConfigListenerIsInvokedAgain() {
        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig) repoConfigs.get(0).getRepo();
        Material gitMaterial = new Materials(new MaterialConfigs(gitMaterialConfig)).first();
        TestingMaterialInstance gitMaterialInstance = new TestingMaterialInstance("git-repo", "flyweight");
        File folder = new File("repo-folder");
        MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(gitMaterial, oneModifiedFile("revision1")));
        Modification modification = materialRevisions.firstModifiedMaterialRevision().getLatestModification();

        when(materialRepository.findMaterialInstance(gitMaterialConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.folderFor(gitMaterial)).thenReturn(folder);
        when(materialRepository.findLatestModification(gitMaterial)).thenReturn(materialRevisions);

        configRepositoryInitializer.onConfigChange(new BasicCruiseConfig());
        configRepositoryInitializer.pluginLoaded(yamlPluginDescriptor);

        verify(goConfigRepoConfigDataSource, times(1)).onCheckoutComplete(gitMaterialConfig, folder, modification);

        // config changes..
        configRepositoryInitializer.onConfigChange(new BasicCruiseConfig());

        verifyNoMoreInteractions(goConfigRepoConfigDataSource);
    }

    @Test
    void shouldRegisterConfigChangeAndPluginLoadListenersWhenInitializeConfigRepositoriesOnStartupSystemPropertyIsSet() {
        when(systemEnvironment.shouldInitializeConfigRepositoriesOnStartup()).thenReturn(true);

        configRepositoryInitializer = new ConfigRepositoryInitializer(pluginManager, configRepoService, materialRepository, goConfigRepoConfigDataSource, goConfigService, systemEnvironment);

        verify(pluginManager, times(1)).addPluginChangeListener(configRepositoryInitializer);
        verify(goConfigService, times(1)).register(configRepositoryInitializer);
    }

    @Test
    void shouldNotRegisterConfigChangeAndPluginLoadListenersWhenInitializeConfigRepositoriesOnStartupSystemPropertyIsUnSet() {
        when(systemEnvironment.shouldInitializeConfigRepositoriesOnStartup()).thenReturn(false);

        configRepositoryInitializer = new ConfigRepositoryInitializer(pluginManager, configRepoService, materialRepository, goConfigRepoConfigDataSource, goConfigService, systemEnvironment);

        verify(pluginManager, never()).addPluginChangeListener(configRepositoryInitializer);
        verify(goConfigService, never()).register(configRepositoryInitializer);
    }
}
