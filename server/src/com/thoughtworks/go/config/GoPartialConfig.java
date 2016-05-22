/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @understands current state of configuration part.
 * <p/>
 * Provides partial configurations.
 */
@Component
public class GoPartialConfig implements PartialConfigUpdateCompletedListener, ChangedRepoConfigWatchListListener, PartialsProvider{

    private static final Logger LOGGER = Logger.getLogger(GoPartialConfig.class);
    public static final String INVALID_CRUISE_CONFIG_MERGE = "Invalid Merged Configuration";

    private GoRepoConfigDataSource repoConfigDataSource;
    private GoConfigWatchList configWatchList;
    private final GoConfigService goConfigService;
    private final CachedGoPartials cachedGoPartials;
    private final ServerHealthService serverHealthService;

    @Autowired
    public GoPartialConfig(GoRepoConfigDataSource repoConfigDataSource,
                           GoConfigWatchList configWatchList, GoConfigService goConfigService, CachedGoPartials cachedGoPartials, ServerHealthService serverHealthService) {
        this.repoConfigDataSource = repoConfigDataSource;
        this.configWatchList = configWatchList;
        this.goConfigService = goConfigService;
        this.cachedGoPartials = cachedGoPartials;
        this.serverHealthService = serverHealthService;

        this.configWatchList.registerListener(this);
        this.repoConfigDataSource.registerListener(this);
    }

    public List<PartialConfig> lastPartials() {
        return cachedGoPartials.lastValidPartials();
    }

    @Override
    public void onFailedPartialConfig(ConfigRepoConfig repoConfig, Exception ex) {
        // do nothing here, we keep previous version of part.
        // As an addition we should stop scheduling pipelines defined in that old part.
    }

    @Override
    public synchronized void onSuccessPartialConfig(ConfigRepoConfig repoConfig, PartialConfig newPart) {
        String fingerprint = repoConfig.getMaterialConfig().getFingerprint();
        if (this.configWatchList.hasConfigRepoWithFingerprint(fingerprint)) {
            //TODO maybe validate new part without context of other partials or main config

            // put latest known
            cachedGoPartials.addOrUpdate(fingerprint, newPart);
            if (updateConfig(newPart, fingerprint)) {
                cachedGoPartials.markAsValid(fingerprint, newPart);
            }
        }
    }

    private boolean updateConfig(final PartialConfig newPart, final String fingerprint) {
        try {
            goConfigService.updateConfig(new UpdateConfigCommand() {
                @Override
                public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                    if (newPart != null && fingerprint != null) {
                        cruiseConfig.getPartials().remove(findMatchingPartial(cruiseConfig, fingerprint));
                        cruiseConfig.getPartials().add(new Cloner().deepClone(newPart));

                        for (PartialConfig partial : cruiseConfig.getPartials()) {
                            for (EnvironmentConfig environmentConfig : partial.getEnvironments()) {
                                if (!cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentConfig.name())) {
                                    cruiseConfig.addEnvironment(new BasicEnvironmentConfig(environmentConfig.name()));
                                }
                            }
                            for (PipelineConfigs pipelineConfigs : partial.getGroups()) {
                                if (!cruiseConfig.getGroups().hasGroup(pipelineConfigs.getGroup())) {
                                    cruiseConfig.getGroups().add(new BasicPipelineConfigs(pipelineConfigs.getGroup(), new Authorization()));
                                }
                            }
                        }
                    }
                    return cruiseConfig;
                }
            });
            serverHealthService.update(ServerHealthState.success(HealthStateType.invalidConfigMerge()));
            return true;
        } catch (Exception e) {
            String description = String.format("%s -  Config-Repo: %s", GoConfigValidity.invalid(e).errorMessage(), newPart.getOrigin().displayName());
            ServerHealthState state = ServerHealthState.error(INVALID_CRUISE_CONFIG_MERGE, description, HealthStateType.invalidConfigMerge());
            serverHealthService.update(state);
            return false;
        }
    }

    private PartialConfig findMatchingPartial(CruiseConfig cruiseConfig, String fingerprint) {
        PartialConfig matchingPartial = findMatchingPartial(cruiseConfig, fingerprint, cachedGoPartials.getValid(fingerprint));
        if (matchingPartial == null) {
            matchingPartial = findMatchingPartial(cruiseConfig, fingerprint, cachedGoPartials.getKnown(fingerprint));
        }
        return matchingPartial;
    }

    private PartialConfig findMatchingPartial(CruiseConfig cruiseConfig, String fingerprint, PartialConfig partial) {
        PartialConfig matching = null;
        if (partial != null) {
            for (PartialConfig partialConfig : cruiseConfig.getPartials()) {
                if (partialConfig.getOrigin() instanceof RepoConfigOrigin && (((RepoConfigOrigin) partialConfig.getOrigin()).getMaterial().getFingerprint().equals(fingerprint))) {
                    matching = partialConfig;
                    break;
                }
            }
        }
        return matching;
    }

    @Override
    public synchronized void onChangedRepoConfigWatchList(ConfigReposConfig newConfigRepos) {
        List<String> toRemove = new ArrayList<>();
        // remove partial configs from map which are no longer on the list
        for (String fingerprint : cachedGoPartials.getFingerprintToLatestKnownConfigMap().keySet()) {
            if (!newConfigRepos.hasMaterialWithFingerprint(fingerprint)) {
                cachedGoPartials.removeKnown(fingerprint);
                toRemove.add(fingerprint);
            }
        }
        if (!toRemove.isEmpty()) {
            if (updateConfig(null, null)) {
                for (String fingerprint : toRemove) {
                    this.cachedGoPartials.removeValid(fingerprint);
                }
            }
        }
    }
}
