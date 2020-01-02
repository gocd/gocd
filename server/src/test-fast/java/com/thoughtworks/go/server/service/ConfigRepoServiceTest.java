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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

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

    private String repoId;
    private String pluginId;
    private ConfigRepoConfig configRepo;

    @BeforeEach
    void setUp() {
        initMocks(this);

        this.repoId = "repo-1";
        this.pluginId = "json-config-repo-plugin";
        MaterialConfig repoMaterial = git("https://foo.git", "master");
        this.configRepo = new ConfigRepoConfig(repoMaterial, pluginId, repoId);
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

        ArgumentCaptor<EntityConfigChangedListener> listenerCaptor = ArgumentCaptor.forClass(EntityConfigChangedListener.class);

        verify(goConfigService).register(listenerCaptor.capture());
        EntityConfigChangedListener listner = listenerCaptor.getValue();

        verify(materialUpdateService, never()).updateMaterial(any());

        listner.onEntityConfigChange(configRepo);

        verify(materialUpdateService, times(1)).updateMaterial(any());
    }
}
