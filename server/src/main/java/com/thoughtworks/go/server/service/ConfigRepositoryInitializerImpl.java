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
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.plugin.infra.ConfigRepositoryInitializer;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginPostLoadHook;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.CONFIG_REPO_EXTENSION;

/**
 * @understands initializing config repositories. Loads the configurations from the last checked out modification on server startup.
 */
@Service
public class ConfigRepositoryInitializerImpl implements ConfigRepositoryInitializer {
    private PluginManager pluginManager;
    private final ConfigRepoService configRepoService;
    private final MaterialRepository materialRepository;
    private final GoConfigRepoConfigDataSource goConfigRepoConfigDataSource;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRepositoryInitializerImpl.class);

    @Autowired
    public ConfigRepositoryInitializerImpl(PluginManager pluginManager, ConfigRepoService configRepoService, MaterialRepository materialRepository, GoConfigRepoConfigDataSource goConfigRepoConfigDataSource) {
        this.pluginManager = pluginManager;
        this.configRepoService = configRepoService;
        this.materialRepository = materialRepository;
        this.goConfigRepoConfigDataSource = goConfigRepoConfigDataSource;
    }

    @Override
    public Result run(GoPluginDescriptor pluginDescriptor, Map<String, List<String>> extensionsInfoFromThePlugin) {
        String pluginId = pluginDescriptor.id();
        boolean isConfigRepoPlugin = pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, pluginId);

        if (isConfigRepoPlugin) {
            try {
                List<ConfigRepoConfig> configRepos = this.configRepoService.getConfigRepos().stream().filter(repo -> repo.getPluginId().equals(pluginId)).collect(Collectors.toList());
                configRepos.forEach(this::initializeConfigRepository);
            } catch (Exception e) {
                // Do nothing when error occurs while initializing the config repository.
                // The config repo initialization may fail due to config repo errors (config errors, or rules violation errors)
                //
                // Though, in error scenario, do not return PluginPostLoadHook.Result as a failure, because doing so, the plugin load will fail and the plugin will be marked as invalid.
            }
        }

        return new PluginPostLoadHook.Result(false, "success");
    }

    private void initializeConfigRepository(ConfigRepoConfig repo) {
        MaterialConfig materialConfig = repo.getRepo();
        Material material = new Materials(new MaterialConfigs(materialConfig)).first();
        MaterialInstance materialInstance = this.materialRepository.findMaterialInstance(materialConfig);

        if (materialInstance != null) {
            File folder = materialRepository.folderFor(material);
            MaterialRevisions latestModification = materialRepository.findLatestModification(material);
            Modification modification = latestModification.firstModifiedMaterialRevision().getLatestModification();

            LOGGER.info("[Config Repository Initializer] Initializing config repository '{}'. Loading the GoCD configuration from last fetched modification '{}'.", repo.getId(), modification.getRevision());
            goConfigRepoConfigDataSource.onCheckoutComplete(materialConfig, folder, modification);
        } else {
            LOGGER.info("[Config Repository Initializer] Skipped initializing config repository '{}'. Could not find material repository under flyweight folder.", repo.getId());
        }
    }
}
