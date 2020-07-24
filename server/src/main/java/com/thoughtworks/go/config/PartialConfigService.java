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
import com.thoughtworks.go.config.update.PartialConfigUpdateCommand;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

import static java.lang.String.format;

@Component
public class PartialConfigService implements PartialConfigUpdateCompletedListener, ChangedRepoConfigWatchListListener {
    public static final String INVALID_CRUISE_CONFIG_MERGE = "Invalid Merged Configuration";

    private final GoConfigService goConfigService;
    private final CachedGoPartials cachedGoPartials;
    private final ServerHealthService serverHealthService;
    private final PartialConfigHelper partialConfigHelper;
    private final GoConfigRepoConfigDataSource repoConfigDataSource;
    private final GoConfigWatchList configWatchList;

    @Autowired
    public PartialConfigService(GoConfigRepoConfigDataSource repoConfigDataSource,
                                GoConfigWatchList configWatchList, GoConfigService goConfigService,
                                CachedGoPartials cachedGoPartials, ServerHealthService serverHealthService, PartialConfigHelper partialConfigHelper) {
        this.repoConfigDataSource = repoConfigDataSource;
        this.configWatchList = configWatchList;
        this.goConfigService = goConfigService;
        this.cachedGoPartials = cachedGoPartials;
        this.serverHealthService = serverHealthService;
        this.partialConfigHelper = partialConfigHelper;

        this.configWatchList.registerListener(this);
        this.repoConfigDataSource.registerListener(this);
    }

    @Override
    public void onFailedPartialConfig(ConfigRepoConfig repoConfig, Exception ex) {
        // Apply latest config repo rules on last valid partial. Remove if there any rule violations.
        final String fingerprint = repoConfig.getRepo().getFingerprint();

        if (hasRuleViolationsOnPreviousValidPartial(repoConfig)) {
            removeCachedLastValidPartial(fingerprint);
        }
    }

    @Override
    public void onSuccessPartialConfig(ConfigRepoConfig repoConfig, PartialConfig incoming) {
        final String fingerprint = repoConfig.getRepo().getFingerprint();

        if (this.configWatchList.hasConfigRepoWithFingerprint(fingerprint)) {
            if (shouldMergePartial(incoming, fingerprint, repoConfig)) {
                // mark the fingerprint as last known
                cachedGoPartials.cacheAsLastKnown(fingerprint, incoming);

                //validate rules
                hasRuleViolations(incoming);

                /* Validate config.
                UpdateConfig will fail to update the configuration if there are validation errors.
                Even in case of rules violation, the updateConfig method is required to populate a server health message
                of rule violation, which also will be shown on the config repo spa.*/
                if (updateConfig(incoming, fingerprint, repoConfig)) {
                    // mark the partial as valid when config is updated successfully for it.
                    cachedGoPartials.markAsValid(fingerprint, incoming);
                } else {
                    /* If the latest partial is invalid for the current config repo rules.
                    1. Apply latest config repo rules to previous valid partial.
                    2. If the previous valid partials are valid - do nothing - as the error for the latest partial is
                       already populated and config contains the last known partial.
                    3. If the previous valid partials are invalid - remove those config without clearing the server health message.
                       Server health message is populated for the same fingerprint with the latest parse failure message.*/
                    if (hasRuleViolationsOnPreviousValidPartial(repoConfig)) {
                        removeCachedLastValidPartial(fingerprint);
                    }
                }
            }
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

    public CruiseConfig merge(PartialConfig partialConfig, String fingerprint, CruiseConfig cruiseConfig) {
        PartialConfigUpdateCommand command = buildUpdateCommand(partialConfig, fingerprint);
        command.update(cruiseConfig);
        return cruiseConfig;
    }

    protected PartialConfigUpdateCommand buildUpdateCommand(final PartialConfig partial, final String fingerprint) {
        return new PartialConfigUpdateCommand(partial, fingerprint, cachedGoPartials);
    }

    private void removeCachedLastValidPartial(String fingerprint) {
        //  remove cached partial without clearing server health message.
        cachedGoPartials.removeValidWithoutClearingServerHealthMessage(fingerprint);

        /*Removing cached partials is not enough, we need to perform a full config save immediately in order to invoke
        appropriate listeners that removes the pipelines.*/
        //todo: Do we care about error handling while removing the partials?
        goConfigService.updateConfig(cruiseConfig -> {
            cruiseConfig.getPartials().remove(cachedGoPartials.findPartialByFingerprint(cruiseConfig, fingerprint));
            return cruiseConfig;
        });
    }

    private boolean hasRuleViolationsOnPreviousValidPartial(ConfigRepoConfig latestConfigRepoConfig) {
        final PartialConfig previousValidPartial = cachedGoPartials.getValid(latestConfigRepoConfig.getRepo().getFingerprint());

        if (previousValidPartial == null) {
            return false;
        }

        ((RepoConfigOrigin) previousValidPartial.getOrigin()).setConfigRepo(latestConfigRepoConfig);

        return hasRuleViolations(previousValidPartial);
    }

    private boolean updateConfig(final PartialConfig newPart, final String fingerprint, ConfigRepoConfig repoConfig) {
        try {
            goConfigService.updateConfig(buildUpdateCommand(newPart, fingerprint));
            return true;
        } catch (Exception e) {
            if (repoConfig != null) {
                String description = format("%s- For Config Repo: %s", e.getMessage(), newPart.getOrigin().displayName());
                ServerHealthState state = ServerHealthState.error(INVALID_CRUISE_CONFIG_MERGE, description, HealthStateType.general(HealthStateScope.forPartialConfigRepo(repoConfig)));
                serverHealthService.update(state);
            }
            return false;
        }
    }

    private boolean shouldMergePartial(PartialConfig partial, String fingerprint, ConfigRepoConfig repoConfig) {
        return isPartialDifferentFromLastKnown(partial, fingerprint) ||
                repoConfigDataSource.hasConfigRepoConfigChangedSinceLastUpdate(repoConfig.getRepo());
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

        return !partialConfigHelper.isEquivalent(previous, partial);
    }

    private boolean hasRuleViolations(PartialConfig partial) {
        if (null == partial) {
            return false;
        }

        partial.validatePermissionsOnSubtree();
        return partial.hasErrors();
    }
}
