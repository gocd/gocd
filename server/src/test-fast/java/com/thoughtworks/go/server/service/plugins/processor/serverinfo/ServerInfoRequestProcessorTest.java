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
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.server.service.GoConfigService;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ServerInfoRequestProcessorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private GoConfigService goConfigService;
    private PluginRequestProcessorRegistry processorRegistry;
    private ServerInfoRequestProcessor processor;
    private ServerConfig serverConfig;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        serverConfig = new ServerConfig();
        serverConfig.ensureServerIdExists();
        serverConfig.setSecureSiteUrl("https://example.com:8154/go");
        serverConfig.setSiteUrl("http://example.com:8153/go");

        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        processorRegistry = new PluginRequestProcessorRegistry();
        processor = new ServerInfoRequestProcessor(processorRegistry, goConfigService);
    }

    @Test
    public void shouldRegisterAPIRequestWithProcessor() throws Exception {
        DefaultGoApiRequest request = new DefaultGoApiRequest(ServerInfoRequestProcessor.GET_SERVER_ID, "1.0", new GoPluginIdentifier("foo", Collections.singletonList("1.0")));
        assertThat(processorRegistry.canProcess(request), is(true));
    }

    @Test
    public void shouldReturnAServerIdInJSONForm() throws Exception {
        DefaultGoApiRequest request = new DefaultGoApiRequest(ServerInfoRequestProcessor.GET_SERVER_ID, "1.0", new GoPluginIdentifier("foo", Arrays.asList("1.0")));
        GoApiResponse response = processor.process(null, request);
        assertThat(response.responseCode(), is(200));
        JsonAssert.assertJsonEquals("{\n" +
                "  \"server_id\": \"" + serverConfig.getServerId() + "\",\n" +
                "  \"site_url\": \"" + serverConfig.getSiteUrl().getUrl() + "\",\n" +
                "  \"secure_site_url\": \"" + serverConfig.getSecureSiteUrl().getUrl() + "\"\n" +
                "}", response.responseBody());
    }

    @Test
    public void shouldRaiseErrorWhenApiVersionIsUnSupported() throws Exception {
        exception.expect(RuntimeException.class);
        exception.expectMessage("Unsupported 'go.processor.server-info.get' API version: bad-version. Supported versions: [1.0]");
        DefaultGoApiRequest request = new DefaultGoApiRequest(ServerInfoRequestProcessor.GET_SERVER_ID, "bad-version", new GoPluginIdentifier("foo", Arrays.asList("1.0")));
        processor.process(null, request);
    }
}