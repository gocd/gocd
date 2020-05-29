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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
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
    private String digest;

    @Mock
    private SecurityService securityService;

    @Mock
    private EntityHashingService entityHashingService;

    @Mock
    private ConfigRepoExtension configRepoExtension;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        oldConfigRepoId = "old-repo";
        newConfigRepoId = "new-repo";
        oldConfigRepo = ConfigRepoConfig.createConfigRepoConfig(git("foo.git", "master"), "json-plugin", oldConfigRepoId);
        newConfigRepo = ConfigRepoConfig.createConfigRepoConfig(git("bar.git", "master"), "yaml-plugin", newConfigRepoId);
        result = new HttpLocalizedOperationResult();
        digest = "digest";
        cruiseConfig.getConfigRepos().add(oldConfigRepo);
    }

    @Test
    public void shouldUpdateTheSpecifiedConfigRepo() throws Exception {
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, oldConfigRepoId, newConfigRepo, digest, currentUser, result, configRepoExtension);

        assertNull(cruiseConfig.getConfigRepos().getConfigRepo(newConfigRepoId));
        command.update(cruiseConfig);
        assertNotNull(cruiseConfig.getConfigRepos().getConfigRepo(newConfigRepoId));
    }

    @Test
    public void shouldNotContinueIfDigestIsStale() throws Exception {
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, oldConfigRepoId, newConfigRepo, digest, currentUser, result, configRepoExtension);
        when(securityService.isUserAdmin(currentUser)).thenReturn(true);
        when(entityHashingService.hashForEntity(oldConfigRepo)).thenReturn("some-hash");
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.stale(EntityType.ConfigRepo.staleConfig(oldConfigRepoId));

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void isValid_shouldValidateConfigRepo() {
        newConfigRepo.setRepo(git("foobar.git", "master"));
        cruiseConfig.getConfigRepos().add(newConfigRepo);
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, oldConfigRepoId, newConfigRepo, digest, currentUser, result, configRepoExtension);
        when(configRepoExtension.canHandlePlugin(newConfigRepo.getPluginId())).thenReturn(true);

        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(newConfigRepo.errors().size(), is(2));
        assertThat(newConfigRepo.errors().on("material"), is("You have defined multiple configuration repositories with the same repository - 'foobar.git'."));
        assertThat(newConfigRepo.errors().on("id"), is("You have defined multiple configuration repositories with the same id - 'new-repo'."));
    }

    @Test
    public void isValid_shouldValidatePresenceOfId() {
        ConfigRepoConfig configRepo = new ConfigRepoConfig();
        configRepo.setId("");

        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, oldConfigRepoId, configRepo, digest, currentUser, result, configRepoExtension);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(configRepo.errors().on("id"), is("Configuration repository id not specified"));
    }

    @Test
    public void isValid_shouldValidatePluginId() {
        ConfigRepoConfig configRepo = new ConfigRepoConfig();
        configRepo.setPluginId("invalid_id");

        when(configRepoExtension.canHandlePlugin(configRepo.getPluginId())).thenReturn(false);

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, currentUser, result, configRepoExtension);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(configRepo.errors().on("plugin_id"), is("Invalid plugin id: invalid_id"));
    }
}
