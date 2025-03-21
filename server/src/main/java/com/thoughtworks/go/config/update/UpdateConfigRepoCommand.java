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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class UpdateConfigRepoCommand extends ConfigRepoCommand {
    private final EntityHashingService entityHashingService;
    private final String repoIdToUpdate;
    private final ConfigRepoConfig newConfigRepo;
    private final String digest;
    private final HttpLocalizedOperationResult result;

    public UpdateConfigRepoCommand(EntityHashingService entityHashingService,
                                   String repoIdToUpdate, ConfigRepoConfig newConfigRepo, String digest,
                                   HttpLocalizedOperationResult result, ConfigRepoExtension configRepoExtension) {
        super(newConfigRepo, configRepoExtension);
        this.entityHashingService = entityHashingService;
        this.repoIdToUpdate = repoIdToUpdate;
        this.newConfigRepo = newConfigRepo;
        this.digest = digest;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        ConfigReposConfig configRepos = preprocessedConfig.getConfigRepos();
        configRepos.replace(configRepos.getConfigRepo(repoIdToUpdate), newConfigRepo);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isRequestFresh(cruiseConfig) && super.canContinue(cruiseConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        ConfigRepoConfig configRepo = cruiseConfig.getConfigRepos().getConfigRepo(repoIdToUpdate);

        boolean freshRequest = entityHashingService.hashForEntity(configRepo).equals(digest);
        if (!freshRequest) {
            result.stale(EntityType.ConfigRepo.staleConfig(repoIdToUpdate));
        }

        return freshRequest;
    }
}
