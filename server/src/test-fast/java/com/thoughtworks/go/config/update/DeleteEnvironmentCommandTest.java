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
import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.merge.MergeConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DeleteEnvironmentCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private BasicEnvironmentConfig environmentConfig;
    private CaseInsensitiveString environmentName;
    private HttpLocalizedOperationResult result;
    private String actionFailed;

    @Mock
    private GoConfigService goConfigService;

    @BeforeEach
    public void setup() throws Exception {
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        environmentName = new CaseInsensitiveString("Dev");
        environmentConfig = new BasicEnvironmentConfig(environmentName);
        result = new HttpLocalizedOperationResult();
        cruiseConfig.addEnvironment(environmentConfig);
        actionFailed = "Could not delete environment " + environmentName;
    }

    @Test
    public void shouldDeleteTheSpecifiedEnvironment() {
        DeleteEnvironmentCommand command = new DeleteEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        assertTrue(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));
        command.update(cruiseConfig);
        assertFalse(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));
    }

    @Test
    public void shouldBeAbleToDeleteEvenIfTheSpecifiedEnvContainsAgents() {
        BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(environmentName);
        environmentConfig.addAgent("uuid");
        DeleteEnvironmentCommand command = new DeleteEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        assertTrue(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));
        command.update(cruiseConfig);
        assertFalse(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));

    }

    @Test
    public void shouldNotBeAbleToDeleteIfEnvironmentPartiallyDefinedInConfigRepository() {
        BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(environmentName);
        RepoConfigOrigin repoConfigOrigin = new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin", "repo1"), "rev1");
        MergeConfigOrigin origins = new MergeConfigOrigin(repoConfigOrigin);
        environmentConfig.setOrigins(origins);
        DeleteEnvironmentCommand command = new DeleteEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        assertFalse(command.canContinue(cruiseConfig));
        String expectedMessage = "Could not delete environment Dev Environment is partially defined in [repo1] config repositories";
        assertEquals(result.message(), expectedMessage);
    }

}
