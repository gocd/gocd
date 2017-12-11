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

package com.thoughtworks.go.plugin.access.common;

import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler2_0;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.hamcrest.core.Is;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants.REQUEST_NOTIFY_PLUGIN_SETTINGS_CHANGE;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;


public class AbstractExtensionTest {

    private AbstractExtension extension;
    @Mock
    private PluginManager pluginManager;

    private PluginRequestHelper pluginRequestHelper;
    private String extensionName;
    private static List<String> goSupportedVersions = Arrays.asList("1.0", "2.0");
    private String pluginId;

    private static class TestExtension extends AbstractExtension {

        protected TestExtension(PluginManager pluginManager, PluginRequestHelper pluginRequestHelper, String extensionName) {
            super(pluginManager, pluginRequestHelper, extensionName);
        }

        @Override
        protected List<String> goSupportedVersions() {
            return goSupportedVersions;
        }
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        pluginId = "plugin_id";
        extensionName = "testExtension";
        pluginRequestHelper = new PluginRequestHelper(pluginManager, goSupportedVersions, extensionName);
        extension = new TestExtension(pluginManager, pluginRequestHelper, extensionName);

        when(pluginManager.isPluginOfType(extensionName, pluginId)).thenReturn(true);

    }

    @Test
    public void shouldNotifySettingsChangeForPluginWhichSupportsNotification() throws Exception {
        String supportedVersion = "2.0";
        Map<String, String> settings = Collections.singletonMap("foo", "bar");
        ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        extension.registerHandler(supportedVersion, new PluginSettingsJsonMessageHandler2_0());
        when(pluginManager.resolveExtensionVersion(pluginId, goSupportedVersions)).thenReturn(supportedVersion);
        when(pluginManager.submitTo(eq(pluginId), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, ""));

        extension.notifyPluginSettingsChange(pluginId, settings);

        assertRequest(requestArgumentCaptor.getValue(), extensionName,
                supportedVersion, REQUEST_NOTIFY_PLUGIN_SETTINGS_CHANGE, "{\"foo\":\"bar\"}");
    }

    @Test
    public void shouldIgnoreSettingsChangeNotificationIfPluginDoesNotSupportsNotification() throws Exception {
        String supportedVersion = "1.0";
        Map<String, String> settings = Collections.singletonMap("foo", "bar");

        extension.registerHandler(supportedVersion, new PluginSettingsJsonMessageHandler1_0());
        when(pluginManager.resolveExtensionVersion(pluginId, goSupportedVersions)).thenReturn(supportedVersion);

        extension.notifyPluginSettingsChange(pluginId, settings);

        verify(pluginManager, times(0)).submitTo(anyString(), any(GoPluginApiRequest.class));
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) throws JSONException {
        Assert.assertThat(goPluginApiRequest.extension(), Is.is(extensionName));
        Assert.assertThat(goPluginApiRequest.extensionVersion(), Is.is(version));
        Assert.assertThat(goPluginApiRequest.requestName(), Is.is(requestName));
        JSONAssert.assertEquals(requestBody, goPluginApiRequest.requestBody(), true);
    }
}
