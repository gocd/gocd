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
package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.configrepo.v1.JsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.configrepo.v2.JsonMessageHandler2_0;
import com.thoughtworks.go.plugin.access.configrepo.v3.JsonMessageHandler3_0;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.configrepo.contract.CRPipeline;
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.*;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.CONFIG_REPO_EXTENSION;
import static org.hamcrest.Matchers.is;
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
    @Mock
    private JsonMessageHandler3_0 jsonMessageHandler3;
    @Mock
    ExtensionsRegistry extensionsRegistry;
    private ConfigRepoExtension extension;
    private String responseBody = "expected-response";
    private Map<String, String> responseHeaders = new HashMap<String, String>() {
        {
            put("Content-Type", "text/plain");
            put("X-Export-Filename", "foo.txt");
        }
    };

    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        extension = new ConfigRepoExtension(pluginManager, extensionsRegistry);
        extension.getMessageHandlerMap().put("1.0", jsonMessageHandler1);
        extension.getMessageHandlerMap().put("2.0", jsonMessageHandler2);
        extension.getMessageHandlerMap().put("3.0", jsonMessageHandler3);

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, CONFIG_REPO_EXTENSION, new ArrayList<>(Arrays.asList("1.0", "2.0", "3.0")))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, PLUGIN_ID)).thenReturn(true);
        DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.success(responseBody);
        responseHeaders.forEach(response::addResponseHeader);

        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(CONFIG_REPO_EXTENSION), requestArgumentCaptor.capture())).thenReturn(response);
    }

    @Test
    public void shouldExtendAbstractExtension() {
        assertTrue(extension instanceof AbstractExtension);
    }

    @Test
    public void shouldTalkToPluginToGetParsedDirectory() {
        CRParseResult deserializedResponse = new CRParseResult();
        when(jsonMessageHandler1.responseMessageForParseDirectory(responseBody)).thenReturn(deserializedResponse);

        CRParseResult response = extension.parseDirectory(PLUGIN_ID, "dir", null);

        assertRequest(requestArgumentCaptor.getValue(), CONFIG_REPO_EXTENSION, "1.0", ConfigRepoExtension.REQUEST_PARSE_DIRECTORY, null);
        verify(jsonMessageHandler1).responseMessageForParseDirectory(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetPipelineExport() {
        CRPipeline pipeline = new CRPipeline();
        String serialized = new GsonCodec().getGson().toJson(pipeline);
        when(jsonMessageHandler2.responseMessageForPipelineExport(responseBody, responseHeaders)).thenReturn(ExportedConfig.from(serialized, responseHeaders));
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, CONFIG_REPO_EXTENSION, new ArrayList<>(Arrays.asList("1.0", "2.0", "3.0")))).thenReturn("2.0");


        ExportedConfig response = extension.pipelineExport(PLUGIN_ID, pipeline);

        assertRequest(requestArgumentCaptor.getValue(), CONFIG_REPO_EXTENSION, "2.0", ConfigRepoExtension.REQUEST_PIPELINE_EXPORT, null);

        verify(jsonMessageHandler2).responseMessageForPipelineExport(responseBody, responseHeaders);
        assertSame(response.getContent(), serialized);
    }

    @Test
    public void shouldTalkToPluginToGetConfigFiles() {
        List<String> deserializedResponse = new ArrayList<>();
        deserializedResponse.add("file.yaml");
        ConfigFileList files = new ConfigFileList(deserializedResponse, null);
        when(jsonMessageHandler3.responseMessageForConfigFiles(responseBody)).thenReturn(files);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, CONFIG_REPO_EXTENSION, new ArrayList<>(Arrays.asList("1.0", "2.0", "3.0")))).thenReturn("3.0");


        ConfigFileList response = extension.getConfigFiles(PLUGIN_ID, "dir", null);

        assertRequest(requestArgumentCaptor.getValue(), CONFIG_REPO_EXTENSION, "3.0", ConfigRepoExtension.REQUEST_CONFIG_FILES, null);

        verify(jsonMessageHandler3).responseMessageForConfigFiles(responseBody);
        assertSame(response, files);
    }

    @Test
    public void shouldGracefullyHandleV1AndV2Incompatability() {
        ConfigFileList expected = ConfigFileList.withError("Unsupported Operation", "This plugin version does not support list config files");

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, CONFIG_REPO_EXTENSION, new ArrayList<>(Arrays.asList("1.0", "2.0", "3.0")))).thenReturn("1.0");
        ConfigFileList responseV1 = extension.getConfigFiles(PLUGIN_ID, "dir", null);

        assertTrue("should have errors", responseV1.hasErrors());
        assertThat(responseV1.getErrors().getErrorsAsText(), is(expected.getErrors().getErrorsAsText()));

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, CONFIG_REPO_EXTENSION, new ArrayList<>(Arrays.asList("1.0", "2.0", "3.0")))).thenReturn("2.0");
        ConfigFileList responseV2 = extension.getConfigFiles(PLUGIN_ID, "dir", null);

        assertTrue("should have errors", responseV2.hasErrors());
        assertThat(responseV2.getErrors().getErrorsAsText(), is(expected.getErrors().getErrorsAsText()));
    }

    @Test
    public void shouldRequestCapabilities() {
        Capabilities capabilities = new Capabilities(true, true);
        when(jsonMessageHandler2.getCapabilitiesFromResponse(responseBody)).thenReturn(capabilities);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, CONFIG_REPO_EXTENSION, new ArrayList<>(Arrays.asList("1.0", "2.0", "3.0")))).thenReturn("2.0");

        Capabilities res = extension.getCapabilities(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), CONFIG_REPO_EXTENSION, "2.0", ConfigRepoExtension.REQUEST_CAPABILITIES, null);
        assertSame(capabilities, res);
    }

    @Test
    public void shouldRequestCapabilitiesV1() {
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
