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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigRepoServiceTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;
    @Mock
    private EntityHashingService entityHashingService;
    @Mock
    private ConfigRepoExtension configRepoExtension;
    @Mock
    private MaterialUpdateService materialUpdateService;
    @Mock
    private MaterialConfigConverter converter;

    private ConfigRepoConfig configRepo;

    @BeforeEach
    void setUp() {
        MaterialConfig repoMaterial = git("https://foo.git", "master");
        this.configRepo = ConfigRepoConfig.createConfigRepoConfig(repoMaterial, "json-config-repo-plugin", "repo-1");
    }

    @Test
    void shouldRegisterListener() {
        new ConfigRepoService(goConfigService, securityService, entityHashingService, configRepoExtension, materialUpdateService, converter);
        verify(goConfigService).register(any(EntityConfigChangedListener.class));
    }

    @Test
    void shouldScheduleAMaterialUpdateOnConfigRepoChange() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.getConfigRepos().add(configRepo);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);

        new ConfigRepoService(goConfigService, securityService, entityHashingService, configRepoExtension, materialUpdateService, converter);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntityConfigChangedListener<ConfigRepoConfig>> listenerCaptor = ArgumentCaptor.forClass(EntityConfigChangedListener.class);

        verify(goConfigService).register(listenerCaptor.capture());
        EntityConfigChangedListener<ConfigRepoConfig> listener = listenerCaptor.getValue();

        verify(materialUpdateService, never()).updateMaterial((Material) any());

        listener.onEntityConfigChange(configRepo);

        verify(materialUpdateService, times(1)).updateMaterial((Material) any());
    }

    @Nested
    public class ExceptionHandling {

        ConfigRepoService service;

        @BeforeEach
        void setUp() {
            service = new ConfigRepoService(goConfigService, securityService, entityHashingService, configRepoExtension, materialUpdateService, converter);
        }

        @Test
        void configExceptionsShouldBeConsideredUnprocessable() {

            Username testUser = Username.valueOf("test");
            doThrow(new GoConfigInvalidException(null, "invalid config")).when(goConfigService)
                    .updateConfig(any(EntityConfigUpdateCommand.class), eq(testUser));

            HttpLocalizedOperationResult result = mock(HttpLocalizedOperationResult.class);
            service.updateConfigRepo("repo", configRepo, "digest", testUser, result);

            verify(result).unprocessableEntity(contains("Validations failed for config-repo 'repo-1'. Error(s): [invalid config]."));
        }

        @Test
        void unexpectedExceptionsShouldBeServerErrors() {

            Username testUser = Username.valueOf("test");
            doThrow(new RuntimeException("unexpected exception")).when(goConfigService)
                    .updateConfig(any(EntityConfigUpdateCommand.class), eq(testUser));

            HttpLocalizedOperationResult result = mock(HttpLocalizedOperationResult.class);
            service.updateConfigRepo("repo", configRepo, "digest", testUser, result);

            verify(result).internalServerError(contains("An error occurred while saving the config repo."));
        }

        @Test
        void shouldIgnoreIfResultAlreadyHasMessage() {

            Username testUser = Username.valueOf("test");
            doThrow(new RuntimeException()).when(goConfigService).updateConfig(any(EntityConfigUpdateCommand.class), eq(testUser));

            HttpLocalizedOperationResult result = mock(HttpLocalizedOperationResult.class);
            when(result.hasMessage()).thenReturn(true);

            service.updateConfigRepo("repo", configRepo, "digest", testUser, result);

            verify(result).isSuccessful();
        }
    }

}
