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

import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.serverinfo.MessageHandlerForServerInfoRequestProcessor;
import com.thoughtworks.go.plugin.access.common.serverinfo.MessageHandlerForServerInfoRequestProcessor1_0;
import com.thoughtworks.go.plugin.access.common.settings.MessageHandlerForPluginSettingsRequestProcessor;
import com.thoughtworks.go.plugin.access.common.settings.MessageHandlerForPluginSettingsRequestProcessor1_0;
import com.thoughtworks.go.plugin.access.elastic.v1.ElasticAgentExtensionConverterV1;
import com.thoughtworks.go.plugin.access.elastic.v2.ElasticAgentExtensionConverterV2;
import com.thoughtworks.go.plugin.access.elastic.v3.ElasticAgentExtensionConverterV3;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticAgentExtensionTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected PluginManager pluginManager;
    protected ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;
    protected GoPluginDescriptor descriptor;
    protected GoPluginDescriptor.About about;
    protected ElasticAgentExtension extension;


    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        descriptor = mock(GoPluginDescriptor.class);
        about = mock(GoPluginDescriptor.About.class);
        extension = new ElasticAgentExtension(pluginManager);

        when(descriptor.id()).thenReturn("cd.go.example.plugin");
        when(descriptor.about()).thenReturn(about);
        when(about.name()).thenReturn("Example plugin");
        when(about.version()).thenReturn("1.0");

        when(pluginManager.getPluginDescriptorFor("cd.go.example.plugin")).thenReturn(descriptor);
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, "cd.go.example.plugin")).thenReturn(true);
    }

    @Test
    public void shouldExtendAbstractExtension() {
        assertTrue(new ElasticAgentExtension(pluginManager) instanceof AbstractExtension);
    }

    @Test
    public void shouldHaveMessageHandlerForPluginSettingsRequestProcessorForAllExtensionVersions() {
        final ElasticAgentExtensionExposingHandlers extension = new ElasticAgentExtensionExposingHandlers(pluginManager);

        assertThat(extension.messageHandlerForPluginSettingsRequestProcessor("1.0"),
                instanceOf(MessageHandlerForPluginSettingsRequestProcessor1_0.class));

        assertThat(extension.messageHandlerForPluginSettingsRequestProcessor("2.0"),
                instanceOf(MessageHandlerForPluginSettingsRequestProcessor1_0.class));

        assertThat(extension.messageHandlerForPluginSettingsRequestProcessor("3.0"),
                instanceOf(MessageHandlerForPluginSettingsRequestProcessor1_0.class));
    }

    @Test
    public void shouldHaveMessageHandlerForServerInfoRequestProcessorForAllExtensionVersions() {
        final ElasticAgentExtensionExposingHandlers extension = new ElasticAgentExtensionExposingHandlers(pluginManager);

        assertThat(extension.messageHandlerForServerInfoRequestProcessor("1.0"),
                instanceOf(MessageHandlerForServerInfoRequestProcessor1_0.class));

        assertThat(extension.messageHandlerForServerInfoRequestProcessor("2.0"),
                instanceOf(MessageHandlerForServerInfoRequestProcessor1_0.class));

        assertThat(extension.messageHandlerForServerInfoRequestProcessor("3.0"),
                instanceOf(MessageHandlerForServerInfoRequestProcessor1_0.class));
    }

    @Test
    public void shouldHaveMessageHandlerForElasticAgentMessageConverterForAllExtensionVersions() {
        final ElasticAgentExtensionExposingHandlers extension = new ElasticAgentExtensionExposingHandlers(pluginManager);

        assertThat(extension.getElasticAgentMessageConverter("1.0"), instanceOf(ElasticAgentExtensionConverterV1.class));
        assertThat(extension.getElasticAgentMessageConverter("2.0"), instanceOf(ElasticAgentExtensionConverterV2.class));
        assertThat(extension.getElasticAgentMessageConverter("3.0"), instanceOf(ElasticAgentExtensionConverterV3.class));
    }

    protected void setExtensionVersionForPluginTo(String pluginId, String extensionVersionForPlugin) {
        when(pluginManager.resolveExtensionVersion(pluginId, extension.goSupportedVersions())).thenReturn(extensionVersionForPlugin);
    }

    protected void assertExtensionRequest(String extensionVersion, String requestName, String requestBody) throws JSONException {
        final GoPluginApiRequest request = requestArgumentCaptor.getValue();
        assertThat(request.requestName(), is(requestName));
        assertThat(request.extensionVersion(), is(extensionVersion));
        assertThat(request.extension(), is(PluginConstants.ELASTIC_AGENT_EXTENSION));
        JSONAssert.assertEquals(requestBody, request.requestBody(), true);
    }

    class ElasticAgentExtensionExposingHandlers extends ElasticAgentExtension {
        public ElasticAgentExtensionExposingHandlers(PluginManager pluginManager) {
            super(pluginManager);
        }

        @Override
        public MessageHandlerForPluginSettingsRequestProcessor messageHandlerForPluginSettingsRequestProcessor(String pluginVersion) {
            return super.messageHandlerForPluginSettingsRequestProcessor(pluginVersion);
        }

        @Override
        public MessageHandlerForServerInfoRequestProcessor messageHandlerForServerInfoRequestProcessor(String pluginVersion) {
            return super.messageHandlerForServerInfoRequestProcessor(pluginVersion);
        }
    }
}