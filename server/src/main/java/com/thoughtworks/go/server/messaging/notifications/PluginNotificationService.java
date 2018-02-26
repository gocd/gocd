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

package com.thoughtworks.go.server.messaging.notifications;

import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PluginNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginNotificationService.class);

    private final NotificationPluginRegistry notificationPluginRegistry;
    private final PluginNotificationsQueueHandler pluginNotificationsQueueHandler;

    @Autowired
    public PluginNotificationService(NotificationPluginRegistry notificationPluginRegistry, PluginNotificationsQueueHandler pluginNotificationsQueueHandler) {
        this.notificationPluginRegistry = notificationPluginRegistry;
        this.pluginNotificationsQueueHandler = pluginNotificationsQueueHandler;
    }

    public <T> void notifyPlugins(PluginNotificationMessage<T> pluginNotificationMessage) {
        Set<String> interestedPlugins = notificationPluginRegistry.getPluginsInterestedIn(pluginNotificationMessage.getRequestName());
        if (interestedPlugins != null && !interestedPlugins.isEmpty()) {
            for (String pluginId : interestedPlugins) {
                pluginNotificationsQueueHandler.post(new NotificationMessage(pluginId, pluginNotificationMessage), 0);
            }
        }
    }
}
