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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;


public class PackageMaterialAddWithNewPackageDefinitionCommand extends PackageMaterialSaveCommand {
    public PackageMaterialAddWithNewPackageDefinitionCommand(PackageDefinitionService packageDefinitionService, SecurityService securityService, String pipeline,
                                                             PackageMaterialConfig packageMaterialConfig,
                                                             Username username, Map params) {
        super(packageDefinitionService, securityService, pipeline, packageMaterialConfig, username, params);
    }

    @Override
    protected void updateConfig(CruiseConfig cruiseConfig) {
        PackageDefinition packageDefinition = createNewPackageDefinition(cruiseConfig);
        packageMaterialConfig.setPackageDefinition(packageDefinition);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipeline));
        pipelineConfig.addMaterialConfig(packageMaterialConfig);
    }
}

