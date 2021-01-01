/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigRepoConfigDataSource;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.LinkedList;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.CONFIG_REPO_EXTENSION;

/**
 * @understands initializing config repositories. Loads the configurations from the last checked out modification on server startup.
 */
@Service
public class ConfigRepositoryInitializer implements ConfigChangedListener, PluginChangeListener {
    private PluginManager pluginManager;
    private final ConfigRepoService configRepoService;
    private final MaterialRepository materialRepository;
    private final GoConfigRepoConfigDataSource goConfigRepoConfigDataSource;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRepositoryInitializer.class);

    private boolean isConfigLoaded = false;

    // list of config repo plugins which are loaded, but not yet processed. Once processed, these plugins will be removed from the list.
    private final LinkedList<String> pluginsQueue = new LinkedList<>();

    @Autowired
    public ConfigRepositoryInitializer(PluginManager pluginManager, ConfigRepoService configRepoService, MaterialRepository materialRepository, GoConfigRepoConfigDataSource goConfigRepoConfigDataSource, GoConfigService goConfigService, SystemEnvironment systemEnvironment) {
        this.pluginManager = pluginManager;
        this.configRepoService = configRepoService;
        this.materialRepository = materialRepository;
        this.goConfigRepoConfigDataSource = goConfigRepoConfigDataSource;

        if (systemEnvironment.shouldInitializeConfigRepositoriesOnStartup()) {
            this.pluginManager.addPluginChangeListener(this);
            goConfigService.register(this);
        }
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        // mark config loaded only once!
        if (!this.isConfigLoaded) {
            this.isConfigLoaded = true;
            this.initializeConfigRepositories();
        }
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        String pluginId = pluginDescriptor.id();
        boolean isConfigRepoPlugin = pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, pluginId);

        if (isConfigRepoPlugin) {
            pluginsQueue.add(pluginId);
            this.initializeConfigRepositories();
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        //do nothing
    }

    private void initializeConfigRepositories() {
        // do nothing if the cruise config is not loaded yet..
        if (!this.isConfigLoaded) {
            return;
        }

        synchronized (this.pluginsQueue) {
            while (!pluginsQueue.isEmpty()) {
                String pluginId = pluginsQueue.poll();
                LOGGER.info("[Config Repository Initializer] Start initializing the config repositories for plugin '{}' ", pluginId);
                this.configRepoService.getConfigRepos().stream()
                        .filter(configRepoConfig -> configRepoConfig.getPluginId().equalsIgnoreCase(pluginId))
                        .forEach(this::initializeConfigRepository);
                LOGGER.info("[Config Repository Initializer] Done initializing the config repositories for plugin '{}' ", pluginId);
            }
        }
    }

    private void initializeConfigRepository(ConfigRepoConfig repo) {
        MaterialConfig materialConfig = repo.getRepo();
        Material material = new Materials(new MaterialConfigs(materialConfig)).first();
        MaterialInstance materialInstance = this.materialRepository.findMaterialInstance(materialConfig);

        if (materialInstance != null) {
            File folder = materialRepository.folderFor(material);
            MaterialRevisions latestModification = materialRepository.findLatestModification(material);
            Modification modification = latestModification.firstModifiedMaterialRevision().getLatestModification();

            try {
                LOGGER.debug("[Config Repository Initializer] Initializing config repository '{}'. Loading the GoCD configuration from last fetched modification '{}'.", repo.getId(), modification.getRevision());
                goConfigRepoConfigDataSource.onCheckoutComplete(materialConfig, folder, modification);
            } catch (Exception e) {
                LOGGER.error(String.format("[Config Repository Initializer] an error occurred while initializing '%s' config repository.", repo.getId()), e);
                // Do nothing when error occurs while initializing the config repository.
                // The config repo initialization may fail due to config repo errors (config errors, or rules violation errors)
            }
        } else {
            LOGGER.debug("[Config Repository Initializer] Skipped initializing config repository '{}'. Could not find material repository under flyweight folder.", repo.getId());
        }
    }
}
