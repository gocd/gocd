/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.ListUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PluginNotificationService {
    private static final Logger LOGGER = Logger.getLogger(PluginNotificationService.class);

    private final NotificationExtension notificationExtension;
    private final ServerHealthService serverHealthService;

    @Autowired
    public PluginNotificationService(NotificationExtension notificationExtension, ServerHealthService serverHealthService) {
        this.notificationExtension = notificationExtension;
        this.serverHealthService = serverHealthService;
    }

    public void notifyPlugins(PluginNotificationMessage pluginNotificationMessage) throws Exception {
        Set<String> interestedPlugins = NotificationPluginRegistry.getInstance().getPluginsInterestedIn(pluginNotificationMessage.getRequestName());

        if (interestedPlugins != null && !interestedPlugins.isEmpty()) {
            for (String interestedPlugin : interestedPlugins) {
                notifyPlugin(interestedPlugin, pluginNotificationMessage);
            }
        }
    }

    private void notifyPlugin(String pluginId, PluginNotificationMessage pluginNotificationMessage) {
        HealthStateScope scope = HealthStateScope.forPlugin(pluginId);
        try {
            Result result = notificationExtension.notify(pluginId, pluginNotificationMessage.getRequestName(), pluginNotificationMessage.getRequestBody());

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
