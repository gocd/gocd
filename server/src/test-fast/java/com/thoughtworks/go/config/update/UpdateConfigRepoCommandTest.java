/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.i18n.LocalizedMessage.staleResourceConfig;
import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UpdateConfigRepoCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private ConfigRepoConfig oldConfigRepo;
    private ConfigRepoConfig newConfigRepo;
    private String oldConfigRepoId;
    private String newConfigRepoId;
    private HttpLocalizedOperationResult result;
    private String actionFailed;
    private String md5;

    @Mock
    private SecurityService securityService;

    @Mock
    private EntityHashingService entityHashingService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        oldConfigRepoId = "old-repo";
        newConfigRepoId = "new-repo";
        oldConfigRepo = new ConfigRepoConfig(new GitMaterialConfig("foo.git", "master"), "json-plugin", oldConfigRepoId);
        newConfigRepo = new ConfigRepoConfig(new GitMaterialConfig("bar.git", "master"), "yaml-plugin", newConfigRepoId);
        result = new HttpLocalizedOperationResult();
        md5 = "md5";
        cruiseConfig.getConfigRepos().add(oldConfigRepo);
        actionFailed = "Could not update config repo " + oldConfigRepoId;
    }

    @Test
    public void shouldUpdateTheSpecifiedConfigRepo() throws Exception {
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, oldConfigRepoId, newConfigRepo, md5, currentUser, result);

        assertNull(cruiseConfig.getConfigRepos().getConfigRepo(newConfigRepoId));
        command.update(cruiseConfig);
        assertNotNull(cruiseConfig.getConfigRepos().getConfigRepo(newConfigRepoId));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnConfigRepos() throws Exception {
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, oldConfigRepoId, newConfigRepo, md5, currentUser, result);
        when(securityService.isUserAdmin(currentUser)).thenReturn(false);
        when(entityHashingService.md5ForEntity(oldConfigRepo)).thenReturn(md5);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.forbidden(forbiddenToEdit(), forbidden());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotContinueIfMD5IsStale() throws Exception {
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, oldConfigRepoId, newConfigRepo, md5, currentUser, result);
        when(securityService.isUserAdmin(currentUser)).thenReturn(true);
        when(entityHashingService.md5ForEntity(oldConfigRepo)).thenReturn("some-hash");
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.stale(staleResourceConfig("Config repo", oldConfigRepoId));

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }


    @Test
    public void isValid_shouldValidateConfigRepo() {
        newConfigRepo.setMaterialConfig(new GitMaterialConfig("foobar.git", "master"));
        cruiseConfig.getConfigRepos().add(newConfigRepo);
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, oldConfigRepoId, newConfigRepo, md5, currentUser, result);

        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(newConfigRepo.errors().size(), is(2));
        assertThat(newConfigRepo.errors().on("material"), is("You have defined multiple configuration repositories with the same repository - 'foobar.git'."));
        assertThat(newConfigRepo.errors().on("id"), is("You have defined multiple configuration repositories with the same id - 'new-repo'."));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        when(securityService.isUserAdmin(currentUser)).thenReturn(true);
        when(entityHashingService.md5ForEntity(oldConfigRepo)).thenReturn(md5);

        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, oldConfigRepoId, newConfigRepo, md5, currentUser, result);
        assertThat(command.canContinue(cruiseConfig), is(true));
    }
}
