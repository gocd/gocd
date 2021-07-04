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

import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.update.AddClusterProfileCommand;
import com.thoughtworks.go.plugin.infra.GoPluginFrameworkException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

@ExtendWith(MockitoExtension.class)
class PluginProfilesServiceTest {

    @Mock
    private GoConfigService goConfigService;

    @Mock
    private EntityHashingService hashingService;

    @Mock
    private AddClusterProfileCommand command;

    private PluginProfilesService service;

    @BeforeEach
    void setUp() {
        service = new PluginProfilesService(goConfigService, hashingService) {
            @Override
            protected PluginProfiles getPluginProfiles() {
                return null;
            }
        };
    }

    @Test
    void shouldAddExceptionsThrownByPluginInfraErrorOnPluginProfile() {
        ClusterProfile pluginProfile = new ClusterProfile();
        String errorMessage = "Plugin is missing!";
        Mockito.when(command.validateUsingExtension(any(), anyMap())).thenThrow(new GoPluginFrameworkException(errorMessage));

        assertThat(pluginProfile.hasErrors()).isFalse();

        service.validatePluginProperties(command, pluginProfile);

        assertThat(pluginProfile.hasErrors()).isTrue();
        assertThat(pluginProfile.errors().get("pluginId")).isEqualTo(Arrays.asList(errorMessage));
    }

    @Test
    void shouldAddNotFoundExceptionsThrownOnPluginProfile() {
        ClusterProfile pluginProfile = new ClusterProfile("foo", "plugin-id");
        String errorMessage = "Plugin with id `plugin-id` is not found.";
        Mockito.when(command.validateUsingExtension(any(), anyMap())).thenThrow(new RecordNotFoundException("Boom!"));

        assertThat(pluginProfile.hasErrors()).isFalse();

        service.validatePluginProperties(command, pluginProfile);

        assertThat(pluginProfile.hasErrors()).isTrue();
        assertThat(pluginProfile.errors().get("pluginId")).isEqualTo(Arrays.asList(errorMessage));
    }
}
