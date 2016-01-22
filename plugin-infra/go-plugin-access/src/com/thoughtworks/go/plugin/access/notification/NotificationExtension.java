/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.settings.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Component
public class NotificationExtension extends AbstractExtension {
    public static final String EXTENSION_NAME = "notification";
    private static final List<String> goSupportedVersions = asList("1.0");

    public static final String REQUEST_NOTIFICATIONS_INTERESTED_IN = "notifications-interested-in";
    public static final String STAGE_STATUS_CHANGE_NOTIFICATION = "stage-status";

    public static final List<String> VALID_NOTIFICATION_TYPES = asList(STAGE_STATUS_CHANGE_NOTIFICATION);

    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public NotificationExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, goSupportedVersions, EXTENSION_NAME), EXTENSION_NAME);
        pluginSettingsMessageHandlerMap.put("1.0", new PluginSettingsJsonMessageHandler1_0());
        this.messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }

    public List<String> getNotificationsOfInterestFor(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_NOTIFICATIONS_INTERESTED_IN, new DefaultPluginInteractionCallback<List<String>>() {


            @Override
            public List<String> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForNotificationsInterestedIn(responseBody);
            }
        });
    }

    public Result notify(String pluginId, final String requestName, final Map requestMap) {
        return pluginRequestHelper.submitRequest(pluginId, requestName, new DefaultPluginInteractionCallback<Result>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForNotify(requestName, requestMap);
            }

            @Override
            public Result onSuccess(String responseBody, String resolvedExtensionVersion) {
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
}
