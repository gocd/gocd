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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import java.util.List;

public class UpdatePackageRepositoryCommand extends PackageRepositoryCommand {
    private final GoConfigService goConfigService;
    private final PackageRepository newRepo;
    private final String digest;
    private final EntityHashingService entityHashingService;
    private final HttpLocalizedOperationResult result;
    private String oldRepoId;

    public UpdatePackageRepositoryCommand(GoConfigService goConfigService, PackageRepositoryService packageRepositoryService, PackageRepository newRepo, Username username, String digest, EntityHashingService entityHashingService, HttpLocalizedOperationResult result, String oldRepoId) {
        super(packageRepositoryService, newRepo, result, goConfigService, username);
        this.goConfigService = goConfigService;
        this.newRepo = newRepo;
        this.digest = digest;
        this.entityHashingService = entityHashingService;
        this.result = result;
        this.oldRepoId = oldRepoId;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) {
        PackageRepository oldRepo = modifiedConfig.getPackageRepositories().find(newRepo.getRepoId());
        this.newRepo.setPackages(oldRepo.getPackages());
        PackageRepositories repositories = modifiedConfig.getPackageRepositories();
        repositories.replace(oldRepo, newRepo);
        modifiedConfig.setPackageRepositories(repositories);

        updatePackageRepositoryConfigurationOnAssociatedPipelines(modifiedConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isIdSame() && isRequestFresh();
    }

    private void updatePackageRepositoryConfigurationOnAssociatedPipelines(CruiseConfig modifiedConfig) {
        List<PipelineConfig> pipelinesWithPackageRepo = modifiedConfig.pipelinesAssociatedWithPackageRepository(newRepo);
        pipelinesWithPackageRepo.forEach(pipelineConfig -> {
            pipelineConfig.packageMaterialConfigs().forEach(packageMaterialConfig -> {
                if (packageMaterialConfig.getPackageDefinition().getRepository().getId().equals(newRepo.getId())) {
                    packageMaterialConfig.getPackageDefinition().setRepository(newRepo);
                }
            });
        });
    }

    private boolean isIdSame() {
        boolean isRepoIdSame = newRepo.getRepoId().equals(oldRepoId);
        if (!isRepoIdSame) {
            result.unprocessableEntity("Changing the repository id is not supported by this API.");
        }
        return isRepoIdSame;
    }

    private boolean isRequestFresh() {
        PackageRepository oldRepo = goConfigService.getPackageRepository(newRepo.getRepoId());
        boolean freshRequest = entityHashingService.hashForEntity(oldRepo).equals(digest);
        if (!freshRequest) {
            result.stale(EntityType.PackageRepository.staleConfig(newRepo.getRepoId()));
        }
        return freshRequest;
    }
}
