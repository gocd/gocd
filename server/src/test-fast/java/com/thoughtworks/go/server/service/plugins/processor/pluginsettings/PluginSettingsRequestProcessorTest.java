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

package com.thoughtworks.go.server.service.plugins.processor.pluginsettings;

import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginSettingsRequestProcessorTest {
    @Mock
    private PluginRequestProcessorRegistry applicationAccessor;
    @Mock
    private PluginSqlMapDao pluginSqlMapDao;
    @Mock
    private JsonMessageHandler jsonMessageHandler;

    private PluginSettingsRequestProcessor processor;
    private ArgumentCaptor<PluginSettings> requestArgumentCaptor;
    @Mock
    private GoPluginDescriptor pluginDescriptor;

    @Mock
    private GoPluginExtension pluginExtension;

    @Before
    public void setUp() {
        initMocks(this);

        Map<String, String> configuration = new HashMap<>();
        configuration.put("k1", "v1");
        configuration.put("k2", "v2");
        when(pluginSqlMapDao.findPlugin("plugin-id-1")).thenReturn(new Plugin("plugin-id-1", JsonHelper.toJsonString(configuration)));

        when(pluginSqlMapDao.findPlugin("plugin-id-2")).thenReturn(new NullPlugin());

        requestArgumentCaptor = ArgumentCaptor.forClass(PluginSettings.class);

        processor = new PluginSettingsRequestProcessor(applicationAccessor, pluginSqlMapDao, Collections.singletonList(pluginExtension));
        processor.getMessageHandlerMap().put("1.0", jsonMessageHandler);
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

        String responseBody = "expected-response";
        Map<String, String> settingsMap = new HashMap<>();
        settingsMap.put("k1", "v1");
        settingsMap.put("k2", "v2");

        when(pluginExtension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(pluginExtension.pluginSettingsJSON(PLUGIN_ID, settingsMap)).thenReturn(responseBody);

        DefaultGoApiRequest apiRequest = new DefaultGoApiRequest(PluginSettingsRequestProcessor.GET_PLUGIN_SETTINGS, "1.0", null);
        apiRequest.setRequestBody("expected-request");
        GoApiResponse response = processor.process(pluginDescriptor, apiRequest);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(), is(responseBody));
    }

    @Test
    public void shouldNotGetPluginSettingsForPluginThatDoesNotExistInDB() {
        String PLUGIN_ID = "plugin-foo-id";
        String requestBody = "expected-request";

        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new NullPlugin());
        when(pluginExtension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(jsonMessageHandler.responseMessagePluginSettingsGet(any(PluginSettings.class))).thenReturn(null);

        DefaultGoApiRequest apiRequest = new DefaultGoApiRequest(PluginSettingsRequestProcessor.GET_PLUGIN_SETTINGS, "1.0", null);
        apiRequest.setRequestBody(requestBody);
        GoApiResponse response = processor.process(pluginDescriptor, apiRequest);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(), is(nullValue()));
        verify(pluginExtension).pluginSettingsJSON(PLUGIN_ID, Collections.EMPTY_MAP);
    }

    @Test
    public void shouldRespondWith400IfPluginExtensionErrorsOut() {
        String PLUGIN_ID = "plugin-foo-id";

        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(pluginSqlMapDao.findPlugin(PLUGIN_ID)).thenReturn(new Plugin(PLUGIN_ID, "{\"k1\": \"v1\",\"k2\": \"v2\"}"));

        when(pluginExtension.canHandlePlugin(PLUGIN_ID)).thenReturn(true);
        when(pluginExtension.pluginSettingsJSON(eq(PLUGIN_ID), any(Map.class))).thenThrow(RuntimeException.class);

        DefaultGoApiRequest apiRequest = new DefaultGoApiRequest(PluginSettingsRequestProcessor.GET_PLUGIN_SETTINGS, "1.0", null);
        apiRequest.setRequestBody("expected-request");

        GoApiResponse response = processor.process(pluginDescriptor, apiRequest);

        assertThat(response.responseCode(), is(400));
    }

    @Test
    public void shouldRespondWith400IfNoneOfExtensionsCanHandleThePlugin() throws Exception {
        String PLUGIN_ID = "plugin-foo-id";

        when(pluginDescriptor.id()).thenReturn(PLUGIN_ID);
        when(pluginExtension.canHandlePlugin(PLUGIN_ID)).thenReturn(false);

        GoApiResponse response = processor.process(pluginDescriptor, new DefaultGoApiRequest(PluginSettingsRequestProcessor.GET_PLUGIN_SETTINGS, "1.0", null));

        assertThat(response.responseCode(), is(400));
    }
}
