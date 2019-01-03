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

package com.thoughtworks.go.plugin.access.notification;

import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.notification.v1.JsonMessageHandler1_0;
import org.hamcrest.core.Is;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class NotificationExtensionTestForV1 extends NotificationExtensionTestBase {
    @Mock
    private PluginSettingsJsonMessageHandler1_0 pluginSettingsJSONMessageHandlerv1;

    @Mock
    private JsonMessageHandler1_0 jsonMessageHandlerv1;

    @Override
    protected String apiVersion() {
        return "1.0";
    }

    @Override
    protected PluginSettingsJsonMessageHandler pluginSettingsJSONMessageHandler() {
        return pluginSettingsJSONMessageHandlerv1;
    }

    @Override
    protected JsonMessageHandler jsonMessageHandler() {
        return jsonMessageHandlerv1;
    }

    @Test
    public void shouldSerializePluginSettingsToJSON() throws Exception {
        String pluginId = "plugin_id";
        HashMap<String, String> pluginSettings = new HashMap<>();
        pluginSettings.put("k1", "value1");
        pluginSettings.put("k2", "value2");

        NotificationExtension notificationExtension = new NotificationExtension(pluginManager);

        when(pluginManager.resolveExtensionVersion(pluginId, PluginConstants.NOTIFICATION_EXTENSION, notificationExtension.goSupportedVersions())).thenReturn(apiVersion());
        String pluginSettingsJSON = notificationExtension.pluginSettingsJSON(pluginId, pluginSettings);

        assertThat(pluginSettingsJSON, is("{\"k1\":\"value1\",\"k2\":\"value2\"}"));
    }
}