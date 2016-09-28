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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class CreatePackageRepositoryCommand extends PackageRepositoryCommand implements EntityConfigUpdateCommand<PackageRepository>{
    private final GoConfigService goConfigService;
    private PackageRepositoryService packageRepositoryService;
    private final PackageRepository repository;
    private final Username username;
    private final HttpLocalizedOperationResult result;

    public CreatePackageRepositoryCommand(GoConfigService goConfigService, PackageRepositoryService packageRepositoryService, PackageRepository repository, Username username, PluginManager pluginManager, HttpLocalizedOperationResult result) {
        super(packageRepositoryService, repository, pluginManager, result);
        this.goConfigService = goConfigService;
        this.packageRepositoryService = packageRepositoryService;
        this.repository = repository;
        this.username = username;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        PackageRepositories repositories = preprocessedConfig.getPackageRepositories();
        repositories.add(this.repository);
        preprocessedConfig.setPackageRepositories(repositories);
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
