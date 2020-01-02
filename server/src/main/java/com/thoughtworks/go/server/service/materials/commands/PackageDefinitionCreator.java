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
package com.thoughtworks.go.server.service.materials.commands;

import java.util.Map;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;

public class PackageDefinitionCreator {
    public static final String PACKAGE_DEFINITION = "package_definition";
    public static final String REPOSITORY_ID = "repositoryId";
    public static final String PACKAGE_ID = "packageId";

    private final PackageDefinitionService packageDefinitionService;
    protected final Map params;

    public PackageDefinitionCreator(PackageDefinitionService packageDefinitionService, Map params) {
        this.packageDefinitionService = packageDefinitionService;
        this.params = params;
    }

    void validatePackageDefinition(PackageDefinition packageDefinition) {
        packageDefinitionService.performPluginValidationsFor(packageDefinition);
    }

    public PackageDefinition createNewPackageDefinition(CruiseConfig cruiseConfig) {
        Map packageDefinitionMap = (Map) params.get(PACKAGE_DEFINITION);
        String repositoryId = (String) packageDefinitionMap.get(REPOSITORY_ID);
        PackageRepository packageRepository = cruiseConfig.getPackageRepositories().find(repositoryId);
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setRepository(packageRepository);
        packageDefinition.setConfigAttributes(packageDefinitionMap);

        if (packageRepository != null) {
            validatePackageDefinition(packageDefinition);
            cruiseConfig.savePackageDefinition(packageDefinition);
        }
        return packageDefinition;
    }

    public PackageDefinition getPackageDefinition(CruiseConfig cruiseConfig) {
        Map packageDefinitionMap = (Map) params.get(PACKAGE_DEFINITION);
        String repositoryId = (String) packageDefinitionMap.get(REPOSITORY_ID);
        PackageRepository packageRepository = cruiseConfig.getPackageRepositories().find(repositoryId);
        PackageDefinition packageDefinition = null;
        if (packageRepository != null) {
            packageDefinition = packageRepository.findPackage((String) params.get(PACKAGE_ID));
        }
        return packageDefinition;
    }
}
