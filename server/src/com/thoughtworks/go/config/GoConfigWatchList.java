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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a list of configuration repositories.
 */
@Component
public class GoConfigWatchList extends EntityConfigChangedListener<ConfigRepoConfig> implements ConfigChangedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoConfigWatchList.class);

    private List<ChangedRepoConfigWatchListListener> listeners = new ArrayList<>();
    private ConfigReposConfig reposConfig;
    private GoConfigService goConfigService;

    @Autowired
    public GoConfigWatchList(CachedGoConfig cachedGoConfig, GoConfigService goConfigService) {
        this.reposConfig = cachedGoConfig.currentConfig().getConfigRepos();
        this.goConfigService = goConfigService;
        cachedGoConfig.registerListener(this);
    }

    public ConfigReposConfig getCurrentConfigRepos() {
        return reposConfig;
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        ConfigReposConfig partSources = newCruiseConfig.getConfigRepos();

        this.reposConfig = partSources;
        notifyListeners(partSources);
    }

    @Override
    public void onEntityConfigChange(ConfigRepoConfig newConfigRepo) {
        this.reposConfig.remove(this.reposConfig.getConfigRepo(newConfigRepo.getId()));
        if(goConfigService.currentCruiseConfig().getConfigRepos().getConfigRepo(newConfigRepo.getId()) != null) {
            this.reposConfig.add(newConfigRepo);
        }

        notifyListeners(this.reposConfig);
    }

    private synchronized void notifyListeners(ConfigReposConfig configRepoConfigs) {
        for (ChangedRepoConfigWatchListListener listener : listeners)
            try {
                listener.onChangedRepoConfigWatchList(configRepoConfigs);
            } catch (Exception e) {
                LOGGER.error("failed to fire config repos list changed event for listener: {}", listener, e);
            }
    }

    public boolean hasConfigRepoWithFingerprint(String fingerprint) {
        return this.reposConfig.hasMaterialWithFingerprint(fingerprint);
    }

    public ConfigRepoConfig getConfigRepoForMaterial(MaterialConfig material) {
        return this.reposConfig.getConfigRepo(material);
    }

    public void registerListener(ChangedRepoConfigWatchListListener listener) {
        listeners.add(listener);
        listener.onChangedRepoConfigWatchList(this.reposConfig);
    }

    public boolean hasListener(ChangedRepoConfigWatchListListener listener) {
        return this.listeners.contains(listener);
    }
}
