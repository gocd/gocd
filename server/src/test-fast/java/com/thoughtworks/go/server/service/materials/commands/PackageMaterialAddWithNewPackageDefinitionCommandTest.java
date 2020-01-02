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
import java.util.Map;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.server.domain.Username;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PackageMaterialAddWithNewPackageDefinitionCommandTest extends PackageMaterialSaveCommandTestBase {
    @Test
    public void shouldUpdateConfigWithNewMaterialAndPackageDefinition() {
        String repoId = cruiseConfig.getPackageRepositories().get(0).getId();
        String pkgName = "new-package";
        HashMap<String, Serializable> params = PackageDefinitionMother.paramsForPackageMaterialCreation(repoId, pkgName);

        PackageMaterialConfig materialToBeCreated = new PackageMaterialConfig();

        PackageMaterialAddWithNewPackageDefinitionCommand command = new PackageMaterialAddWithNewPackageDefinitionCommand(packageDefinitionService,
                securityService, pipelineName, materialToBeCreated, admin, params);

        command.update(cruiseConfig);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        assertThat(pipelineConfig.materialConfigs().last() instanceof PackageMaterialConfig, is(true));
        PackageMaterialConfig packageMaterial = (PackageMaterialConfig) pipelineConfig.materialConfigs().last();
        assertThat(packageMaterial, is(materialToBeCreated));
        assertThat(packageMaterial.getPackageDefinition(), is(notNullValue()));

        assertThat(packageMaterial.getPackageDefinition().getId(), is(notNullValue()));
        assertThat(packageMaterial.getPackageDefinition(), is(notNullValue()));
        assertThat(packageMaterial.getPackageDefinition(), is(notNullValue()));
        assertThat(packageMaterial.getPackageDefinition().getId(), is(notNullValue()));
        assertThat(packageMaterial.getPackageDefinition().getRepository().getId(), is(repoId));
        assertThat(packageMaterial.getPackageDefinition().getName(), is(pkgName));
        assertThat(packageMaterial.getPackageDefinition().getConfiguration().size(), is(2));
        assertThat(packageMaterial.getPackageDefinition().getConfiguration().getProperty("key1").getConfigurationValue().getValue(), is("value1"));
        assertThat(packageMaterial.getPackageDefinition().getConfiguration().getProperty("key2").getConfigurationValue().getValue(), is("value2"));
        verify(packageDefinitionService, times(1)).performPluginValidationsFor(packageMaterial.getPackageDefinition());
    }

    @Test
    public void shouldHandleDeletedPackageRepo() {
        String repoId = "deleted-repo";
        String pkgName = "new-package";
        HashMap<String, Serializable> params = PackageDefinitionMother.paramsForPackageMaterialCreation(repoId, pkgName);

        PackageMaterialConfig materialToBeCreated = new PackageMaterialConfig();

        PackageMaterialAddWithNewPackageDefinitionCommand command = new PackageMaterialAddWithNewPackageDefinitionCommand(packageDefinitionService,
                securityService, pipelineName, materialToBeCreated, admin, params);

        command.update(cruiseConfig);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        assertThat(pipelineConfig.materialConfigs().last() instanceof PackageMaterialConfig, is(true));
        PackageMaterialConfig packageMaterial = (PackageMaterialConfig) pipelineConfig.materialConfigs().last();
        assertThat(packageMaterial, is(materialToBeCreated));

        assertThat(packageMaterial.getPackageId(), is(nullValue()));
        assertThat(packageMaterial.getPackageDefinition(), is(notNullValue()));
        assertThat(packageMaterial.getPackageDefinition().getId(), is(nullValue()));
        assertThat(packageMaterial.getPackageDefinition().getRepository(), is(nullValue()));
        assertThat(packageMaterial.getPackageDefinition().getName(), is(pkgName));
        assertThat(packageMaterial.getPackageDefinition().getConfiguration().size(), is(0));
        verify(packageDefinitionService, never()).performPluginValidationsFor(any(PackageDefinition.class));
    }


    @Test
    public void shouldPerformValidationsBeforeDeleteEmptyConfigurationsHappensDuringAddingPackageIntoConfig() throws Exception {
        String repoId = "repoId";
        HashMap repositoryIdMap = new HashMap();
        repositoryIdMap.put("repositoryId", repoId);
        Map params = new HashMap();
        params.put("package_definition", repositoryIdMap);

        CruiseConfig config = mock(BasicCruiseConfig.class);
        PackageRepositories packageRepositories = mock(PackageRepositories.class);
        when(packageRepositories.find(repoId)).thenReturn(mock(PackageRepository.class));
        when(config.getPackageRepositories()).thenReturn(packageRepositories);

        PackageMaterialAddWithNewPackageDefinitionCommand command = spy(new PackageMaterialAddWithNewPackageDefinitionCommand(packageDefinitionService, securityService, pipelineName, new PackageMaterialConfig(), admin, params));

        command.createNewPackageDefinition(config);

        InOrder inOrder = inOrder(command, config);
        inOrder.verify(config).savePackageDefinition(any(PackageDefinition.class));
    }

    @Override
    protected PackageMaterialSaveCommand getCommand(Username username) {
        return new PackageMaterialAddWithNewPackageDefinitionCommand(packageDefinitionService, securityService, pipelineName,
                new PackageMaterialConfig(), username, PackageDefinitionMother.paramsForPackageMaterialCreation("repo1", "repo1-pkg-1"));
    }
}
