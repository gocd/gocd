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
package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.configrepo.contract.CRPipeline;
import com.thoughtworks.go.plugin.access.configrepo.v1.JsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.configrepo.v2.JsonMessageHandler2_0;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.CONFIG_REPO_EXTENSION;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConfigRepoExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private PluginManager pluginManager;
    @Mock
    private JsonMessageHandler1_0 jsonMessageHandler1;
    @Mock
    private JsonMessageHandler2_0 jsonMessageHandler2;
    private ConfigRepoExtension extension;
    private String requestBody = "expected-request";
    private String responseBody = "expected-response";
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        extension = new ConfigRepoExtension(pluginManager);
        extension.getMessageHandlerMap().put("1.0", jsonMessageHandler1);
        extension.getMessageHandlerMap().put("2.0", jsonMessageHandler2);

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, CONFIG_REPO_EXTENSION, new ArrayList<>(Arrays.asList("1.0", "2.0")))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(CONFIG_REPO_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));
    }

    @Test
    public void shouldExtendAbstractExtension() throws Exception {
        assertTrue(extension instanceof AbstractExtension);
    }

    @Test
    public void shouldTalkToPluginToGetParsedDirectory() throws Exception {
        CRParseResult deserializedResponse = new CRParseResult();
        when(jsonMessageHandler1.responseMessageForParseDirectory(responseBody)).thenReturn(deserializedResponse);

        CRParseResult response = extension.parseDirectory(PLUGIN_ID, "dir", null);

        assertRequest(requestArgumentCaptor.getValue(), CONFIG_REPO_EXTENSION, "1.0", ConfigRepoExtension.REQUEST_PARSE_DIRECTORY, null);
        verify(jsonMessageHandler1).responseMessageForParseDirectory(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetPipelineExport() throws Exception {
        CRPipeline pipeline = new CRPipeline();
        String deserializedResponse = new GsonCodec().getGson().toJson(pipeline);
        when(jsonMessageHandler2.responseMessageForPipelineExport(responseBody)).thenReturn(deserializedResponse);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, CONFIG_REPO_EXTENSION, new ArrayList<>(Arrays.asList("1.0", "2.0")))).thenReturn("2.0");


        String response = extension.pipelineExport(PLUGIN_ID, pipeline);

        assertRequest(requestArgumentCaptor.getValue(), CONFIG_REPO_EXTENSION, "2.0", ConfigRepoExtension.REQUEST_PIPELINE_EXPORT, null);

        verify(jsonMessageHandler2).responseMessageForPipelineExport(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldRequestCapabilities() throws Exception {
        Capabilities capabilities = new Capabilities(true, true);
        when(jsonMessageHandler2.getCapabilitiesFromResponse(responseBody)).thenReturn(capabilities);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, CONFIG_REPO_EXTENSION, new ArrayList<>(Arrays.asList("1.0", "2.0")))).thenReturn("2.0");

        Capabilities res = extension.getCapabilities(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), CONFIG_REPO_EXTENSION, "2.0", ConfigRepoExtension.REQUEST_CAPABILITIES, null);
        assertSame(capabilities, res);
    }

    @Test
    public void shouldRequestCapabilitiesV1() throws Exception {
        Capabilities capabilities = new Capabilities(false, false);

        Capabilities res = extension.getCapabilities(PLUGIN_ID);

        assertThat(capabilities, is(res));
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension(), is(extensionName));
        assertThat(goPluginApiRequest.extensionVersion(), is(version));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThat(goPluginApiRequest.requestBody(), is(requestBody));
    }

}
