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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class AddClusterProfileCommandTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private ElasticAgentExtension extension;
    private ClusterProfile clusterProfile;
    private Username username;
    private HttpLocalizedOperationResult result;

    AddClusterProfileCommand command;
    private CruiseConfig config;

    @BeforeEach
    void setUp() {
        initMocks(this);
        config = new BasicCruiseConfig();
        clusterProfile = new ClusterProfile("cluster-id", "plugin-id");
        username = new Username("Bob");
        result = new HttpLocalizedOperationResult();
        command = new AddClusterProfileCommand(extension, goConfigService, clusterProfile, username, result);
    }

    @Test
    void shouldAddClusterProfile() throws Exception {
        assertThat(config.getElasticConfig().getClusterProfiles()).hasSize(0);
        command.update(config);

        assertThat(config.getElasticConfig().getClusterProfiles()).hasSize(1);
        assertThat(config.getElasticConfig().getClusterProfiles().get(0)).isEqualTo(clusterProfile);
    }

    @Test
    void shouldReturnWhetherAdditionOfNewClusterProfileIsValid() throws Exception {
        command.update(config);
        assertThat(command.isValid(config)).isTrue();
    }

    @Test
    void shouldSpecifyClusterProfileObjectDescriptor() {
        assertThat(command.getObjectDescriptor()).isEqualTo(EntityType.ClusterProfile);
    }

    @Test
    void shouldMakeACallToExtensionToValidateClusterProfile() {
        String pluginId = "plugin-id";
        HashMap<String, String> configuration = new HashMap<>();
        command.validateUsingExtension(pluginId, configuration);

        verify(extension, times(1)).validateClusterProfile(pluginId, configuration);
    }
}
