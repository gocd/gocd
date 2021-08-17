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
import com.thoughtworks.go.config.elastic.*;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.models.ElasticAgentInformation;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.ClusterProfilesService;
import com.thoughtworks.go.server.service.ElasticProfileService;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplaceElasticAgentInformationCommandTest {
    @Mock
    private ClusterProfilesService clusterProfilesService;
    @Mock
    private ElasticProfileService elasticProfileService;
    @Mock
    private ElasticAgentExtension elasticAgentExtension;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private GoPluginDescriptor pluginDescriptor;

    private ReplaceElasticAgentInformationCommand replaceElasticAgentInformationCommand;
    private BasicCruiseConfig basicCruiseConfig;
    private String pluginId;
    private HashMap<String, String> pluginSettings;
    private ClusterProfiles clusterProfiles;
    private ElasticProfiles elasticProfiles;

    @BeforeEach
    void setUp() {
        basicCruiseConfig = new BasicCruiseConfig();

        pluginId = "plugin-id";

        pluginSettings = new HashMap<>();
        clusterProfiles = new ClusterProfiles();
        clusterProfiles.add(new ClusterProfile("prod-cluster", pluginId));
        elasticProfiles = new ElasticProfiles();
        elasticProfiles.add(new ElasticProfile("profile-id", "prod-cluster"));

        replaceElasticAgentInformationCommand = new ReplaceElasticAgentInformationCommand(clusterProfilesService, elasticProfileService, elasticAgentExtension, pluginDescriptor, pluginSettings);

        when(pluginDescriptor.id()).thenReturn(pluginId);
        when(clusterProfilesService.getPluginProfiles()).thenReturn(clusterProfiles);
        when(elasticProfileService.findElasticAgentProfilesByPluginId(pluginId)).thenReturn(elasticProfiles);
        when(elasticAgentExtension.migrateConfig(eq(pluginId), any())).thenReturn(new ElasticAgentInformation(Collections.emptyMap(), clusterProfiles, elasticProfiles));
        lenient().when(goConfigService.getElasticConfig()).thenReturn(new ElasticConfig());
    }

    @Test
    void shouldMakeCallToElasticAgentExtensionToMigrateElasticAgentRelatedConfig() throws Exception {
        replaceElasticAgentInformationCommand.update(basicCruiseConfig);

        verify(elasticAgentExtension).migrateConfig(pluginId, new ElasticAgentInformation(Collections.emptyMap(), clusterProfiles, elasticProfiles));
    }

    @Test
    void shouldMakeCallToElasticAgentExtensionToMigrateElasticAgentRelatedConfig_WhenNoPluginSettingsAreConfigured() throws Exception {
        replaceElasticAgentInformationCommand = new ReplaceElasticAgentInformationCommand(clusterProfilesService, elasticProfileService, elasticAgentExtension, pluginDescriptor, new HashMap<>());

        replaceElasticAgentInformationCommand.update(basicCruiseConfig);

        verify(elasticAgentExtension).migrateConfig(pluginId, new ElasticAgentInformation(Collections.emptyMap(), clusterProfiles, elasticProfiles));
    }

    @Test
    void shouldUpdateGoCDConfigWithPluginReturnedMigratedConfig() throws Exception {
        ElasticConfig elasticConfig = new ElasticConfig();

        assertThat(elasticConfig.getProfiles()).hasSize(0);
        assertThat(elasticConfig.getClusterProfiles()).hasSize(0);

        CruiseConfig basicCruiseConfig = replaceElasticAgentInformationCommand.update(this.basicCruiseConfig);

        verify(elasticAgentExtension).migrateConfig(pluginId, new ElasticAgentInformation(Collections.emptyMap(), clusterProfiles, elasticProfiles));

        assertThat(basicCruiseConfig.getElasticConfig().getProfiles()).hasSize(1);
        assertThat(basicCruiseConfig.getElasticConfig().getProfiles()).isEqualTo(elasticProfiles);
        assertThat(basicCruiseConfig.getElasticConfig().getClusterProfiles()).hasSize(1);
        assertThat(basicCruiseConfig.getElasticConfig().getClusterProfiles()).isEqualTo(clusterProfiles);
    }
}
