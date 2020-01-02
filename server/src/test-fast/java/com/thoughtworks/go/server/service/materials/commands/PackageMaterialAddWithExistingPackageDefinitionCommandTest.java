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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.server.domain.Username;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PackageMaterialAddWithExistingPackageDefinitionCommandTest extends PackageMaterialSaveCommandTestBase {
    @Test
    public void shouldUpdateConfigWithNewMaterial() {
        PackageDefinition packageDefinition = cruiseConfig.getPackageRepositories().get(0).getPackages().get(0);
        String repoId = packageDefinition.getRepository().getId();
        String pkgId = packageDefinition.getId();
        HashMap<String, Serializable> params = PackageDefinitionMother.paramsForPackageMaterialAssociation(repoId, pkgId);
        PackageMaterialConfig materialToBeCreated = new PackageMaterialConfig();

        PackageMaterialAddWithExistingPackageDefinitionCommand command = new PackageMaterialAddWithExistingPackageDefinitionCommand(packageDefinitionService, securityService, pipelineName,
                materialToBeCreated, admin, params);

        command.update(cruiseConfig);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        assertThat(pipelineConfig.materialConfigs().last() instanceof PackageMaterialConfig, is(true));
        PackageMaterialConfig packageMaterial = (PackageMaterialConfig) pipelineConfig.materialConfigs().last();
        assertThat(packageMaterial, is(materialToBeCreated));
        assertThat(packageMaterial.getPackageDefinition(), is(packageDefinition));
        verify(packageDefinitionService, never()).performPluginValidationsFor(packageDefinition);
    }

    @Test
    public void shouldHandleDeletedPackageRepo() {
        PackageDefinition packageDefinition = cruiseConfig.getPackageRepositories().get(0).getPackages().get(0);
        String repoId = "deleted-repo";
        String pkgId = packageDefinition.getId();
        HashMap<String, Serializable> params = PackageDefinitionMother.paramsForPackageMaterialAssociation(repoId, pkgId);
        PackageMaterialConfig materialToBeCreated = new PackageMaterialConfig();

        PackageMaterialAddWithExistingPackageDefinitionCommand command = new PackageMaterialAddWithExistingPackageDefinitionCommand(packageDefinitionService, securityService, pipelineName,
                materialToBeCreated, admin, params);

        command.update(cruiseConfig);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        assertThat(pipelineConfig.materialConfigs().last() instanceof PackageMaterialConfig, is(true));
        PackageMaterialConfig packageMaterial = (PackageMaterialConfig) pipelineConfig.materialConfigs().last();

        assertThat(packageMaterial, is(materialToBeCreated));
        assertThat(packageMaterial.getPackageDefinition(), is(nullValue()));
        assertThat(packageMaterial.getPackageId(), is(nullValue()));
        verify(packageDefinitionService, never()).performPluginValidationsFor(any(PackageDefinition.class));
    }

    @Test
    public void shouldHandleDeletedPackageDefinition() {
        PackageDefinition packageDefinition = cruiseConfig.getPackageRepositories().get(0).getPackages().get(0);
        String repoId = packageDefinition.getRepository().getId();
        String pkgId = "deleted-package-definition";
        HashMap<String, Serializable> params = PackageDefinitionMother.paramsForPackageMaterialAssociation(repoId, pkgId);
        PackageMaterialConfig materialToBeCreated = new PackageMaterialConfig();

        PackageMaterialAddWithExistingPackageDefinitionCommand command = new PackageMaterialAddWithExistingPackageDefinitionCommand(packageDefinitionService, securityService, pipelineName,
                materialToBeCreated, admin, params);

        command.update(cruiseConfig);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        assertThat(pipelineConfig.materialConfigs().last() instanceof PackageMaterialConfig, is(true));
        PackageMaterialConfig packageMaterial = (PackageMaterialConfig) pipelineConfig.materialConfigs().last();

        assertThat(packageMaterial, is(materialToBeCreated));
        assertThat(packageMaterial.getPackageDefinition(), is(nullValue()));
        assertThat(packageMaterial.getPackageId(), is(nullValue()));
        verify(packageDefinitionService, never()).performPluginValidationsFor(any(PackageDefinition.class));
    }

    @Override
    protected PackageMaterialSaveCommand getCommand(Username username) {
        return new PackageMaterialAddWithExistingPackageDefinitionCommand(packageDefinitionService, securityService, pipelineName,
                new PackageMaterialConfig(), username, PackageDefinitionMother.paramsForPackageMaterialAssociation("repo1", "repo1-pkg-1"));
    }
}
