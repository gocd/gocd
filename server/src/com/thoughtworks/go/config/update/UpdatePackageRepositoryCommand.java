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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class UpdatePackageRepositoryCommand extends PackageRepositoryCommand implements EntityConfigUpdateCommand<PackageRepository> {
    private final GoConfigService goConfigService;
    private final PackageRepository oldRepo;
    private final PackageRepository newRepo;
    private final Username username;
    private final PluginManager pluginManager;
    private final String md5;
    private final EntityHashingService entityHashingService;
    private final HttpLocalizedOperationResult result;

    public UpdatePackageRepositoryCommand(GoConfigService goConfigService, PackageRepository oldRepo, PackageRepository newRepo, Username username, PluginManager pluginManager, String md5, EntityHashingService entityHashingService, HttpLocalizedOperationResult result) {
        super(goConfigService, newRepo, username, pluginManager, result);
        this.goConfigService = goConfigService;
        this.oldRepo = oldRepo;
        this.newRepo = newRepo;
        this.username = username;
        this.pluginManager = pluginManager;
        this.md5 = md5;
        this.entityHashingService = entityHashingService;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        this.newRepo.setPackages(this.oldRepo.getPackages());
        this.newRepo.setPluginConfiguration(getPluginCofiguration(this.newRepo));
        PackageRepositories repositories = preprocessedConfig.getPackageRepositories();
        repositories.removePackageRepository(this.oldRepo.getId());
        repositories.add(this.newRepo);
        preprocessedConfig.setPackageRepositories(repositories);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        boolean freshRequest = entityHashingService.md5ForEntity(oldRepo, oldRepo.getId()).equals(md5);
        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Package Repository", oldRepo.getId()));
        }
        return freshRequest;
    }

    private boolean isAuthorized(CruiseConfig cruiseConfig) {
        if (!goConfigService.isAdministrator(username.getUsername())) {
            Localizable noPermission = LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE");
            result.unauthorized(noPermission, HealthStateType.unauthorised());
            return false;
        }
        return true;
    }
}
