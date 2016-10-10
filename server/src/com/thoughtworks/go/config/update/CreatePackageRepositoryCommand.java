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
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class CreatePackageRepositoryCommand extends PackageRepositoryCommand{
    private final PackageRepository repository;

    public CreatePackageRepositoryCommand(GoConfigService goConfigService, PackageRepositoryService packageRepositoryService, PackageRepository repository, Username username, HttpLocalizedOperationResult result) {
        super(packageRepositoryService, repository, result, goConfigService, username);
        this.repository = repository;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        PackageRepositories repositories = preprocessedConfig.getPackageRepositories();
        repositories.add(this.repository);
        preprocessedConfig.setPackageRepositories(repositories);
    }
}
