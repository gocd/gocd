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
package com.thoughtworks.go.server.messaging.notifications;

import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginNotificationMessageListener implements GoMessageListener<PluginNotificationMessage> {
    private NotificationExtension notificationExtension;
    private ServerHealthService serverHealthService;
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginNotificationMessageListener.class);

    public PluginNotificationMessageListener(NotificationExtension notificationExtension, ServerHealthService serverHealthService) {
        this.notificationExtension = notificationExtension;
        this.serverHealthService = serverHealthService;
    }

    @Override
    public void onMessage(PluginNotificationMessage message) {
        HealthStateScope scope = HealthStateScope.aboutPlugin(message.pluginId());
        try {
            LOGGER.debug("Sending {} notification message {} for plugin {}", message.getRequestName(), message, message.pluginId());
            Result result = notificationExtension.notify(message.pluginId(), message.getRequestName(), message.getData());

            if (result.isSuccessful()) {
                serverHealthService.removeByScope(scope);
                LOGGER.debug("Successfully sent {} notification message {} for plugin {}", message.getRequestName(), message, message.pluginId());
            } else {
                String errorDescription = result.getMessages() == null ? null : StringUtils.join(result.getMessages(), ", ");
                handlePluginNotifyError(message.pluginId(), scope, errorDescription, null);
            }
        } catch (Exception e) {
            String errorDescription = e.getMessage() == null ? "Unknown error" : e.getMessage();
            handlePluginNotifyError(message.pluginId(), scope, errorDescription, e);
        }
    }

    private void handlePluginNotifyError(String pluginId, HealthStateScope scope, String errorDescription, Exception e) {
        String message = "Notification update failed for plugin: " + pluginId;
        serverHealthService.update(ServerHealthState.error(message, errorDescription, HealthStateType.general(scope)));
        LOGGER.warn(message, e);
    }

}
