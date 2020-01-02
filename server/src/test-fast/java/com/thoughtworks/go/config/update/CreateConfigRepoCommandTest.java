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
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
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
    @Mock
    private ConfigRepoExtension configRepoExtension;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        result = new HttpLocalizedOperationResult();

        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        configRepo = new ConfigRepoConfig(git("https://foo.git", "master"), "json-plugin", repoId);
    }

    @Test
    public void shouldAddTheSpecifiedConfigRepo() throws Exception {
        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, currentUser, result, configRepoExtension);
        assertNull(cruiseConfig.getConfigRepos().getConfigRepo(repoId));
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getConfigRepos().getConfigRepo(repoId), is(configRepo));
    }

    @Test
    public void isValid_shouldValidateConfigRepo() {
        GitMaterialConfig material = git("https://foo.git", "master");
        material.setAutoUpdate(false);
        configRepo.setMaterialConfig(material);
        when(configRepoExtension.canHandlePlugin(configRepo.getPluginId())).thenReturn(true);

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, currentUser, result, configRepoExtension);
        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(configRepo.getMaterialConfig().errors().on("autoUpdate"), is("Configuration repository material 'https://foo.git' must have autoUpdate enabled."));
    }

    @Test
    public void isValid_shouldValidatePresenceOfId() {
        ConfigRepoConfig configRepo = new ConfigRepoConfig();
        configRepo.setId("");

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, currentUser, result, configRepoExtension);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(configRepo.errors().on("id"), is("Configuration repository id not specified"));
    }

    @Test
    public void isValid_shouldValidatePluginId() {
        when(configRepoExtension.canHandlePlugin(configRepo.getPluginId())).thenReturn(false);

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, currentUser, result, configRepoExtension);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(configRepo.errors().on("plugin_id"), is("Invalid plugin id: json-plugin"));
    }
}
