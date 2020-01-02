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

import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ClusterProfilesServiceTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private EntityHashingService hashingService;
    @Mock
    private ElasticAgentExtension extension;

    private ClusterProfilesService clusterProfilesService;
    private ClusterProfile clusterProfile;

    @BeforeEach
    void setUp() {
        initMocks(this);

        clusterProfile = new ClusterProfile("prod_cluster", "k8s.ea.plugin");

        clusterProfilesService = new ClusterProfilesService(goConfigService, hashingService, extension);
    }

    @Test
    void shouldFetchClustersDefinedAsPartOfElasticTag() {
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.setClusterProfiles(new ClusterProfiles(clusterProfile));
        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);

        PluginProfiles<ClusterProfile> actualClusterProfiles = clusterProfilesService.getPluginProfiles();

        assertThat(actualClusterProfiles).isEqualTo(elasticConfig.getClusterProfiles());
    }

    @Test
    void shouldValidateClusterProfileUponClusterProfileCreation() {
        clusterProfilesService.create(clusterProfile, new Username("Bob"), new HttpLocalizedOperationResult());

        verify(extension, times(1)).validateClusterProfile(clusterProfile.getPluginId(), clusterProfile.getConfigurationAsMap(true));
    }

    @Test
    void shouldNotValidateClusterProfileWhenDeletingClusterProfile() {
        clusterProfilesService.delete(clusterProfile, new Username("Bob"), new HttpLocalizedOperationResult());

        verify(extension, never()).validateClusterProfile(clusterProfile.getPluginId(), clusterProfile.getConfigurationAsMap(true));
    }

    @Test
    void shouldValidateClusterProfileUponClusterProfileUpdate() {
        clusterProfilesService.update(clusterProfile, new Username("Bob"), new HttpLocalizedOperationResult());

        verify(extension, times(1)).validateClusterProfile(clusterProfile.getPluginId(), clusterProfile.getConfigurationAsMap(true));
    }
}
