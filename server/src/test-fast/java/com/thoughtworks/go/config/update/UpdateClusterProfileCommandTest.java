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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class UpdateClusterProfileCommandTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private ElasticAgentExtension extension;

    private ClusterProfile clusterProfile;
    private Username username;
    private HttpLocalizedOperationResult result;

    UpdateClusterProfileCommand command;
    private CruiseConfig config;

    @BeforeEach
    void setUp() {
        initMocks(this);
        config = new BasicCruiseConfig();
        clusterProfile = new ClusterProfile("cluster-id", "plugin-id");
        config.getElasticConfig().getClusterProfiles().add(clusterProfile);
        username = new Username("Bob");
        result = new HttpLocalizedOperationResult();
        command = new UpdateClusterProfileCommand(extension, goConfigService, clusterProfile, username, result);
    }

    @Test
    void shouldUpdateClusterProfile() throws Exception {
        assertThat(config.getElasticConfig().getClusterProfiles().get(0)).isEqualTo(clusterProfile);
        command.update(config);
        assertThat(config.getElasticConfig().getClusterProfiles().get(0)).isEqualTo(clusterProfile);
    }

    @Test
    void shouldReturnWhetherRemovalOfExistingClusterProfileIsValid() throws Exception {
        assertThat(command.isValid(config)).isTrue();
        command.update(config);
        assertThat(command.isValid(config)).isTrue();
    }

    @Test
    void shouldSpecifyClusterProfileObjectDescriptor() {
        assertThat(command.getObjectDescriptor()).isEqualTo(EntityType.ClusterProfile);
    }

    @Test
    void shouldValidateElasticAgentProfilesAsPartOfUpdateClusterProfile() {
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.getClusterProfiles().add(new ClusterProfile("cluster1", "ecs"));
        elasticConfig.getProfiles().add(new ElasticProfile("profile1", "cluster1"));
        config.setElasticConfig(elasticConfig);

        RecordNotFoundException exception = assertThrows(RecordNotFoundException.class, () -> command.isValid(config));
        assertThat(exception.getMessage()).isEqualTo("Cluster profile with id 'cluster-id' was not found!");
    }

    @Test
    void shouldMakeACallToExtensionToValidateClusterProfile() {
        String pluginId = "plugin-id";
        HashMap<String, String> configuration = new HashMap<>();
        command.validateUsingExtension(pluginId, configuration);

        verify(extension, times(1)).validateClusterProfile(pluginId, configuration);
    }
}
