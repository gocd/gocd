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
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class DeletePackageRepositoryCommand implements EntityConfigUpdateCommand<PackageRepository> {
    private final GoConfigService goConfigService;
    private final PackageRepository repository;
    private PackageRepository existingPackageRepository;
    private final Username username;
    private final HttpLocalizedOperationResult result;

    public DeletePackageRepositoryCommand(GoConfigService goConfigService, PackageRepository repository, Username username, HttpLocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.repository = repository;
        this.username = username;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) throws Exception {
        existingPackageRepository = modifiedConfig.getPackageRepositories().find(repository.getRepoId());
        PackageRepositories packageRepositories = modifiedConfig.getPackageRepositories();
        packageRepositories.removePackageRepository(this.repository.getId());
        modifiedConfig.setPackageRepositories(packageRepositories);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        boolean canDeleteRepository = preprocessedConfig.canDeletePackageRepository(existingPackageRepository);
        if (!canDeleteRepository) {

            Localizable.CurryableLocalizable message = LocalizedMessage.string("PACKAGE_REPOSITORY_DELETE_FAILED", repository.getId());
            this.result.unprocessableEntity(message);
        }
        return canDeleteRepository;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(this.repository);
    }

    @Override
    public PackageRepository getPreprocessedEntityConfig() {
        return this.repository;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!goConfigService.isAdministrator(username.getUsername())) {
            Localizable noPermission = LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE");
            result.unauthorized(noPermission, HealthStateType.unauthorised());
            return false;
        }
        return true;
    }
}
