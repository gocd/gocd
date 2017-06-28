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

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PluginNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginNotificationService.class);

    private final NotificationExtension notificationExtension;
    private final NotificationPluginRegistry notificationPluginRegistry;
    private final ServerHealthService serverHealthService;

    @Autowired
    public PluginNotificationService(NotificationExtension notificationExtension, NotificationPluginRegistry notificationPluginRegistry, ServerHealthService serverHealthService) {
        this.notificationExtension = notificationExtension;
        this.notificationPluginRegistry = notificationPluginRegistry;
        this.serverHealthService = serverHealthService;
    }

    public <T> void notifyPlugins(PluginNotificationMessage<T> pluginNotificationMessage) throws Exception {
        Set<String> interestedPlugins = notificationPluginRegistry.getPluginsInterestedIn(pluginNotificationMessage.getRequestName());

        if (interestedPlugins != null && !interestedPlugins.isEmpty()) {
            for (String interestedPlugin : interestedPlugins) {
                notifyPlugin(interestedPlugin, pluginNotificationMessage);
            }
        }
    }

    private <T> void notifyPlugin(String pluginId, PluginNotificationMessage<T> pluginNotificationMessage) {
        HealthStateScope scope = HealthStateScope.forPlugin(pluginId);
        try {
            Result result = notificationExtension.notify(pluginId, pluginNotificationMessage.getRequestName(), pluginNotificationMessage.getData());

            if (result.isSuccessful()) {
                serverHealthService.removeByScope(scope);
            } else {
                String errorDescription = result.getMessages() == null ? null : ListUtil.join(result.getMessages());
                handlePluginNotifyError(pluginId, scope, errorDescription, null);
            }
        } catch (Exception e) {
            String errorDescription = e.getMessage() == null ? "Unknown error" : e.getMessage();
            handlePluginNotifyError(pluginId, scope, errorDescription, e);
        }
    }

    private void handlePluginNotifyError(String pluginId, HealthStateScope scope, String errorDescription, Exception e) {
        String message = "Notification update failed for plugin: " + pluginId;
        serverHealthService.update(ServerHealthState.error(message, errorDescription, HealthStateType.general(scope)));
        LOGGER.warn(message, e);
    }
}
