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

import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension.SUPPORTED_VERSIONS;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ElasticAgentPluginInfoBuilderTest {
    @Mock(lenient = true)
    private ElasticAgentExtension extension;

    @Mock(lenient = true)
    private PluginManager pluginManager;


    @Test
    public void shouldBuildPluginInfoWithProfileSettings() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        List<PluginConfiguration> pluginConfigurations = List.of(new PluginConfiguration("aws_password", new Metadata(true, false)));
        PluginSettingsProperty property = new PluginSettingsProperty("ami-id", "ami-123");
        PluginSettingsConfiguration pluginSettingsConfiguration = new PluginSettingsConfiguration();
        pluginSettingsConfiguration.add(property);
        Image icon = new Image("content_type", "data", "hash");

        when(pluginManager.resolveExtensionVersion("plugin1", ELASTIC_AGENT_EXTENSION, SUPPORTED_VERSIONS)).thenReturn("1.0");
        when(extension.getPluginSettingsConfiguration(descriptor.id())).thenReturn(pluginSettingsConfiguration);
        when(extension.getPluginSettingsView(descriptor.id())).thenReturn("some html");

        when(extension.getIcon(descriptor.id())).thenReturn(icon);

        when(extension.getProfileMetadata(descriptor.id())).thenReturn(pluginConfigurations);
        when(extension.getProfileView(descriptor.id())).thenReturn("profile_view");

        ElasticAgentPluginInfoBuilder builder = new ElasticAgentPluginInfoBuilder(extension);
        ElasticAgentPluginInfo pluginInfo = builder.pluginInfoFor(descriptor);

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("elastic-agent"));

        assertThat(pluginInfo.getImage(), is(icon));
        assertThat(pluginInfo.getElasticAgentProfileSettings(), is(new PluggableInstanceSettings(pluginConfigurations, new PluginView("profile_view"))));
        assertThat(pluginInfo.getPluginSettings(), is(new PluggableInstanceSettings(builder.configurations(pluginSettingsConfiguration), new PluginView("some html"))));
        assertFalse(pluginInfo.supportsStatusReport());
    }

    @Test
    public void shouldBuildPluginInfoWithClusterProfileSettings() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        List<PluginConfiguration> elasticAgentProfileConfigurations = Arrays.asList(new PluginConfiguration("aws_password", new Metadata(true, false)));
        List<PluginConfiguration> clusterProfileConfigurations = Arrays.asList(new PluginConfiguration("aws_url", new Metadata(true, false)));
        PluginSettingsProperty property = new PluginSettingsProperty("ami-id", "ami-123");
        PluginSettingsConfiguration pluginSettingsConfiguration = new PluginSettingsConfiguration();
        pluginSettingsConfiguration.add(property);
        Image icon = new Image("content_type", "data", "hash");

        when(pluginManager.resolveExtensionVersion("plugin1", ELASTIC_AGENT_EXTENSION, SUPPORTED_VERSIONS)).thenReturn("1.0");
        when(extension.getPluginSettingsConfiguration(descriptor.id())).thenReturn(pluginSettingsConfiguration);
        when(extension.getPluginSettingsView(descriptor.id())).thenReturn("some html");

        when(extension.getIcon(descriptor.id())).thenReturn(icon);

        when(extension.getClusterProfileMetadata(descriptor.id())).thenReturn(clusterProfileConfigurations);
        when(extension.getClusterProfileView(descriptor.id())).thenReturn("cluster_profile_view");

        when(extension.getProfileMetadata(descriptor.id())).thenReturn(elasticAgentProfileConfigurations);
        when(extension.getProfileView(descriptor.id())).thenReturn("elastic_agent_profile_view");

        when(extension.supportsClusterProfiles("plugin1")).thenReturn(true);

        ElasticAgentPluginInfoBuilder builder = new ElasticAgentPluginInfoBuilder(extension);
        ElasticAgentPluginInfo pluginInfo = builder.pluginInfoFor(descriptor);

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("elastic-agent"));

        assertThat(pluginInfo.getImage(), is(icon));
        assertThat(pluginInfo.getElasticAgentProfileSettings(), is(new PluggableInstanceSettings(elasticAgentProfileConfigurations, new PluginView("elastic_agent_profile_view"))));
        assertThat(pluginInfo.getClusterProfileSettings(), is(new PluggableInstanceSettings(clusterProfileConfigurations, new PluginView("cluster_profile_view"))));
        assertNull(pluginInfo.getPluginSettings());
        assertFalse(pluginInfo.supportsStatusReport());
    }

    @Test
    public void shouldBuildPluginInfoWithoutClusterProfileSettingsForPluginsImplementedUsingv4Extension() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        List<PluginConfiguration> elasticAgentProfileConfigurations = Arrays.asList(new PluginConfiguration("aws_password", new Metadata(true, false)));
        PluginSettingsProperty property = new PluginSettingsProperty("ami-id", "ami-123");
        PluginSettingsConfiguration pluginSettingsConfiguration = new PluginSettingsConfiguration();
        pluginSettingsConfiguration.add(property);
        Image icon = new Image("content_type", "data", "hash");

        when(pluginManager.resolveExtensionVersion("plugin1", ELASTIC_AGENT_EXTENSION, SUPPORTED_VERSIONS)).thenReturn("1.0");
        when(extension.getPluginSettingsConfiguration(descriptor.id())).thenReturn(pluginSettingsConfiguration);
        when(extension.getPluginSettingsView(descriptor.id())).thenReturn("some html");

        when(extension.getIcon(descriptor.id())).thenReturn(icon);

        when(extension.getProfileMetadata(descriptor.id())).thenReturn(elasticAgentProfileConfigurations);
        when(extension.getProfileView(descriptor.id())).thenReturn("elastic_agent_profile_view");

        when(extension.supportsClusterProfiles("plugin1")).thenReturn(false);

        ElasticAgentPluginInfoBuilder builder = new ElasticAgentPluginInfoBuilder(extension);
        ElasticAgentPluginInfo pluginInfo = builder.pluginInfoFor(descriptor);

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("elastic-agent"));

        assertThat(pluginInfo.getImage(), is(icon));
        assertThat(pluginInfo.getElasticAgentProfileSettings(), is(new PluggableInstanceSettings(elasticAgentProfileConfigurations, new PluginView("elastic_agent_profile_view"))));
        assertThat(pluginInfo.getClusterProfileSettings(), is(new PluggableInstanceSettings(null, null)));
        assertThat(pluginInfo.getPluginSettings(), is(new PluggableInstanceSettings(builder.configurations(pluginSettingsConfiguration), new PluginView("some html"))));
        assertFalse(pluginInfo.supportsStatusReport());

        verify(extension, never()).getClusterProfileMetadata(any());
        verify(extension, never()).getClusterProfileView(any());
    }

    @Test
    public void shouldContinueWithBuildingPluginInfoIfPluginSettingsIsNotProvidedByThePlugin() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();
        List<PluginConfiguration> pluginConfigurations = Arrays.asList(new PluginConfiguration("aws_password", new Metadata(true, false)));

        Image icon = new Image("content_type", "data", "hash");

        doThrow(new RuntimeException("foo")).when(extension).getPluginSettingsConfiguration(descriptor.id());
        when(pluginManager.resolveExtensionVersion("plugin1", ELASTIC_AGENT_EXTENSION, SUPPORTED_VERSIONS)).thenReturn("1.0");
        when(extension.getIcon(descriptor.id())).thenReturn(icon);

        when(extension.getProfileMetadata(descriptor.id())).thenReturn(pluginConfigurations);
        when(extension.getProfileView(descriptor.id())).thenReturn("profile_view");

        ElasticAgentPluginInfoBuilder builder = new ElasticAgentPluginInfoBuilder(extension);
        ElasticAgentPluginInfo pluginInfo = builder.pluginInfoFor(descriptor);

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("elastic-agent"));

        assertThat(pluginInfo.getImage(), is(icon));
        assertThat(pluginInfo.getElasticAgentProfileSettings(), is(new PluggableInstanceSettings(pluginConfigurations, new PluginView("profile_view"))));
        assertNull(pluginInfo.getPluginSettings());
    }

    @Test
    public void shouldGetCapabilitiesForAPlugin() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();

        when(pluginManager.resolveExtensionVersion("plugin1", ELASTIC_AGENT_EXTENSION, SUPPORTED_VERSIONS)).thenReturn("2.0");
        Capabilities capabilities = new Capabilities(true);
        when(extension.getCapabilities(descriptor.id())).thenReturn(capabilities);

        ElasticAgentPluginInfo pluginInfo = new ElasticAgentPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getCapabilities(), is(capabilities));
    }

    @Test
    public void shouldFetchPluginSettingsForPluginsNotSupportingClusterProfiles() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();

        PluginSettingsConfiguration pluginSettingsConfiguration = new PluginSettingsConfiguration();
        pluginSettingsConfiguration.add(new PluginSettingsProperty("ami-id", "ami-123"));

        when(extension.getPluginSettingsConfiguration(descriptor.id())).thenReturn(pluginSettingsConfiguration);
        when(extension.getPluginSettingsView(descriptor.id())).thenReturn("some html");

        when(extension.supportsClusterProfiles("plugin1")).thenReturn(false);

        ElasticAgentPluginInfoBuilder builder = new ElasticAgentPluginInfoBuilder(extension);
        ElasticAgentPluginInfo pluginInfo = builder.pluginInfoFor(descriptor);

        assertThat(pluginInfo.getPluginSettings(), is(new PluggableInstanceSettings(builder.configurations(pluginSettingsConfiguration), new PluginView("some html"))));
    }

    @Test
    public void shouldNotFetchPluginSettingsForPluginsSupportingClusterProfiles() {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();

        when(extension.supportsClusterProfiles("plugin1")).thenReturn(true);

        ElasticAgentPluginInfoBuilder builder = new ElasticAgentPluginInfoBuilder(extension);
        ElasticAgentPluginInfo pluginInfo = builder.pluginInfoFor(descriptor);

        assertNull(pluginInfo.getPluginSettings());

        verify(extension, never()).getPluginSettingsConfiguration(descriptor.id());
        verify(extension, never()).getPluginSettingsView(descriptor.id());
    }
}
