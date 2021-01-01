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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.Packages;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

abstract class PackageConfigCommand implements EntityConfigUpdateCommand<PackageDefinition> {
    private final PackageDefinition packageDefinition;
    private final HttpLocalizedOperationResult result;
    private PackageDefinitionService packageDefinitionService;
    private GoConfigService goConfigService;
    private Username username;
    private PackageDefinition preprocessedPackageDefinition;

    PackageConfigCommand(PackageDefinition packageDefinition, HttpLocalizedOperationResult result, PackageDefinitionService packageDefinitionService, GoConfigService goConfigService, Username username) {
        this.packageDefinition = packageDefinition;
        this.result = result;
        this.packageDefinitionService = packageDefinitionService;
        this.goConfigService = goConfigService;
        this.username = username;
    }

    public boolean isValid(CruiseConfig preprocessedConfig, String repositoryId) {
        PackageRepositories packageRepositories = preprocessedConfig.getPackageRepositories();
        PackageRepository repository = packageRepositories.find(repositoryId);
        Packages packages = repository.getPackages();
        preprocessedPackageDefinition = packages.find(this.packageDefinition.getId());
        preprocessedPackageDefinition.validate(null);
        if (preprocessedPackageDefinition.getAllErrors().isEmpty()) {
            packageRepositories.validate(null);
            packages.validate(null);
            packageDefinitionService.validatePackageConfiguration(preprocessedPackageDefinition);
            BasicCruiseConfig.copyErrors(preprocessedPackageDefinition, packageDefinition);
            return preprocessedPackageDefinition.getAllErrors().isEmpty() && result.isSuccessful();
        }
        BasicCruiseConfig.copyErrors(preprocessedPackageDefinition, packageDefinition);
        return false;
    }

    protected boolean isAuthorized() {
        if (!(goConfigService.isUserAdmin(username) || goConfigService.isGroupAdministrator(username.getUsername()))) {
            result.forbidden(EntityType.PackageDefinition.forbiddenToEdit(packageDefinition.getId(), username.getUsername()), forbidden());
            return false;
        }
        return true;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(packageDefinition);
    }

    @Override
    public PackageDefinition getPreprocessedEntityConfig() {
        return preprocessedPackageDefinition;
    }
}
