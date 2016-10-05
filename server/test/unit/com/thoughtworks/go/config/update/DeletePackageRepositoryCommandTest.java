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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DeletePackageRepositoryCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private PackageRepository packageRepository;
    private String repoId;
    private HttpLocalizedOperationResult result;

    @Mock
    private PackageRepositoryService service;

    @Mock
    private GoConfigService goConfigService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        packageRepository = new PackageRepository();
        repoId = "npm";
        packageRepository.setId(repoId);
        result = new HttpLocalizedOperationResult();
        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
    }

    @Test
    public void shouldDeletePackageRepository() throws Exception {
        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(repoId), is(packageRepository));
        DeletePackageRepositoryCommand command = new DeletePackageRepositoryCommand(goConfigService, packageRepository, currentUser, result);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getPackageRepositories().size(), is(0));
        assertNull(cruiseConfig.getPackageRepositories().find(repoId));
    }

    @Test
    public void shouldNotDeletePackageRepositoryIfItIsUsedAsAMaterialInPipeline() throws Exception {
        PackageDefinition pkg = new PackageDefinition();
        pkg.setId("pkg");
        packageRepository.addPackage(pkg);
        PackageMaterialConfig packageMaterial = new PackageMaterialConfig("pkg");
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setMaterialConfigs(new MaterialConfigs(packageMaterial));
        cruiseConfig.addPipeline("first", pipeline);

        assertThat(cruiseConfig.getPackageRepositories().size(), is(1));
        assertThat(cruiseConfig.getPackageRepositories().find(repoId), is(packageRepository));
        DeletePackageRepositoryCommand command = new DeletePackageRepositoryCommand(goConfigService, packageRepository, currentUser, result);
        command.update(cruiseConfig);
        assertFalse(command.isValid(cruiseConfig));
    }

    @Test
    public void shouldNotContinueIfTheUserIsNotAdmin() throws Exception {
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE"), HealthStateType.unauthorised());
        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(false);
        DeletePackageRepositoryCommand command = new DeletePackageRepositoryCommand(goConfigService, packageRepository, currentUser, result);

        boolean canContinue = command.canContinue(cruiseConfig);

        assertFalse(canContinue);
        assertThat(result, is(expectedResult));
    }
}