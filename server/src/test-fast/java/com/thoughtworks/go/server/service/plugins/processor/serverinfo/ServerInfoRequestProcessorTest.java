/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.server.service.plugins.processor.serverinfo.ServerInfoRequestProcessor.GET_SERVER_INFO;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
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
        processor = new ServerInfoRequestProcessor(processorRegistry, goConfigService);

        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(pluginExtension.extensionName()).thenReturn("extension1");
        when(pluginDescriptor.id()).thenReturn(pluginId);
    }

    @Test
    public void shouldRegisterAPIRequestWithProcessor() {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_INFO, "1.0", new GoPluginIdentifier("extension1", Collections.singletonList("1.0")));
        assertThat(processorRegistry.canProcess(request), is(true));
    }

    @Test
    public void shouldReturnAServerIdInJSONForm() {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_INFO, "1.0", new GoPluginIdentifier("extension1", Arrays.asList("1.0")));


        GoApiResponse response = processor.process(pluginDescriptor, request);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(),
                is(format("{\"server_id\":\"%s\",\"site_url\":\"%s\",\"secure_site_url\":\"%s\"}",
                        serverConfig.getServerId(), serverConfig.getSiteUrl().getUrl(), serverConfig.getSecureSiteUrl().getUrl())));
    }

    @Test
    public void shouldReturnSuccessForServerInfoV2() {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_INFO, "2.0", new GoPluginIdentifier("extension1", Arrays.asList("1.0")));

        GoApiResponse response = processor.process(pluginDescriptor, request);

        assertThat(response.responseCode(), is(200));
    }

    @Test
    public void shouldReturnAErrorResponseIfExtensionDoesNotSupportServerInfo() {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_INFO, "bad-version", new GoPluginIdentifier("foo", Arrays.asList("1.0")));

        GoApiResponse response = processor.process(pluginDescriptor, request);

        assertThat(response.responseCode(), is(400));
    }
}
