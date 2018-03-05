/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElasticAgentPluginRegistryTest {
    private static final String PLUGIN_ID = "cd.go.example.plugin";
    @Mock
    private PluginManager pluginManager;
    @Mock
    private ElasticAgentExtension elasticAgentExtension;
    @Mock
    private GoPluginDescriptor pluginDescriptor;

    private ElasticAgentPluginRegistry elasticAgentPluginRegistry;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        elasticAgentPluginRegistry = new ElasticAgentPluginRegistry(pluginManager, elasticAgentExtension);

        when(elasticAgentExtension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);

        elasticAgentPluginRegistry.pluginLoaded(pluginDescriptor);
        verify(elasticAgentExtension, times(1)).canHandlePlugin(PLUGIN_ID);
    }

    @Test
    public void shouldTalkToExtensionToCreateElasticAgent() {
        final Map<String, String> configuration = Collections.singletonMap("GoServerURL", "foo");
        final JobIdentifier jobIdentifier = new JobIdentifier();
        final String autoRegisterKey = "auto-register-key";
        final String environment = "test-env";

        elasticAgentPluginRegistry.createAgent(PLUGIN_ID, autoRegisterKey, environment, configuration, jobIdentifier);

        verify(elasticAgentExtension, times(1)).createAgent(PLUGIN_ID, autoRegisterKey, environment, configuration, jobIdentifier);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToExecuteServerPingCall() {
        elasticAgentPluginRegistry.serverPing(PLUGIN_ID);

        verify(elasticAgentExtension, times(1)).serverPing(PLUGIN_ID);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToExecuteShouldAssignWorkCall() {
        final String environment = "test-env";
        final JobIdentifier jobIdentifier = new JobIdentifier();
        final Map<String, String> configuration = Collections.singletonMap("GoServerURL", "foo");
        final AgentMetadata agentMetadata = new AgentMetadata("som-id", "Idle", "Idle", "Enabled");

        elasticAgentPluginRegistry.shouldAssignWork(pluginDescriptor, agentMetadata, environment, configuration, jobIdentifier);

        verify(elasticAgentExtension, times(1)).shouldAssignWork(PLUGIN_ID, agentMetadata, environment, configuration, jobIdentifier);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToGetPluginStatusReport() {
        elasticAgentPluginRegistry.getPluginStatusReport(PLUGIN_ID);

        verify(elasticAgentExtension, times(1)).getPluginStatusReport(PLUGIN_ID);
        verifyNoMoreInteractions(elasticAgentExtension);
    }

    @Test
    public void shouldTalkToExtensionToGetAgentStatusReport() {
        final JobIdentifier jobIdentifier = new JobIdentifier();

        elasticAgentPluginRegistry.getAgentStatusReport(PLUGIN_ID, jobIdentifier, "some-id");

        verify(elasticAgentExtension, times(1)).getAgentStatusReport(PLUGIN_ID, jobIdentifier, "some-id");
        verifyNoMoreInteractions(elasticAgentExtension);
    }
}