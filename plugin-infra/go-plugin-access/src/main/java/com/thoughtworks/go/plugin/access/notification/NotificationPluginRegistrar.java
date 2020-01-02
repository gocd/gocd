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

import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationPluginRegistrar implements PluginChangeListener {
    private static Logger LOGGER = LoggerFactory.getLogger(NotificationPluginRegistrar.class);

    private NotificationExtension notificationExtension;
    private NotificationPluginRegistry notificationPluginRegistry;

    @Autowired
    public NotificationPluginRegistrar(PluginManager pluginManager, NotificationExtension notificationExtension, NotificationPluginRegistry notificationPluginRegistry) {
        this.notificationExtension = notificationExtension;
        this.notificationPluginRegistry = notificationPluginRegistry;
        pluginManager.addPluginChangeListener(this);
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (notificationExtension.canHandlePlugin(pluginDescriptor.id())) {
            try {
                notificationPluginRegistry.registerPlugin(pluginDescriptor.id());
                List<String> notificationsInterestedIn = notificationExtension.getNotificationsOfInterestFor(pluginDescriptor.id());
                if (notificationsInterestedIn != null && !notificationsInterestedIn.isEmpty()) {
                    checkNotificationTypes(pluginDescriptor, notificationsInterestedIn);

                    notificationPluginRegistry.registerPluginInterests(pluginDescriptor.id(), notificationsInterestedIn);
                }
            } catch (Exception e) {
                LOGGER.warn("Error occurred during plugin notification interest registration.", e);
            }
        }
    }

    private void checkNotificationTypes(GoPluginDescriptor pluginDescriptor, List<String> notificationsInterestedIn) {
        for (String notificationType : notificationsInterestedIn) {
            if (!NotificationExtension.VALID_NOTIFICATION_TYPES.contains(notificationType)) {
                LOGGER.warn("Plugin '{}' is trying to register for '{}' which is not a valid notification type. Valid notification types are: {}", pluginDescriptor.id(), notificationType, NotificationExtension.VALID_NOTIFICATION_TYPES);
            }
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        if (notificationExtension.canHandlePlugin(pluginDescriptor.id())) {
            notificationPluginRegistry.deregisterPlugin(pluginDescriptor.id());
            notificationPluginRegistry.removePluginInterests(pluginDescriptor.id());
        }
    }
}
