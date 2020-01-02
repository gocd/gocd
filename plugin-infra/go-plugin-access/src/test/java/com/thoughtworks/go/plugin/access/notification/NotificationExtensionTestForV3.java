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
package com.thoughtworks.go.plugin.access.notification;

import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler2_0;
import com.thoughtworks.go.plugin.access.notification.v3.JsonMessageHandler3_0;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants.REQUEST_NOTIFY_PLUGIN_SETTINGS_CHANGE;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.NOTIFICATION_EXTENSION;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class NotificationExtensionTestForV3 extends NotificationExtensionTestBase {
    @Mock
    private PluginSettingsJsonMessageHandler2_0 pluginSettingsJSONMessageHandlerv2;

    @Mock
    private JsonMessageHandler3_0 jsonMessageHandlerv3;

    @Override
    protected String apiVersion() {
        return "3.0";
    }

    @Override
    protected PluginSettingsJsonMessageHandler pluginSettingsJSONMessageHandler() {
        return pluginSettingsJSONMessageHandlerv2;
    }

    @Override
    protected JsonMessageHandler jsonMessageHandler() {
        return jsonMessageHandlerv3;
    }

    @Test
    public void shouldNotifyPluginSettingsChange() throws Exception {
        String supportedVersion = "3.0";
        Map<String, String> settings = Collections.singletonMap("foo", "bar");
        ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.resolveExtensionVersion(eq("pluginId"), eq(NOTIFICATION_EXTENSION), anyList())).thenReturn(supportedVersion);
        when(pluginManager.isPluginOfType(NOTIFICATION_EXTENSION, "pluginId")).thenReturn(true);
        when(pluginManager.submitTo(eq("pluginId"), eq(NOTIFICATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, ""));

        NotificationExtension extension = new NotificationExtension(pluginManager, extensionsRegistry);
        extension.notifyPluginSettingsChange("pluginId", settings);

        assertRequest(requestArgumentCaptor.getValue(), NOTIFICATION_EXTENSION,
                supportedVersion, REQUEST_NOTIFY_PLUGIN_SETTINGS_CHANGE, "{\"foo\":\"bar\"}");
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        Assert.assertThat(goPluginApiRequest.extension(), is(extensionName));
        Assert.assertThat(goPluginApiRequest.extensionVersion(), is(version));
        Assert.assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThatJson(requestBody).isEqualTo(goPluginApiRequest.requestBody());
    }
}
