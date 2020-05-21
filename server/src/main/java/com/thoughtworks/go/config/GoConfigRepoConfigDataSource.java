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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.synchronizedSet;

/**
 * Parses partial configurations and exposes latest configurations as soon as possible.
 */
@Component
public class GoConfigRepoConfigDataSource implements ChangedRepoConfigWatchListListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoConfigRepoConfigDataSource.class);
    private final ServerHealthService serverHealthService;
    private final GoConfigPluginService configPluginService;
    private final GoConfigWatchList configWatchList;
    private final ConfigReposMaterialParseResultManager configReposMaterialParseResultManager;
    private final GoConfigService goConfigService;

    private final List<PartialConfigUpdateCompletedListener> listeners = new ArrayList<>();
    private final Set<ConfigRepoConfig> modifiedConfigRepoConfigsAwaitingParse = synchronizedSet(new HashSet<>());

    @Autowired
    public GoConfigRepoConfigDataSource(GoConfigWatchList configWatchList, GoConfigPluginService configPluginService,
                                        ServerHealthService healthService, ConfigRepoService configRepoService,
                                        GoConfigService goConfigService) {
        this.configReposMaterialParseResultManager = new ConfigReposMaterialParseResultManager(healthService, configRepoService);
        this.configPluginService = configPluginService;
        this.serverHealthService = healthService;
        this.configWatchList = configWatchList;
        this.goConfigService = goConfigService;

        this.configWatchList.registerListener(this);
        this.goConfigService.register(new EntityConfigChangedListener<ConfigRepoConfig>() {
            @Override
            public void onEntityConfigChange(ConfigRepoConfig entity) {
                onConfigRepoConfigChange(entity);
            }
        });
        configReposMaterialParseResultManager.attachConfigUpdateListeners(this.goConfigService);
    }

    public boolean hasListener(PartialConfigUpdateCompletedListener listener) {
        return this.listeners.contains(listener);
    }

    public void registerListener(PartialConfigUpdateCompletedListener listener) {
        this.listeners.add(listener);
    }

    public boolean latestParseHasFailedForMaterial(MaterialConfig material) {
        PartialConfigParseResult result = getLastParseResult(material);
        if (result == null)
            return false;
        return !result.isSuccessful();
    }

    public PartialConfigParseResult getLastParseResult(MaterialConfig material) {
        return configReposMaterialParseResultManager.get(material.getFingerprint());
    }

    public PartialConfig latestPartialConfigForMaterial(MaterialConfig material) throws Exception {
        PartialConfigParseResult result = getLastParseResult(material);
        if (result == null)
            return null;
        if (!result.isSuccessful())
            throw result.getLastFailure();

        return result.lastGoodPartialConfig();
    }

    public boolean hasConfigRepoConfigChangedSinceLastUpdate(MaterialConfig material) {
        return modifiedConfigRepoConfigsAwaitingParse.contains(configWatchList.getConfigRepoForMaterial(material));
    }

    @Override
    public void onChangedRepoConfigWatchList(ConfigReposConfig newConfigRepos) {
        // remove partial configs from map which are no longer on the list
        for (String fingerprint : this.configReposMaterialParseResultManager.allFingerprints()) {
            if (!newConfigRepos.hasMaterialWithFingerprint(fingerprint)) {
                this.configReposMaterialParseResultManager.remove(fingerprint);
            }
        }
    }

    public void onCheckoutComplete(MaterialConfig material, File folder, Modification modification) {
        // called when pipelines/flyweight/[flyweight] has a clean checkout of latest material

        // Having modifications in signature might seem like an overkill
        // but on the other hand if plugin is smart enough it could
        // parse only files that have changed, which is a huge performance gain where there are many pipelines

        /* if this is material listed in config-repos
           Then ask for config plugin implementation
           Give it the directory and store partial config
           post event about completed (successful or not) parsing
         */

        String fingerprint = material.getFingerprint();
        if (this.configWatchList.hasConfigRepoWithFingerprint(fingerprint)) {
            PartialConfigProvider plugin = null;
            ConfigRepoConfig repoConfig = configWatchList.getConfigRepoForMaterial(material);
            HealthStateScope scope = HealthStateScope.forPartialConfigRepo(repoConfig);

            try {
                plugin = this.configPluginService.partialConfigProviderFor(repoConfig);
            } catch (Exception ex) {
                this.configReposMaterialParseResultManager.parseFailed(fingerprint, modification, ex);
                LOGGER.error("Failed to get config plugin for {}", material.getDisplayName());
                String message = String.format("Failed to obtain configuration plugin '%s' for material: %s",
                        repoConfig.getPluginId(), material.getLongDescription());
                String errorDescription = ex.getMessage() == null ? ex.toString()
                        : ex.getMessage();
                serverHealthService.update(ServerHealthState.error(message, errorDescription, HealthStateType.general(scope)));
                notifyFailureListeners(repoConfig, ex);
                return;
            }
            try {
                this.modifiedConfigRepoConfigsAwaitingParse.remove(repoConfig);
                //TODO put modifications and previous partial config in context
                // the context is just a helper for plugin.
                PartialConfigLoadContext context = new LoadContext(repoConfig);
                PartialConfig newPart = plugin.load(folder, context);
                if (newPart == null) {
                    LOGGER.warn("Parsed configuration material {} by {} is null", material.getDisplayName(), plugin.displayName());
                    newPart = new PartialConfig();
                }

                newPart.setOrigins(new RepoConfigOrigin(repoConfig, modification.getRevision()));
                this.configReposMaterialParseResultManager.parseSuccess(fingerprint, modification, newPart);

                // it is the responsibility of the success listeners to clear the ServerHealthState
                notifySuccessListeners(repoConfig, newPart);
            } catch (Exception ex) {
                this.configReposMaterialParseResultManager.parseFailed(fingerprint, modification, ex);
                LOGGER.error("Failed to parse configuration material {} by {}", material.getDisplayName(), plugin.displayName(), ex);
                String message = String.format("Parsing configuration repository using %s failed for material: %s",
                        plugin.displayName(), material.getLongDescription());
                String errorDescription = ex.getMessage() == null ? ex.toString()
                        : ex.getMessage();
                serverHealthService.update(ServerHealthState.error(message, errorDescription, HealthStateType.general(scope)));
                notifyFailureListeners(repoConfig, ex);
            }
        }
    }

    protected void onConfigRepoConfigChange(ConfigRepoConfig configRepoConfig) {
        modifiedConfigRepoConfigsAwaitingParse.add(configRepoConfig);
    }

    private void notifyFailureListeners(ConfigRepoConfig repoConfig, Exception ex) {
        for (PartialConfigUpdateCompletedListener listener : this.listeners) {
            try {
                listener.onFailedPartialConfig(repoConfig, ex);
            } catch (Exception e) {
                LOGGER.error("Failed to fire event 'exception while parsing partial configuration' for listener {}", listener);
            }
        }
    }

    private void notifySuccessListeners(ConfigRepoConfig repoConfig, PartialConfig newPart) {
        if (this.listeners.isEmpty()) {
            // likely to never get here, but including for logical completeness
            serverHealthService.removeByScope(HealthStateScope.forPartialConfigRepo(repoConfig));
            return;
        }

        for (PartialConfigUpdateCompletedListener listener : this.listeners) {
            try {
                listener.onSuccessPartialConfig(repoConfig, newPart);
            } catch (Exception e) {
                LOGGER.error("Failed to fire parsed partial configuration for listener {}", listener);
            }
        }
    }

    public String getRevisionAtLastAttempt(MaterialConfig material) {
        PartialConfigParseResult result = getLastParseResult(material);
        if (result == null || result.getLatestParsedModification() == null)
            return null;

        return result.getLatestParsedModification().getRevision();
    }

    private class LoadContext implements PartialConfigLoadContext {
        private final ConfigRepoConfig repoConfig;

        public LoadContext(ConfigRepoConfig repoConfig) {
            this.repoConfig = repoConfig;
        }

        @Override
        public Configuration configuration() {
            return repoConfig.getConfiguration();
        }

        @Override
        public MaterialConfig configMaterial() {
            return this.repoConfig.getRepo();
        }
    }
}
