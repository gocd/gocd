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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public abstract class PackageMaterialSaveCommandTestBase {
    @Mock protected PackageDefinitionService packageDefinitionService;
    @Mock protected SecurityService securityService;
    protected CruiseConfig cruiseConfig;
    protected String pipelineName;
    protected Username admin = new Username(new CaseInsensitiveString("admin"));
    private String pipelineGroup;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        cruiseConfig = GoConfigMother.configWithPackageRepo("repo1");
        pipelineName = "test";
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName);
        PackageDefinition packageDefinition = cruiseConfig.getPackageRepositories().get(0).getPackages().get(0);
        pipelineConfig.materialConfigs().add(0, new PackageMaterialConfig(null, "repo1-pkg-1", packageDefinition));
        pipelineGroup = "grp1";
        cruiseConfig.addPipeline(pipelineGroup, pipelineConfig);
    }

    protected abstract PackageMaterialSaveCommand getCommand(Username user);

    @Test
    public void shouldAllowSuperAdminToSavePackageMaterial() throws Exception {
        when(securityService.isUserAdminOfGroup(admin.getUsername(), pipelineGroup)).thenReturn(true);
        PackageMaterialSaveCommand command = getCommand(admin);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        command.checkPermission(cruiseConfig, result);
        assertThat(result.isSuccessful(), is(true));
        verify(securityService, times(1)).isUserAdminOfGroup(admin.getUsername(), pipelineGroup);
    }

    @Test
    public void shouldNotAllowNonAdminToSavePackageMaterial() throws Exception {
        Username view = new Username(new CaseInsensitiveString("view"));
        when(securityService.isUserAdminOfGroup(view.getUsername(), pipelineGroup)).thenReturn(false);
        PackageMaterialSaveCommand command = getCommand(view);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        command.checkPermission(cruiseConfig, result);
        assertThat(result.isSuccessful(), is(false));

        verify(securityService, times(1)).isUserAdminOfGroup(view.getUsername(), pipelineGroup);
    }
}
