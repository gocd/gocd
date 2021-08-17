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
package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.domain.ClusterProfilesChangedStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ElasticAgentPluginRegistryTest {
    private static final String PLUGIN_ID = "cd.go.example.plugin";
    @Mock
    private PluginManager pluginManager;
    @Mock
    private ElasticAgentExtension elasticAgentExtension;
    @Mock
    private GoPluginDescriptor pluginDescriptor;

    private ElasticAgentPluginRegistry elasticAgentPluginRegistry;

    @BeforeEach
    public void setUp() throws Exception {
        elasticAgentPluginRegistry = new ElasticAgentPluginRegistry(pluginManager, elasticAgentExtension);

        when(elasticAgentExtension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);

        elasticAgentPluginRegistry.pluginLoaded(pluginDescriptor);
        verify(elasticAgentExtension, times(1)).canHandlePlugin(PLUGIN_ID);
    }

    @Test
    public void shouldTalkToExtensionToCreateElasticAgent() {
        final Map<String, String> configuration = Collections.singletonMap("GoServerURL", "foo");
        final Map<String, String> clusterConfiguration = Collections.singletonMap("GoServerURL", "foo");
        final JobIdentifier jobIdentifier = new JobIdentifier();
        final String autoRegisterKey = "auto-register-key";
        final String environment = "test-env";

        elasticAgentPluginRegistry.createAgent(PLUGIN_ID, autoRegisterKey, environment, configuration, clusterConfiguration, jobIdentifier);

        verify(elasticAgentExtension, times(1)).createAgent(PLUGIN_ID, autoRegisterKey, environment, configuration, clusterConfiguration, jobIdentifier);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToExecuteServerPingCall() {
        final Map<String, String> clusterProfileProperties = Collections.singletonMap("GoServerURL", "foo");
        elasticAgentPluginRegistry.serverPing(PLUGIN_ID, Arrays.asList(clusterProfileProperties));

        verify(elasticAgentExtension, times(1)).serverPing(PLUGIN_ID, Arrays.asList(clusterProfileProperties));
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToExecuteShouldAssignWorkCall() {
        final String environment = "test-env";
        final JobIdentifier jobIdentifier = new JobIdentifier();
        final Map<String, String> configuration = Collections.singletonMap("Image", "alpine:latest");
        final Map<String, String> clusterProfileProperties = Collections.singletonMap("GoServerURL", "foo");
        final AgentMetadata agentMetadata = new AgentMetadata("som-id", "Idle", "Idle", "Enabled");

        elasticAgentPluginRegistry.shouldAssignWork(pluginDescriptor, agentMetadata, environment, configuration, clusterProfileProperties, jobIdentifier);
        verify(elasticAgentExtension, times(1)).shouldAssignWork(PLUGIN_ID, agentMetadata, environment, configuration, clusterProfileProperties, jobIdentifier);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToGetPluginStatusReport() {
        List<Map<String, String>> clusterProfiles = Collections.emptyList();
        elasticAgentPluginRegistry.getPluginStatusReport(PLUGIN_ID, clusterProfiles);

        verify(elasticAgentExtension, times(1)).getPluginStatusReport(PLUGIN_ID, clusterProfiles);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToGetAgentStatusReport() {
        final JobIdentifier jobIdentifier = new JobIdentifier();

        elasticAgentPluginRegistry.getAgentStatusReport(PLUGIN_ID, jobIdentifier, "some-id", null);

        verify(elasticAgentExtension, times(1)).getAgentStatusReport(PLUGIN_ID, jobIdentifier, "some-id", null);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToGetClusterStatusReport() {
        Map<String, String> clusterProfileConfigurations = Collections.emptyMap();
        elasticAgentPluginRegistry.getClusterStatusReport(PLUGIN_ID, clusterProfileConfigurations);

        verify(elasticAgentExtension, times(1)).getClusterStatusReport(PLUGIN_ID, clusterProfileConfigurations);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToReportJobCompletion() {
        final JobIdentifier jobIdentifier = new JobIdentifier();
        final String elasticAgentId = "ea_1";
        final Map<String, String> elasticProfileConfiguration = Collections.singletonMap("Image", "alpine:latest");
        final Map<String, String> clusterProfileConfiguration = Collections.singletonMap("ServerURL", "https://example.com/go");

        elasticAgentPluginRegistry.reportJobCompletion(PLUGIN_ID, elasticAgentId, jobIdentifier, elasticProfileConfiguration, clusterProfileConfiguration);

        verify(elasticAgentExtension, times(1)).reportJobCompletion(PLUGIN_ID, elasticAgentId, jobIdentifier, elasticProfileConfiguration, clusterProfileConfiguration);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToNotifyClusterProfileHasChanged() {
        final Map<String, String> newClusterProfileConfigurations = Collections.singletonMap("Image", "alpine:latest");
        elasticAgentPluginRegistry.notifyPluginAboutClusterProfileChanged(PLUGIN_ID, ClusterProfilesChangedStatus.CREATED, null, newClusterProfileConfigurations);

        verify(elasticAgentExtension, times(1)).clusterProfileChanged(PLUGIN_ID, ClusterProfilesChangedStatus.CREATED, null, newClusterProfileConfigurations);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldNotFailEvenWhenExtensionFailsToHandleClusterProfileChangedCall() {
        final Map<String, String> newClusterProfileConfigurations = Collections.singletonMap("Image", "alpine:latest");
        doThrow(new RuntimeException("Boom!")).when(elasticAgentExtension).clusterProfileChanged(any(), any(), any(), any());

        elasticAgentPluginRegistry.notifyPluginAboutClusterProfileChanged(PLUGIN_ID, ClusterProfilesChangedStatus.CREATED, null, newClusterProfileConfigurations);

        verify(elasticAgentExtension, times(1)).clusterProfileChanged(PLUGIN_ID, ClusterProfilesChangedStatus.CREATED, null, newClusterProfileConfigurations);
        verifyNoMoreInteractions(elasticAgentExtension);
    }
}
