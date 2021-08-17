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

import com.thoughtworks.go.config.update.ReplaceElasticAgentInformationCommand;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.v4.ElasticAgentExtensionV4;
import com.thoughtworks.go.plugin.access.elastic.v5.ElasticAgentExtensionV5;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.PluginPostLoadHook;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticAgentInformationMigratorImplTest {
    @Mock
    private PluginSqlMapDao pluginSqlMapDao;
    @Mock
    private ClusterProfilesService clusterProfilesService;
    @Mock
    private ElasticProfileService elasticProfileService;
    @Mock
    private ElasticAgentExtension elasticAgentExtension;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private GoConfigService goConfigService;

    private ElasticAgentInformationMigratorImpl elasticAgentInformationMigrator;
    private GoPluginDescriptor goPluginDescriptor;
    private String PLUGIN_ID = "plugin-id";

    @BeforeEach
    void setUp() {
        goPluginDescriptor = GoPluginDescriptor.builder().id(PLUGIN_ID).build();
        goPluginDescriptor.setBundleDescriptor(new GoPluginBundleDescriptor(goPluginDescriptor));
        elasticAgentInformationMigrator = new ElasticAgentInformationMigratorImpl(pluginSqlMapDao, clusterProfilesService, elasticProfileService, elasticAgentExtension, pluginManager, goConfigService);
    }

    @Test
    void shouldDoNothingForNonElasticAgentPlugins() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(false);

        PluginPostLoadHook.Result result = elasticAgentInformationMigrator.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isFalse();
        verifyNoInteractions(goConfigService);
    }

    @Test
    void shouldPerformReplaceElasticAgentInformationCommandToMigrateElasticAgentInformation() {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("k1", "v1");
        configuration.put("k2", "v2");

        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(true);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, JsonHelper.toJsonString(configuration)));
        when(pluginManager.resolveExtensionVersion(goPluginDescriptor.id(), ELASTIC_AGENT_EXTENSION, ElasticAgentExtension.SUPPORTED_VERSIONS)).thenReturn(ElasticAgentExtensionV5.VERSION);

        PluginPostLoadHook.Result result = elasticAgentInformationMigrator.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isFalse();
        verify(goConfigService, times(1)).updateConfig(any(ReplaceElasticAgentInformationCommand.class));
    }

    @Test
    void shouldPerformReplaceElasticAgentInformationCommandToMigrateElasticAgentInformationWhenNoPluginSettingsAreConfigured() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(true);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, null));
        when(pluginManager.resolveExtensionVersion(goPluginDescriptor.id(), ELASTIC_AGENT_EXTENSION, ElasticAgentExtension.SUPPORTED_VERSIONS)).thenReturn(ElasticAgentExtensionV5.VERSION);

        PluginPostLoadHook.Result result = elasticAgentInformationMigrator.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Success");
        verify(goConfigService, times(1)).updateConfig(any(ReplaceElasticAgentInformationCommand.class));
    }

    @Test
    void shouldMarkPluginDescriptorInvalidIncaseOfErrors() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(true);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, null));
        when(goConfigService.updateConfig(any())).thenThrow(new RuntimeException("Boom!"));

        assertThat(goPluginDescriptor.isInvalid()).isFalse();

        String expectedErrorMessage = "Plugin 'plugin-id' failed to perform 'cd.go.elastic-agent.migrate-config' call. Plugin sent an invalid config. Reason: Boom!.\n Please fix the errors and restart GoCD server.";

        PluginPostLoadHook.Result result = elasticAgentInformationMigrator.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isTrue();
        assertThat(result.getMessage()).isEqualTo(expectedErrorMessage);
        assertThat(goPluginDescriptor.isInvalid()).isTrue();
        assertThat(goPluginDescriptor.getStatus().getMessages().get(0)).isEqualTo(expectedErrorMessage);
    }

    @Test
    void shouldDeletePluginSettingsOnASuccessfulMigrateConfigCall() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(true);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, null));
        when(pluginManager.resolveExtensionVersion(goPluginDescriptor.id(), ELASTIC_AGENT_EXTENSION, ElasticAgentExtension.SUPPORTED_VERSIONS)).thenReturn(ElasticAgentExtensionV5.VERSION);

        PluginPostLoadHook.Result result = elasticAgentInformationMigrator.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Success");
        verify(goConfigService, times(1)).updateConfig(any(ReplaceElasticAgentInformationCommand.class));

        verify(pluginSqlMapDao, times(1)).deletePluginIfExists(goPluginDescriptor.id());
    }

    @Test
    void shouldNotDeletePluginSettingsOnWhenMigrateConfigCallFails() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(true);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, null));
        when(goConfigService.updateConfig(any())).thenThrow(new RuntimeException("Boom!"));

        assertThat(goPluginDescriptor.isInvalid()).isFalse();

        String expectedErrorMessage = "Plugin 'plugin-id' failed to perform 'cd.go.elastic-agent.migrate-config' call. Plugin sent an invalid config. Reason: Boom!.\n Please fix the errors and restart GoCD server.";

        PluginPostLoadHook.Result result = elasticAgentInformationMigrator.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isTrue();
        assertThat(result.getMessage()).isEqualTo(expectedErrorMessage);
        assertThat(goPluginDescriptor.isInvalid()).isTrue();
        assertThat(goPluginDescriptor.getStatus().getMessages().get(0)).isEqualTo(expectedErrorMessage);

        verify(goConfigService, times(1)).updateConfig(any(ReplaceElasticAgentInformationCommand.class));
        verify(pluginSqlMapDao, never()).deletePluginIfExists(goPluginDescriptor.id());
    }

    @Test
    void shouldNotDeletePluginSettingsOnASuccessfulMigrateConfigCallWhenPluginIsUsingElasticAgentExtensionV4() {
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, goPluginDescriptor.id())).thenReturn(true);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, null));
        when(pluginManager.resolveExtensionVersion(goPluginDescriptor.id(), ELASTIC_AGENT_EXTENSION, ElasticAgentExtension.SUPPORTED_VERSIONS)).thenReturn(ElasticAgentExtensionV4.VERSION);

        PluginPostLoadHook.Result result = elasticAgentInformationMigrator.run(goPluginDescriptor, new HashMap<>());

        assertThat(result.isAFailure()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Success");
        verify(goConfigService, times(1)).updateConfig(any(ReplaceElasticAgentInformationCommand.class));

        verify(pluginSqlMapDao, never()).deletePluginIfExists(goPluginDescriptor.id());
    }
}
