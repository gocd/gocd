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
package com.thoughtworks.go.plugin.access.notification;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler2_0;
import com.thoughtworks.go.plugin.access.notification.v1.JsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.notification.v2.JsonMessageHandler2_0;
import com.thoughtworks.go.plugin.access.notification.v3.JsonMessageHandler3_0;
import com.thoughtworks.go.plugin.access.notification.v4.JsonMessageHandler4_0;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.NOTIFICATION_EXTENSION;
import static java.util.Arrays.asList;

@Component
public class NotificationExtension extends AbstractExtension {
    static final List<String> goSupportedVersions = asList("1.0", "2.0", "3.0", "4.0");

    static final String REQUEST_NOTIFICATIONS_INTERESTED_IN = "notifications-interested-in";
    public static final String STAGE_STATUS_CHANGE_NOTIFICATION = "stage-status";
    public static final String AGENT_STATUS_CHANGE_NOTIFICATION = "agent-status";

    static final List<String> VALID_NOTIFICATION_TYPES = asList(STAGE_STATUS_CHANGE_NOTIFICATION, AGENT_STATUS_CHANGE_NOTIFICATION);

    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public NotificationExtension(PluginManager pluginManager, ExtensionsRegistry extensionsRegistry) {
        super(pluginManager, extensionsRegistry, new PluginRequestHelper(pluginManager, goSupportedVersions, NOTIFICATION_EXTENSION), NOTIFICATION_EXTENSION);

        registerHandlers("1.0", new PluginSettingsJsonMessageHandler1_0(), new JsonMessageHandler1_0());

        registerHandlers("2.0", new PluginSettingsJsonMessageHandler1_0(), new JsonMessageHandler2_0());

        registerHandlers("3.0", new PluginSettingsJsonMessageHandler2_0(), new JsonMessageHandler3_0());

        registerHandlers("4.0", new PluginSettingsJsonMessageHandler2_0(), new JsonMessageHandler4_0());
    }

    private void registerHandlers(String version, PluginSettingsJsonMessageHandler pluginSettingsJsonMessageHandler, JsonMessageHandler jsonMessageHandler) {
        registerHandler(version, pluginSettingsJsonMessageHandler);
        messageHandlerMap.put(version, jsonMessageHandler);
    }

    public List<String> getNotificationsOfInterestFor(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_NOTIFICATIONS_INTERESTED_IN, new DefaultPluginInteractionCallback<List<String>>() {


            @Override
            public List<String> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForNotificationsInterestedIn(responseBody);
            }
        });
    }

    public <T> Result notify(String pluginId, final String requestName, final T data) {
        return pluginRequestHelper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Result>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForNotify(data);
            }

            @Override
            public Result onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForNotify(responseBody);
            }
        });
    }

    Map<String, PluginSettingsJsonMessageHandler> getPluginSettingsMessageHandlerMap() {
        return pluginSettingsMessageHandlerMap;
    }

    Map<String, JsonMessageHandler> getMessageHandlerMap() {
        return messageHandlerMap;
    }

    @Override
    public List<String> goSupportedVersions() {
        return goSupportedVersions;
    }
}
