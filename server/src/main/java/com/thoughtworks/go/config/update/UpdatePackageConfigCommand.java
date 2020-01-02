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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class UpdatePackageConfigCommand extends PackageConfigCommand {
    private final GoConfigService goConfigService;
    private final String oldPackageId;
    private final PackageDefinition newPackage;
    private String md5;
    private EntityHashingService entityHashingService;
    private final HttpLocalizedOperationResult result;

    public UpdatePackageConfigCommand(GoConfigService goConfigService, String oldPackageId, PackageDefinition newPackage, Username username, String md5, EntityHashingService entityHashingService, HttpLocalizedOperationResult result, PackageDefinitionService packageDefinitionService) {
        super(newPackage, result, packageDefinitionService, goConfigService, username);
        this.goConfigService = goConfigService;
        this.oldPackageId = oldPackageId;
        this.newPackage = newPackage;
        this.md5 = md5;
        this.entityHashingService = entityHashingService;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) {
        PackageRepositories packageRepositories = modifiedConfig.getPackageRepositories();
        PackageRepository repository = packageRepositories.findPackageRepositoryHaving(oldPackageId);
        int index = packageRepositories.indexOf(repository);

        newPackage.setRepository(repository);
        repository.removePackage(oldPackageId);
        repository.addPackage(this.newPackage);

        packageRepositories.replace(index, repository);
        modifiedConfig.setPackageRepositories(packageRepositories);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValid(preprocessedConfig, newPackage.getRepository().getId());
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized() && isRepositoryPresent(cruiseConfig) && isIdSame() && isRequestFresh();
    }

    private boolean isRepositoryPresent(CruiseConfig cruiseConfig) {
        String repoId = newPackage.getRepository().getRepoId();
        if (cruiseConfig.getPackageRepositories().find(repoId) == null) {
            result.unprocessableEntity(EntityType.PackageRepository.notFoundMessage(repoId));
            return false;
        }
        return true;
    }

    private boolean isIdSame() {
        boolean isPackageIdSame = newPackage.getId().equals(oldPackageId);
        if (!isPackageIdSame) {
            result.unprocessableEntity("Changing the package id is not supported by this API.");
        }
        return isPackageIdSame;
    }

    private boolean isRequestFresh() {
        PackageDefinition oldPackage = goConfigService.getConfigForEditing().getPackageRepositories().findPackageDefinitionWith(oldPackageId);
        boolean freshRequest = entityHashingService.md5ForEntity(oldPackage).equals(md5);
        if (!freshRequest) {
            result.stale(EntityType.PackageDefinition.staleConfig(oldPackage.getId()));
        }
        return freshRequest;
    }
}
