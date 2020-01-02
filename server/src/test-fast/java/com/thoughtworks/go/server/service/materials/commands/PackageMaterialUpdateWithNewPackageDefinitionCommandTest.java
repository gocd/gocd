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

import java.io.Serializable;
import java.util.HashMap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.server.domain.Username;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PackageMaterialUpdateWithNewPackageDefinitionCommandTest extends PackageMaterialSaveCommandTestBase {
    private PipelineConfig pipelineConfig;
    private PackageMaterialConfig materialToBeUpdated;

    @Before
    public void setUp() throws Exception {
        pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        materialToBeUpdated = (PackageMaterialConfig) pipelineConfig.materialConfigs().first();
    }

    @Test
    public void shouldUpdateMaterialWithNewPackageDefinition() {
        String repoId = "repo1";
        String pkgName = "pkg-2";
        HashMap<String, Serializable> params = PackageDefinitionMother.paramsForPackageMaterialCreation(repoId, pkgName);

        PackageMaterialUpdateWithNewPackageDefinitionCommand command = new PackageMaterialUpdateWithNewPackageDefinitionCommand(packageDefinitionService,
                securityService, pipelineName, materialToBeUpdated, admin, params);

        command.update(cruiseConfig);

        assertThat(pipelineConfig.materialConfigs().first() instanceof PackageMaterialConfig, is(true));
        PackageMaterialConfig editedMaterial = (PackageMaterialConfig) pipelineConfig.materialConfigs().first();

        assertThat(editedMaterial, is(materialToBeUpdated));

        assertThat(editedMaterial.getPackageDefinition(), is(notNullValue()));
        assertThat(editedMaterial.getPackageDefinition().getId(), is(notNullValue()));
        assertThat(editedMaterial.getPackageDefinition().getRepository().getId(), is(repoId));
        assertThat(editedMaterial.getPackageDefinition().getName(), is(pkgName));
        assertThat(editedMaterial.getPackageDefinition().getConfiguration().size(), is(2));
        assertThat(editedMaterial.getPackageDefinition().getConfiguration().getProperty("key1").getConfigurationValue().getValue(), is("value1"));
        assertThat(editedMaterial.getPackageDefinition().getConfiguration().getProperty("key2").getConfigurationValue().getValue(), is("value2"));
        verify(packageDefinitionService, times(1)).performPluginValidationsFor(editedMaterial.getPackageDefinition());
    }

    @Test
    public void shouldHandleDeletedPackageRepo() {
        String repoId = "deleted-repo";
        String pkgName = "pkg-2";
        HashMap<String, Serializable> params = PackageDefinitionMother.paramsForPackageMaterialCreation(repoId, pkgName);

        PackageMaterialUpdateWithNewPackageDefinitionCommand command = new PackageMaterialUpdateWithNewPackageDefinitionCommand(packageDefinitionService,
                securityService, pipelineName, materialToBeUpdated, admin, params);

        command.update(cruiseConfig);

        assertThat(pipelineConfig.materialConfigs().first() instanceof PackageMaterialConfig, is(true));
        PackageMaterialConfig editedMaterial = (PackageMaterialConfig) pipelineConfig.materialConfigs().first();

        assertThat(editedMaterial, is(materialToBeUpdated));

        assertThat(editedMaterial.getPackageDefinition(), is(notNullValue()));
        assertThat(editedMaterial.getPackageDefinition().getId(), is(nullValue()));
        assertThat(editedMaterial.getPackageDefinition().getRepository(), is(nullValue()));
        assertThat(editedMaterial.getPackageDefinition().getName(), is(pkgName));
        assertThat(editedMaterial.getPackageDefinition().getConfiguration().size(), is(0));
        verify(packageDefinitionService, never()).performPluginValidationsFor(editedMaterial.getPackageDefinition());
    }

    @Override
    protected PackageMaterialSaveCommand getCommand(Username username) {
        return new PackageMaterialUpdateWithNewPackageDefinitionCommand(packageDefinitionService, securityService, pipelineName,
                materialToBeUpdated, username, PackageDefinitionMother.paramsForPackageMaterialCreation("repo1", "repo1-pkg-1"));
    }
}
