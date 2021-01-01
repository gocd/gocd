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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
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

public class DeletePackageConfigCommand implements EntityConfigUpdateCommand<PackageDefinition> {
    private final GoConfigService goConfigService;
    private final PackageDefinition packageDefinition;
    private final Username username;
    private final HttpLocalizedOperationResult result;

    public DeletePackageConfigCommand(GoConfigService goConfigService, PackageDefinition packageDefinition, Username username, HttpLocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.packageDefinition = packageDefinition;
        this.username = username;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) {
        PackageRepositories repositories = modifiedConfig.getPackageRepositories();
        PackageRepository packageRepository = repositories.findPackageRepositoryHaving(this.packageDefinition.getId());
        packageRepository.removePackage(this.packageDefinition.getId());
        modifiedConfig.setPackageRepositories(repositories);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> packageUsageInPipelines = goConfigService.getPackageUsageInPipelines();
        List<String> pipelinesUsingPackages = populateList(packageUsageInPipelines);
        if (!pipelinesUsingPackages.isEmpty()) {
            result.unprocessableEntity(cannotDeleteResourceBecauseOfDependentPipelines("package definition", packageDefinition.getId(), pipelinesUsingPackages));
            return false;
        }
        return true;
    }

    private List<String> populateList(Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> packageUsageInPipelines) {
        ArrayList<String> pipelinesReferringPackages = new ArrayList<>();
        List<Pair<PipelineConfig, PipelineConfigs>> pairs = packageUsageInPipelines.get(packageDefinition.getId());
        if (pairs != null) {
            for (Pair<PipelineConfig, PipelineConfigs> pair : pairs) {
                pipelinesReferringPackages.add(pair.first().getName().toLower());
            }
        }
        return pipelinesReferringPackages;
    }

    @Override
    public void clearErrors() {
    }

    @Override
    public PackageDefinition getPreprocessedEntityConfig() {
        return this.packageDefinition;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized();
    }

    private boolean isAuthorized() {
        if (!(goConfigService.isUserAdmin(username) || goConfigService.isGroupAdministrator(username.getUsername()))) {
            result.forbidden(EntityType.PackageDefinition.forbiddenToDelete(packageDefinition.getId(), username.getUsername()), forbidden());
            return false;
        }
        return true;
    }
}
