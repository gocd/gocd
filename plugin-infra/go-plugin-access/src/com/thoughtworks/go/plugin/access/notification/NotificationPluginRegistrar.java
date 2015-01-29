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

package com.thoughtworks.go.plugin.access.notification;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.apache.log4j.Logger.getLogger;

@Component
public class NotificationPluginRegistrar implements PluginChangeListener {
    private static final Logger LOGGER = getLogger(NotificationPluginRegistrar.class);

    private NotificationExtension notificationExtension;
    private NotificationPluginRegistry notificationPluginRegistry;

    @Autowired
    public NotificationPluginRegistrar(PluginManager pluginManager, NotificationExtension notificationExtension, NotificationPluginRegistry notificationPluginRegistry) {
        this.notificationExtension = notificationExtension;
        this.notificationPluginRegistry = notificationPluginRegistry;
        pluginManager.addPluginChangeListener(this, GoPlugin.class);
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (notificationExtension.isNotificationPlugin(pluginDescriptor.id())) {
            try {
                List<String> notificationsInterestedIn = notificationExtension.getNotificationsOfInterestFor(pluginDescriptor.id());
                notificationPluginRegistry.registerPluginInterests(pluginDescriptor.id(), notificationsInterestedIn);
            } catch (Exception e) {
                LOGGER.warn("Error occurred during plugin notification interest registration.", e);
            }
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        if (notificationExtension.isNotificationPlugin(pluginDescriptor.id())) {
            notificationPluginRegistry.removePluginInterests(pluginDescriptor.id());
        }
    }
}
