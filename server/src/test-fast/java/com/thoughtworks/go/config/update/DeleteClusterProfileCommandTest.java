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
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
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
        config = new BasicCruiseConfig();
        clusterProfile = new ClusterProfile("cluster-id", "plugin-id");
        config.getElasticConfig().getClusterProfiles().add(clusterProfile);
        username = new Username("Bob");
        result = new HttpLocalizedOperationResult();
        command = new DeleteClusterProfileCommand(extension, goConfigService, clusterProfile, username, result);
    }

    @Test
    void shouldSetPreprocessedEntityAsPartOfUpdate() throws Exception {
        assertNull(command.getPreprocessedEntityConfig());
        command.update(config);
        assertThat(command.getPreprocessedEntityConfig()).isEqualTo(clusterProfile);
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
    void shouldSetMessageOnResultIfClusterProfileIsValid() {
        assertThat(result.message()).isEqualTo(null);
        command.isValid(config);
        assertThat(result.message()).isEqualTo("The Cluster Profile 'cluster-id' was deleted successfully.");
    }

    @Test
    void shouldNotAllowDeletionOfClusterProfileWhenReferencedFromElasticAgentProfile() {
        assertThat(result.message()).isEqualTo(null);

        ElasticProfile dependentElasticAgentProfile1 = new ElasticProfile("profile1", clusterProfile.getId());
        ElasticProfile dependentElasticAgentProfile2 = new ElasticProfile("profile2", clusterProfile.getId());
        config.getElasticConfig().getProfiles().add(dependentElasticAgentProfile1);
        config.getElasticConfig().getProfiles().add(dependentElasticAgentProfile2);

        GoConfigInvalidException exception = assertThrows(GoConfigInvalidException.class, () -> command.isValid(config));
        assertThat(exception.getMessage()).isEqualTo("Cannot delete cluster profile 'cluster-id' as it is referenced from elastic agent profile(s) [profile1, profile2]");
    }

    @Test
    void shouldSpecifyClusterProfileObjectDescriptor() {
        assertThat(command.getObjectDescriptor()).isEqualTo(EntityType.ClusterProfile);
    }
}
