/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CreateConfigRepoCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private ConfigRepoConfig configRepo;
    private String repoId = "repo-1";

    private HttpLocalizedOperationResult result;

    @Mock
    private SecurityService securityService;
    private Localizable.CurryableLocalizable actionFailed;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        actionFailed = LocalizedMessage.string("RESOURCE_ADD_FAILED", "Configs repo");
        result = new HttpLocalizedOperationResult();

        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        configRepo = new ConfigRepoConfig(new GitMaterialConfig("https://foo.git", "master"), "json-plugin", repoId);
    }

    @Test
    public void shouldAddTheSpecifiedConfigRepo() throws Exception {
        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, actionFailed, currentUser, result);
        assertNull(cruiseConfig.getConfigRepos().getConfigRepo(repoId));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getConfigRepos().getConfigRepo(repoId), is(configRepo));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnPackages() throws Exception {
        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, actionFailed, currentUser, result);
        when(securityService.isUserAdmin(currentUser)).thenReturn(false);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldValidateDuplicateRepoId() throws Exception {
        ConfigRepoConfig anotherconfigRepo = new ConfigRepoConfig(new GitMaterialConfig("https://foos.git", "master"), "json-plugin", repoId);
        cruiseConfig.getConfigRepos().add(anotherconfigRepo);

        String error = "You have defined multiple configuration repositories with the same id - repo-1";

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(actionFailed.addParam(Arrays.asList(error)));

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, actionFailed, currentUser, result);
        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(configRepo.errors().firstError(), is(error));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldValidateDuplicateMaterial() throws Exception {
        ConfigRepoConfig anotherConfigRepo = new ConfigRepoConfig(new GitMaterialConfig("https://foo.git", "master"), "json-plugin", "anotherid");
        cruiseConfig.getConfigRepos().add(anotherConfigRepo);

        String error = "You have defined multiple configuration repositories with the same repository - https://foo.git";
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(actionFailed.addParam(Arrays.asList(error)));

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, actionFailed, currentUser, result);
        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(configRepo.errors().size(), is(1));
        assertThat(configRepo.errors().firstError(), is(error));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        when(securityService.isUserAdmin(currentUser)).thenReturn(true);
        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, actionFailed, currentUser, result);
        assertThat(command.canContinue(cruiseConfig), is(true));
    }
}
