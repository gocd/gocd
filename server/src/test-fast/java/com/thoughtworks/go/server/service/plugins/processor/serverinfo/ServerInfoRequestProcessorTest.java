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

package com.thoughtworks.go.server.service.plugins.processor.serverinfo;

import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.server.service.plugins.processor.serverinfo.ServerInfoRequestProcessor.GET_SERVER_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ServerInfoRequestProcessorTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private GoPluginExtension pluginExtension;
    @Mock
    private GoPluginDescriptor pluginDescriptor;
    private PluginRequestProcessorRegistry processorRegistry;
    private ServerInfoRequestProcessor processor;
    private ServerConfig serverConfig;
    private String pluginId = "plugin_id";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        serverConfig = new ServerConfig();
        serverConfig.ensureServerIdExists();
        serverConfig.setSecureSiteUrl("https://example.com:8154/go");
        serverConfig.setSiteUrl("http://example.com:8153/go");

        processorRegistry = new PluginRequestProcessorRegistry();
        processor = new ServerInfoRequestProcessor(processorRegistry, goConfigService, Collections.singletonList(pluginExtension));

        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(pluginExtension.extensionName()).thenReturn("extension1");
        when(pluginDescriptor.id()).thenReturn(pluginId);
    }

    @Test
    public void shouldRegisterAPIRequestWithProcessor() throws Exception {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_ID, "1.0", new GoPluginIdentifier("extension1", Collections.singletonList("1.0")));
        assertThat(processorRegistry.canProcess(request), is(true));
    }

    @Test
    public void shouldReturnAServerIdInJSONForm() throws Exception {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_ID, "1.0", new GoPluginIdentifier("extension1", Arrays.asList("1.0")));

        when(pluginExtension.canHandlePlugin(pluginId)).thenReturn(true);
        when(pluginExtension.serverInfoJSON(pluginId, serverConfig.getServerId(), serverConfig.getSiteUrl().getUrl(), serverConfig.getSecureSiteUrl().getUrl())).thenReturn("server_info");

        GoApiResponse response = processor.process(pluginDescriptor, request);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(), is("server_info"));
    }

    @Test
    public void shouldReturnAErrorResponseIfExtensionDoesNotSupportServerInfo() throws Exception {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_ID, "bad-version", new GoPluginIdentifier("foo", Arrays.asList("1.0")));

        when(pluginExtension.canHandlePlugin(pluginId)).thenReturn(true);
        when(pluginExtension.serverInfoJSON(any(String.class), any(String.class), any(String.class), any(String.class))).thenThrow(new UnsupportedOperationException("Operation not supported."));

        GoApiResponse response = processor.process(pluginDescriptor, request);

        assertThat(response.responseCode(), is(400));
    }

    @Test
    public void shouldRouteToTheRightExtensionBasedOnTheRequest_WhenAPluginHasMultipleExtensions() throws Exception {
        GoPluginExtension anotherPluginExtension = mock(GoPluginExtension.class);
        processor = new ServerInfoRequestProcessor(processorRegistry, goConfigService, Arrays.asList(this.pluginExtension, anotherPluginExtension));



        DefaultGoApiRequest requestFromExtension1 = new DefaultGoApiRequest(GET_SERVER_ID, "1.0", new GoPluginIdentifier("extension1", Arrays.asList("1.0")));
        setupExpectationsFor(pluginExtension, pluginId, "extension1");
        setupExpectationsFor(anotherPluginExtension, pluginId, "extension2");

        processor.process(pluginDescriptor, requestFromExtension1);

        verify(pluginExtension).serverInfoJSON(pluginId, serverConfig.getServerId(), serverConfig.getSiteUrl().getUrl(), serverConfig.getSecureSiteUrl().getUrl());
        verify(anotherPluginExtension, never()).serverInfoJSON(anyString(), anyString(), anyString(), anyString());
        Mockito.reset(pluginExtension, anotherPluginExtension);



        DefaultGoApiRequest requestFromExtension2 = new DefaultGoApiRequest(GET_SERVER_ID, "1.0", new GoPluginIdentifier("extension2", Arrays.asList("1.0")));
        setupExpectationsFor(pluginExtension, pluginId, "extension1");
        setupExpectationsFor(anotherPluginExtension, pluginId, "extension2");

        processor.process(pluginDescriptor, requestFromExtension2);

        verify(pluginExtension, never()).serverInfoJSON(anyString(), anyString(), anyString(), anyString());
        verify(anotherPluginExtension).serverInfoJSON(pluginId, serverConfig.getServerId(), serverConfig.getSiteUrl().getUrl(), serverConfig.getSecureSiteUrl().getUrl());
        Mockito.reset(pluginExtension, anotherPluginExtension);
    }

    private void setupExpectationsFor(GoPluginExtension extension, String pluginId, String extensionType) {
        when(extension.extensionName()).thenReturn(extensionType);
        when(extension.canHandlePlugin(pluginId)).thenReturn(true);
        when(extension.serverInfoJSON(pluginId, serverConfig.getServerId(), serverConfig.getSiteUrl().getUrl(), serverConfig.getSecureSiteUrl().getUrl())).thenReturn("server_info");
    }
}
