/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class DeleteClusterProfileCommandTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private ElasticAgentExtension extension;
    private ClusterProfile clusterProfile;
    private Username username;
    private HttpLocalizedOperationResult result;

    DeleteClusterProfileCommand command;
    private CruiseConfig config;

    @BeforeEach
    void setUp() {
        initMocks(this);
        config = new BasicCruiseConfig();
        clusterProfile = new ClusterProfile("cluster-id", "plugin-id");
        config.getElasticConfig().getClusterProfiles().add(clusterProfile);
        username = new Username("Bob");
        result = new HttpLocalizedOperationResult();
        command = new DeleteClusterProfileCommand(extension, goConfigService, clusterProfile, username, result);
    }

    @Test
    void shouldAllowAdministratorToDeleteClusterProfile() {
        when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);

        boolean canContinue = command.canContinue(config);
        assertThat(canContinue).isTrue();
    }

    @Test
    void shouldNotAllowNonAdministratorToDeleteClusterProfile() {
        when(goConfigService.isAdministrator(username.getUsername())).thenReturn(false);

        boolean canContinue = command.canContinue(config);
        assertThat(canContinue).isFalse();
    }

    @Test
    void shouldDeleteClusterProfile() throws Exception {
        assertThat(config.getElasticConfig().getClusterProfiles()).hasSize(1);
        command.update(config);

        assertThat(config.getElasticConfig().getClusterProfiles()).hasSize(0);
    }

    @Test
    void shouldReturnWhetherRemovalOfExistingClusterProfileIsValid() throws Exception {
        assertThat(command.isValid(config)).isTrue();
        command.update(config);
        assertThat(command.isValid(config)).isTrue();
    }

    @Test
    void shouldSetMessageOnResultIfClusterProfileIsValid() throws Exception {
        assertThat(result.message()).isEqualTo(null);
        command.isValid(config);
        assertThat(result.message()).isEqualTo("The Cluster Profile 'cluster-id' was deleted successfully.");
    }

    @Test
    void shouldSpecifyClusterProfileObjectDescriptor() {
        assertThat(command.getObjectDescriptor()).isEqualTo("Cluster Profile");
    }
}
