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
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class CreatePackageConfigCommand extends PackageConfigCommand implements EntityConfigUpdateCommand<PackageDefinition> {
    private final PackageDefinition packageDefinition;
    private String repositoryId;
    private final HttpLocalizedOperationResult result;

    public CreatePackageConfigCommand(GoConfigService goConfigService, PackageDefinition packageDefinition, String repositoryId, Username username, HttpLocalizedOperationResult result, PackageDefinitionService packageDefinitionService) {
        super(packageDefinition, result, packageDefinitionService, goConfigService, username);
        this.packageDefinition = packageDefinition;
        this.repositoryId = repositoryId;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) {
        PackageRepositories packageRepositories = modifiedConfig.getPackageRepositories();
        PackageRepository packageRepository = packageRepositories.find(repositoryId);
        int index = packageRepositories.indexOf(packageRepository);
        packageDefinition.setRepository(packageRepository);
        packageRepository.addPackage(this.packageDefinition);
        packageRepositories.replace(index, packageRepository);
        modifiedConfig.setPackageRepositories(packageRepositories);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValid(preprocessedConfig, repositoryId);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!isAuthorized()) {
            return false;
        }
        if (cruiseConfig.getPackageRepositories().find(repositoryId) == null) {
            result.unprocessableEntity(EntityType.PackageRepository.notFoundMessage(repositoryId));
            return false;
        }
        return true;
    }
}
