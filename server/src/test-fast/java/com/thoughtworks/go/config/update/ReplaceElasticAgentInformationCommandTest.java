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
import com.thoughtworks.go.config.elastic.*;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.models.ElasticAgentInformation;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.service.ClusterProfilesService;
import com.thoughtworks.go.server.service.ElasticProfileService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class ReplaceElasticAgentInformationCommandTest {
    @Mock
    private PluginService pluginService;
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
    private PluginSettings pluginSettings;
    private ClusterProfiles clusterProfiles;
    private ElasticProfiles elasticProfiles;

    @BeforeEach
    void setUp() {
        initMocks(this);
        basicCruiseConfig = new BasicCruiseConfig();

        pluginId = "plugin-id";

        pluginSettings = new PluginSettings();
        clusterProfiles = new ClusterProfiles();
        clusterProfiles.add(new ClusterProfile("cluster-id", pluginId));
        elasticProfiles = new ElasticProfiles();
        elasticProfiles.add(new ElasticProfile("profile-id", pluginId));

        replaceElasticAgentInformationCommand = new ReplaceElasticAgentInformationCommand(pluginService, clusterProfilesService, elasticProfileService, elasticAgentExtension, goConfigService, pluginDescriptor);

        when(pluginDescriptor.id()).thenReturn(pluginId);
        when(pluginService.getPluginSettings(pluginId)).thenReturn(pluginSettings);
        when(clusterProfilesService.getPluginProfiles()).thenReturn(clusterProfiles);
        when(elasticProfileService.getPluginProfiles()).thenReturn(elasticProfiles);
        when(elasticAgentExtension.migrateConfig(eq(pluginId), any())).thenReturn(new ElasticAgentInformation(Collections.emptyMap(), clusterProfiles, elasticProfiles));
        when(goConfigService.getElasticConfig()).thenReturn(new ElasticConfig());
    }

    @Test
    void shouldMakeCallToElasticAgentExtensionToMigrateElasticAgentRelatedConfig() throws Exception {
        replaceElasticAgentInformationCommand.update(basicCruiseConfig);

        verify(elasticAgentExtension).migrateConfig(pluginId, new ElasticAgentInformation(Collections.emptyMap(), clusterProfiles, elasticProfiles));
    }

    @Test
    void shouldUpdateGoCDConfigWithPluginReturnedMigratedConfig() throws Exception {
        ElasticConfig elasticConfig = new ElasticConfig();
        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);

        assertThat(elasticConfig.getProfiles()).hasSize(0);
        assertThat(elasticConfig.getClusterProfiles()).hasSize(0);

        replaceElasticAgentInformationCommand.update(basicCruiseConfig);

        verify(elasticAgentExtension).migrateConfig(pluginId, new ElasticAgentInformation(Collections.emptyMap(), clusterProfiles, elasticProfiles));

        assertThat(elasticConfig.getProfiles()).hasSize(1);
        assertThat(elasticConfig.getProfiles()).isEqualTo(elasticProfiles);
        assertThat(elasticConfig.getClusterProfiles()).hasSize(1);
        assertThat(elasticConfig.getClusterProfiles()).isEqualTo(clusterProfiles);
    }

    @Test
    void shouldGetPreprocessedConfig() throws Exception {
        replaceElasticAgentInformationCommand.update(basicCruiseConfig);

        ElasticAgentInformation preprocessedEntityConfig = replaceElasticAgentInformationCommand.getPreprocessedEntityConfig();

        assertThat(preprocessedEntityConfig).isEqualTo(new ElasticAgentInformation(Collections.emptyMap(), clusterProfiles, elasticProfiles));
    }

    @Test
    void shouldAlwaysAllowTheReplaceElasticAgentInformationCommandToProceed() {
        assertThat(replaceElasticAgentInformationCommand.canContinue(basicCruiseConfig)).isTrue();
    }
}
