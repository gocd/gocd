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
package com.thoughtworks.go.server.service.plugins.processor.pluginsettings;

import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluginSettingsRequestProcessorTest {
    @Mock
    private PluginRequestProcessorRegistry applicationAccessor;
    @Mock
    private PluginSqlMapDao pluginSqlMapDao;
    @Mock
    private GoPluginDescriptor pluginDescriptor;
    @Mock
    private GoPluginExtension pluginExtension;

    private PluginSettingsRequestProcessor processor;

    @BeforeEach
    public void setUp() {

        Map<String, String> configuration = new HashMap<>();
        configuration.put("k1", "v1");
        configuration.put("k2", "v2");

        lenient().when(pluginSqlMapDao.findPlugin("plugin-id-1")).thenReturn(new Plugin("plugin-id-1", JsonHelper.toJsonString(configuration)));
        lenient().when(pluginSqlMapDao.findPlugin("plugin-id-2")).thenReturn(null);
        lenient().when(pluginExtension.extensionName()).thenReturn("extension1");

        processor = new PluginSettingsRequestProcessor(applicationAccessor, pluginSqlMapDao);
    }

    @Test
    public void shouldRegisterItselfForRequestProcessing() {
        verify(applicationAccessor).registerProcessorFor(PluginSettingsRequestProcessor.GET_PLUGIN_SETTINGS, processor);
    }

    @Test
    public void shouldGetPluginSettingsForPluginThatExistsInDB() {
        String PLUGIN_ID = "plugin-foo-id";

        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, "{\"k1\": \"v1\",\"k2\": \"v2\"}"));

        DefaultGoApiRequest apiRequest = new DefaultGoApiRequest(PluginSettingsRequestProcessor.GET_PLUGIN_SETTINGS, "1.0", new GoPluginIdentifier("extension1", Collections.singletonList("1.0")));
        apiRequest.setRequestBody("expected-request");
        GoApiResponse response = processor.process(pluginDescriptor, apiRequest);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(), is("{\"k1\":\"v1\",\"k2\":\"v2\"}"));
    }

    @Test
    public void shouldNotGetPluginSettingsForPluginThatDoesNotExistInDB() {
        String PLUGIN_ID = "plugin-foo-id";
        String requestBody = "expected-request";

        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new NullPlugin());

        DefaultGoApiRequest apiRequest = new DefaultGoApiRequest(PluginSettingsRequestProcessor.GET_PLUGIN_SETTINGS, "1.0", new GoPluginIdentifier("extension1", Collections.singletonList("1.0")));
        apiRequest.setRequestBody(requestBody);
        GoApiResponse response = processor.process(pluginDescriptor, apiRequest);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(), is(nullValue()));
    }

    @Test
    public void shouldRespondWith400InCaseOfErrors() {
        String PLUGIN_ID = "plugin-foo-id";

        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenThrow(new RuntimeException());

        DefaultGoApiRequest apiRequest = new DefaultGoApiRequest(PluginSettingsRequestProcessor.GET_PLUGIN_SETTINGS, "1.0", new GoPluginIdentifier("extension1", Collections.singletonList("1.0")));
        apiRequest.setRequestBody("expected-request");

        GoApiResponse response = processor.process(pluginDescriptor, apiRequest);

        assertThat(response.responseCode(), is(400));
    }
}
