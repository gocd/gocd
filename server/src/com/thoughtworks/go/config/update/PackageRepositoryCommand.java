/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import static com.thoughtworks.go.config.ErrorCollector.getAllErrors;

public abstract class PackageRepositoryCommand implements EntityConfigUpdateCommand<PackageRepository> {
    private PackageRepositoryService packageRepositoryService;
    private GoConfigService goConfigService;
    private Username username;
    private final PackageRepository repository;
    private final HttpLocalizedOperationResult result;
    private PackageRepository preprocessedRepository;

    public PackageRepositoryCommand(PackageRepositoryService packageRepositoryService, PackageRepository repository, HttpLocalizedOperationResult result, GoConfigService goConfigService, Username username) {
        this.packageRepositoryService = packageRepositoryService;
        this.repository = repository;
        this.result = result;
        this.goConfigService = goConfigService;
        this.username = username;
    }

    public boolean isValid(CruiseConfig preprocessedConfig) {
        PackageRepositories repositories = preprocessedConfig.getPackageRepositories();
        this.preprocessedRepository = repositories.find(this.repository.getRepoId());
        preprocessedRepository.validate(null);
        repositories.validate(null);
        boolean isValidConfiguration = packageRepositoryService.validatePluginId(preprocessedRepository) && packageRepositoryService.validateRepositoryConfiguration(preprocessedRepository);
        BasicCruiseConfig.copyErrors(preprocessedRepository, this.repository);
        return getAllErrors(this.repository).isEmpty() && isValidConfiguration && result.isSuccessful();
    }

    public void clearErrors() {
        BasicCruiseConfig.clearErrors(this.preprocessedRepository);
    }

    public PackageRepository getPreprocessedEntityConfig() {
        return this.preprocessedRepository;
    }

    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!goConfigService.isAdministrator(username.getUsername())) {
            Localizable noPermission = LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE");
            result.unauthorized(noPermission, HealthStateType.unauthorised());
            return false;
        }
        return true;
    }
}
