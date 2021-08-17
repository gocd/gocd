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
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClusterProfilesServiceTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private EntityHashingService hashingService;
    @Mock
    private ElasticAgentExtension extension;
    @Mock
    private SecretParamResolver secretParamResolver;

    private ClusterProfilesService clusterProfilesService;
    private ClusterProfile clusterProfile;

    @BeforeEach
    void setUp() {

        clusterProfile = new ClusterProfile("prod_cluster", "k8s.ea.plugin");
        clusterProfilesService = new ClusterProfilesService(goConfigService, hashingService, extension, secretParamResolver);
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

        verify(secretParamResolver).resolve(clusterProfile);
        verify(extension, times(1)).validateClusterProfile(clusterProfile.getPluginId(), clusterProfile.getConfigurationAsMap(true));
    }

    @Test
    void shouldNotValidateClusterProfileWhenDeletingClusterProfile() {
        clusterProfilesService.delete(clusterProfile, new Username("Bob"), new HttpLocalizedOperationResult());

        verifyNoInteractions(secretParamResolver);
        verify(extension, never()).validateClusterProfile(clusterProfile.getPluginId(), clusterProfile.getConfigurationAsMap(true));
    }

    @Test
    void shouldValidateClusterProfileUponClusterProfileUpdate() {
        clusterProfilesService.update(clusterProfile, new Username("Bob"), new HttpLocalizedOperationResult());

        verify(secretParamResolver).resolve(clusterProfile);
        verify(extension, times(1)).validateClusterProfile(clusterProfile.getPluginId(), clusterProfile.getConfigurationAsMap(true));
    }

    @Test
    void shouldSendResolvedValueToThePluginWhileValidation() {
        clusterProfile.addNewConfigurationWithValue("key", "{{SECRET:[config_id][key]}}", false);
        clusterProfile.getSecretParams().get(0).setValue("some-resolved-value");
        clusterProfilesService.update(clusterProfile, new Username("Bob"), new HttpLocalizedOperationResult());

        verify(secretParamResolver).resolve(clusterProfile);
        verify(extension, times(1)).validateClusterProfile(clusterProfile.getPluginId(), clusterProfile.getConfigurationAsMap(true, true));
    }

    @Test
    void shouldSetResultAsUnprocessableEntityWhenRulesAreViolated() {
        clusterProfile.addNewConfigurationWithValue("key", "{{SECRET:[config_id][key]}}", false);

        doThrow(new RulesViolationException("some rules violation message")).when(secretParamResolver).resolve(clusterProfile);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        clusterProfilesService.update(clusterProfile, new Username("Bob"), result);

        assertThat(result.httpCode()).isEqualTo(422);
        assertThat(result.message()).isEqualTo("Validations failed for clusterProfile 'prod_cluster'. Error(s): [some rules violation message]. Please correct and resubmit.");
    }
}
