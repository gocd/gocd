/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateConfigRepoCommandTest {
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

    @BeforeEach
    public void setup() {
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        oldConfigRepoId = "old-repo";
        newConfigRepoId = "new-repo";
        oldConfigRepo = ConfigRepoConfig.createConfigRepoConfig(git("foo.git", "master"), "json-plugin", oldConfigRepoId);
        newConfigRepo = ConfigRepoConfig.createConfigRepoConfig(git("bar.git", "master"), "yaml-plugin", newConfigRepoId);
        result = new HttpLocalizedOperationResult();
        digest = "digest";
        cruiseConfig.getConfigRepos().add(oldConfigRepo);
    }

    @Test
    public void shouldUpdateTheSpecifiedConfigRepo() {
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(entityHashingService, oldConfigRepoId, newConfigRepo, digest, result, configRepoExtension);

        assertNull(cruiseConfig.getConfigRepos().getConfigRepo(newConfigRepoId));
        command.update(cruiseConfig);
        assertNotNull(cruiseConfig.getConfigRepos().getConfigRepo(newConfigRepoId));
    }

    @Test
    public void shouldNotContinueIfDigestIsStale() {
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(entityHashingService, oldConfigRepoId, newConfigRepo, digest, result, configRepoExtension);
        when(entityHashingService.hashForEntity(oldConfigRepo)).thenReturn("some-hash");
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.stale(EntityType.ConfigRepo.staleConfig(oldConfigRepoId));

        assertThat(command.canContinue(cruiseConfig)).isFalse();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void isValid_shouldValidateConfigRepo() {
        newConfigRepo.setRepo(git("foobar.git", "master"));
        cruiseConfig.getConfigRepos().add(newConfigRepo);
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(entityHashingService, oldConfigRepoId, newConfigRepo, digest, result, configRepoExtension);
        when(configRepoExtension.canHandlePlugin(newConfigRepo.getPluginId())).thenReturn(true);

        command.update(cruiseConfig);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(newConfigRepo.errors().size()).isEqualTo(2);
        assertThat(newConfigRepo.errors().firstErrorOn("material")).isEqualTo("You have defined multiple configuration repositories with the same repository - 'foobar.git'.");
        assertThat(newConfigRepo.errors().firstErrorOn("id")).isEqualTo("You have defined multiple configuration repositories with the same id - 'new-repo'.");
    }

    @Test
    public void isValid_shouldValidatePresenceOfId() {
        ConfigRepoConfig configRepo = new ConfigRepoConfig();
        configRepo.setId("");

        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(entityHashingService, oldConfigRepoId, configRepo, digest, result, configRepoExtension);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(configRepo.errors().firstErrorOn("id")).isEqualTo("Configuration repository id not specified");
    }

    @Test
    public void isValid_shouldValidatePluginId() {
        ConfigRepoConfig configRepo = new ConfigRepoConfig();
        configRepo.setPluginId("invalid_id");

        when(configRepoExtension.canHandlePlugin(configRepo.getPluginId())).thenReturn(false);

        CreateConfigRepoCommand command = new CreateConfigRepoCommand(configRepo, configRepoExtension);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(configRepo.errors().firstErrorOn("plugin_id")).isEqualTo("Invalid plugin id: invalid_id");
    }
}
