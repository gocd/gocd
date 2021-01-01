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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.i18n.LocalizedMessage.cannotDeleteResourceBecauseOfDependentPipelines;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

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
    public void update(CruiseConfig modifiedConfig) {
        existingPackageRepository = modifiedConfig.getPackageRepositories().find(repository.getRepoId());
        PackageRepositories packageRepositories = modifiedConfig.getPackageRepositories();
        packageRepositories.removePackageRepository(this.repository.getId());
        modifiedConfig.setPackageRepositories(packageRepositories);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        boolean canDeleteRepository = preprocessedConfig.canDeletePackageRepository(existingPackageRepository);
        if (!canDeleteRepository) {
            Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> packageUsageInPipelines = goConfigService.getPackageUsageInPipelines();
            String message = cannotDeleteResourceBecauseOfDependentPipelines("package repository", repository.getId(), populateList(packageUsageInPipelines));
            this.result.unprocessableEntity(message);
        }
        return canDeleteRepository;
    }

    private List<String> populateList(Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> packageUsageInPipelines) {
        ArrayList<String> pipleines = new ArrayList<>();
        for(String key: packageUsageInPipelines.keySet()) {
            List<Pair<PipelineConfig, PipelineConfigs>> pairs = packageUsageInPipelines.get(key);
            for(Pair<PipelineConfig, PipelineConfigs> pair : pairs) {
                pipleines.add(pair.first().getName().toLower());
            }
        }
        return pipleines;
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
        return isAuthorized();
    }

    private boolean isAuthorized() {
        if (!(goConfigService.isUserAdmin(username) || goConfigService.isGroupAdministrator(username.getUsername()))) {
            result.forbidden(EntityType.PackageRepository.forbiddenToDelete(repository.getId(), username.getUsername()), forbidden());
            return false;
        }
        return true;
    }
}
