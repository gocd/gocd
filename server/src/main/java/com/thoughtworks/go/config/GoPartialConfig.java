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
import com.thoughtworks.go.config.update.PartialConfigUpdateCommand;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @understands current state of configuration part.
 * <p/>
 * Provides partial configurations.
 */
@Component
public class GoPartialConfig implements PartialConfigUpdateCompletedListener, ChangedRepoConfigWatchListListener {
    public static final String INVALID_CRUISE_CONFIG_MERGE = "Invalid Merged Configuration";

    private final GoConfigService goConfigService;
    private final CachedGoPartials cachedGoPartials;
    private final ServerHealthService serverHealthService;
    private final EntityHashingService entityHashingService;
    private GoRepoConfigDataSource repoConfigDataSource;
    private GoConfigWatchList configWatchList;

    @Autowired
    public GoPartialConfig(GoRepoConfigDataSource repoConfigDataSource,
                           GoConfigWatchList configWatchList, GoConfigService goConfigService, CachedGoPartials cachedGoPartials, ServerHealthService serverHealthService, EntityHashingService entityHashingService) {
        this.repoConfigDataSource = repoConfigDataSource;
        this.configWatchList = configWatchList;
        this.goConfigService = goConfigService;
        this.cachedGoPartials = cachedGoPartials;
        this.serverHealthService = serverHealthService;
        this.entityHashingService = entityHashingService;

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
    public void onSuccessPartialConfig(ConfigRepoConfig repoConfig, PartialConfig newPart) {
        String fingerprint = repoConfig.getRepo().getFingerprint();
        if (this.configWatchList.hasConfigRepoWithFingerprint(fingerprint)) {
            //TODO maybe validate new part without context of other partials or main config

            if (isPartialDifferentFromLastKnown(newPart, fingerprint)) {
                // caches this partial as the last known merge attempt
                cachedGoPartials.addOrUpdate(fingerprint, newPart);

                if (updateConfig(newPart, fingerprint, repoConfig)) {
                    cachedGoPartials.markAsValid(fingerprint, newPart);
                }
            }
        }
    }

    public CruiseConfig merge(PartialConfig partialConfig, String fingerprint, CruiseConfig cruiseConfig, ConfigRepoConfig repoConfig) {
        PartialConfigUpdateCommand command = buildUpdateCommand(partialConfig, fingerprint, repoConfig);
        command.update(cruiseConfig);
        return cruiseConfig;
    }

    public PartialConfigUpdateCommand buildUpdateCommand(final PartialConfig partial, final String fingerprint, ConfigRepoConfig configRepoConfig) {
        return new PartialConfigUpdateCommand(partial, fingerprint, cachedGoPartials, configRepoConfig);
    }

    private boolean updateConfig(final PartialConfig newPart, final String fingerprint, ConfigRepoConfig repoConfig) {
        try {
            goConfigService.updateConfig(buildUpdateCommand(newPart, fingerprint, repoConfig));
            return true;
        } catch (Exception e) {
            if (repoConfig != null) {
                String description = String.format("%s- For Config Repo: %s", e.getMessage(), newPart.getOrigin().displayName());
                ServerHealthState state = ServerHealthState.error(INVALID_CRUISE_CONFIG_MERGE, description, HealthStateType.general(HealthStateScope.forPartialConfigRepo(repoConfig)));
                serverHealthService.update(state);
            }
            return false;
        }
    }

    @Override
    public void onChangedRepoConfigWatchList(ConfigReposConfig newConfigRepos) {
        // remove partial configs from map which are no longer on the list
        Set<String> known = cachedGoPartials.getFingerprintToLatestKnownConfigMap().keySet();
        for (String fingerprint : known) {
            if (!newConfigRepos.hasMaterialWithFingerprint(fingerprint)) {
                cachedGoPartials.removeKnown(fingerprint);
            }
        }
        Set<String> valid = cachedGoPartials.getFingerprintToLatestValidConfigMap().keySet();
        for (String fingerprint : valid) {
            if (!newConfigRepos.hasMaterialWithFingerprint(fingerprint)) {
                cachedGoPartials.removeValid(fingerprint);
            }
        }
    }

    /**
     * Tests whether a given {@link PartialConfig} is different from the last known cached attempt.
     *
     * @param partial     a {@link PartialConfig}
     * @param fingerprint the config repo material fingerprint ({@link String})
     * @return whether or not the incoming partial is different from the last cached partial
     */
    private boolean isPartialDifferentFromLastKnown(PartialConfig partial, String fingerprint) {
        final PartialConfig previous = cachedGoPartials.getKnown(fingerprint);

        return !hasSameOrigins(previous, partial) || !isStructurallyEquivalent(previous, partial);
    }

    /**
     * Tests whether two {@link PartialConfig} instances define structurally identical configurations.
     *
     * @param previous a {@link PartialConfig}
     * @param incoming a {@link PartialConfig}
     * @return whether or not the structures are identical
     */
    private boolean isStructurallyEquivalent(PartialConfig previous, PartialConfig incoming) {
        return hash(incoming) == hash(previous);
    }

    /**
     * Tests whether two {@link PartialConfig} instances share an identical
     * {@link com.thoughtworks.go.config.remote.ConfigOrigin}.
     * <p>
     * This is needed because we need to update the origins of the generated {@link PipelineConfig} instances to match
     * the revisions of their {@link com.thoughtworks.go.domain.materials.MaterialConfig}s. If they don't, the pipelines
     * will not be scheduled to build.
     * <p>
     * See {@link com.thoughtworks.go.domain.buildcause.BuildCause#pipelineConfigAndMaterialRevisionMatch(PipelineConfig)}.
     *
     * @param previous a {@link PartialConfig}
     * @param incoming a {@link PartialConfig}
     * @return whether or not the origins are identical
     */
    private boolean hasSameOrigins(PartialConfig previous, PartialConfig incoming) {
        return Objects.equals(
                Optional.ofNullable(previous).map(PartialConfig::getOrigin).orElse(null),
                Optional.ofNullable(incoming).map(PartialConfig::getOrigin).orElse(null)
        );
    }

    private int hash(PartialConfig partial) {
        return entityHashingService.computeHashForEntity(partial);
    }
}
