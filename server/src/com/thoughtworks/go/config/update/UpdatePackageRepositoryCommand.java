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
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class UpdatePackageRepositoryCommand extends PackageRepositoryCommand {
    private final GoConfigService goConfigService;
    private String oldRepoId;
    private final PackageRepository newRepo;
    private final Username username;
    private final String md5;
    private final EntityHashingService entityHashingService;
    private final HttpLocalizedOperationResult result;

    public UpdatePackageRepositoryCommand(GoConfigService goConfigService, PackageRepositoryService packageRepositoryService, String oldRepoId, PackageRepository newRepo, Username username, PluginManager pluginManager, String md5, EntityHashingService entityHashingService, HttpLocalizedOperationResult result) {
        super(packageRepositoryService, newRepo, result, goConfigService, username);
        this.goConfigService = goConfigService;
        this.oldRepoId = oldRepoId;
        this.newRepo = newRepo;
        this.username = username;
        this.md5 = md5;
        this.entityHashingService = entityHashingService;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        PackageRepository oldRepo = preprocessedConfig.getPackageRepositories().find(oldRepoId);
        this.newRepo.setPackages(oldRepo.getPackages());
        PackageRepositories repositories = preprocessedConfig.getPackageRepositories();
        repositories.removePackageRepository(oldRepo.getId());
        repositories.add(this.newRepo);
        preprocessedConfig.setPackageRepositories(repositories);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh();
    }

    private boolean isRequestFresh() {
        PackageRepository oldRepo = goConfigService.getPackageRepository(oldRepoId);
        boolean freshRequest = entityHashingService.md5ForEntity(oldRepo).equals(md5);
        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Package Repository", oldRepoId));
        }
        return freshRequest;
    }
}
