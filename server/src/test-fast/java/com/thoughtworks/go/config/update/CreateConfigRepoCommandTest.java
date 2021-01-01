/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CreateConfigRepoCommandTest {
    private static final String REPO_1 = "repo-1";

    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private ConfigRepoConfig configRepo;

    private HttpLocalizedOperationResult result;

    @Mock
    private SecurityService securityService;
    @Mock
    private ConfigRepoExtension configRepoExtension;

    @BeforeEach
    public void setup() throws Exception {
        initMocks(this);
        currentUser = new Username(new CaseInsensitiveString("user"));
        result = new HttpLocalizedOperationResult();

        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        configRepo = ConfigRepoConfig.createConfigRepoConfig(git("https://foo.git", "master"), "json-plugin", REPO_1);
    }

    @Test
    public void shouldAddTheSpecifiedConfigRepo() {
        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, currentUser, result, configRepoExtension);
        assertNull(cruiseConfig.getConfigRepos().getConfigRepo(REPO_1));
        command.update(cruiseConfig);
        assertEquals(configRepo, cruiseConfig.getConfigRepos().getConfigRepo(REPO_1));
    }

    @Test
    public void isValid_shouldValidateConfigRepo() {
        GitMaterialConfig material = git("", "master");
        configRepo.setRepo(material);
        when(configRepoExtension.canHandlePlugin(configRepo.getPluginId())).thenReturn(true);

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, currentUser, result, configRepoExtension);
        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertEquals("URL cannot be blank", configRepo.getRepo().errors().on("url"));
    }

    @Test
    public void isValid_shouldValidatePresenceOfId() {
        ConfigRepoConfig configRepo = new ConfigRepoConfig();
        configRepo.setId("");

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, currentUser, result, configRepoExtension);

        assertFalse(command.isValid(cruiseConfig));
        assertEquals("Configuration repository id not specified", configRepo.errors().on("id"));
    }

    @Test
    public void isValid_shouldValidatePluginId() {
        when(configRepoExtension.canHandlePlugin(configRepo.getPluginId())).thenReturn(false);

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, currentUser, result, configRepoExtension);

        assertFalse(command.isValid(cruiseConfig));
        assertEquals("Invalid plugin id: json-plugin", configRepo.errors().on("plugin_id"));
    }
}
