/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginConstants.SUPPORTED_VERSIONS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ElasticAgentPluginInfoBuilderTest {
    private ElasticAgentExtension extension;
    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        extension = mock(ElasticAgentExtension.class);
        pluginManager = mock(PluginManager.class);
    }

    @Test
    public void shouldBuildPluginInfoWithProfileSettings() throws Exception {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);
        List<PluginConfiguration> pluginConfigurations = Arrays.asList(new PluginConfiguration("aws_password", new Metadata(true, false)));
        PluginSettingsProperty property = new PluginSettingsProperty("ami-id", "ami-123");
        PluginSettingsConfiguration pluginSettingsConfiguration = new PluginSettingsConfiguration();
        pluginSettingsConfiguration.add(property);
        Image icon = new Image("content_type", "data", "hash");

        when(pluginManager.resolveExtensionVersion("plugin1", SUPPORTED_VERSIONS)).thenReturn("1.0");
        when(extension.getPluginSettingsConfiguration(descriptor.id())).thenReturn(pluginSettingsConfiguration);
        when(extension.getPluginSettingsView(descriptor.id())).thenReturn("some html");

        when(extension.getIcon(descriptor.id())).thenReturn(icon);

        when(extension.getProfileMetadata(descriptor.id())).thenReturn(pluginConfigurations);
        when(extension.getProfileView(descriptor.id())).thenReturn("profile_view");

        ElasticAgentPluginInfoBuilder builder = new ElasticAgentPluginInfoBuilder(extension, pluginManager);
        ElasticAgentPluginInfo pluginInfo = builder.pluginInfoFor(descriptor);

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("elastic-agent"));

        assertThat(pluginInfo.getImage(), is(icon));
        assertThat(pluginInfo.getProfileSettings(), is(new PluggableInstanceSettings(pluginConfigurations, new PluginView("profile_view"))));
        assertThat(pluginInfo.getPluginSettings(), is(new PluggableInstanceSettings(builder.configurations(pluginSettingsConfiguration), new PluginView("some html"))));
        assertFalse(pluginInfo.supportsStatusReport());
    }

    @Test
    public void shouldContinueWithBuildingPluginInfoIfPluginSettingsIsNotProvidedByThePlugin() {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);
        List<PluginConfiguration> pluginConfigurations = Arrays.asList(new PluginConfiguration("aws_password", new Metadata(true, false)));

        Image icon = new Image("content_type", "data", "hash");

        doThrow(new RuntimeException("foo")).when(extension).getPluginSettingsConfiguration(descriptor.id());
        when(pluginManager.resolveExtensionVersion("plugin1", SUPPORTED_VERSIONS)).thenReturn("1.0");
        when(extension.getIcon(descriptor.id())).thenReturn(icon);

        when(extension.getProfileMetadata(descriptor.id())).thenReturn(pluginConfigurations);
        when(extension.getProfileView(descriptor.id())).thenReturn("profile_view");

        ElasticAgentPluginInfoBuilder builder = new ElasticAgentPluginInfoBuilder(extension, pluginManager);
        ElasticAgentPluginInfo pluginInfo = builder.pluginInfoFor(descriptor);

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("elastic-agent"));

        assertThat(pluginInfo.getImage(), is(icon));
        assertThat(pluginInfo.getProfileSettings(), is(new PluggableInstanceSettings(pluginConfigurations, new PluginView("profile_view"))));
        assertNull(pluginInfo.getPluginSettings());
    }

    @Test
    public void capabilitiesIsSupportedByElasticAgentPluginsWhichImplementV2() throws Exception {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);

        when(pluginManager.resolveExtensionVersion("plugin1", SUPPORTED_VERSIONS)).thenReturn("2.0");
        Capabilities capabilities = new Capabilities(true);
        when(extension.getCapabilities(descriptor.id())).thenReturn(capabilities);

        ElasticAgentPluginInfo pluginInfo = new ElasticAgentPluginInfoBuilder(extension, pluginManager).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getCapabilities(), is(capabilities));
    }

    @Test
    public void capabilitiesIsNotSupportedByElastucAgentPluginsWhichImplementV1() throws Exception {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);

        when(pluginManager.resolveExtensionVersion("plugin1", SUPPORTED_VERSIONS)).thenReturn("1.0");

        ElasticAgentPluginInfo pluginInfo = new ElasticAgentPluginInfoBuilder(extension, pluginManager).pluginInfoFor(descriptor);

        assertNull(pluginInfo.getCapabilities());
        verify(extension, times(0)).getCapabilities(descriptor.id());
    }
}
