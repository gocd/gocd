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
package com.thoughtworks.go.server.messaging.notifications;

import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.messaging.*;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PluginNotificationsQueueHandler extends PluginMessageQueueHandler<PluginNotificationMessage> {
    private final static String QUEUE_NAME_PREFIX = PluginNotificationsQueueHandler.class.getSimpleName() + ".";

    @Autowired
    public PluginNotificationsQueueHandler(final MessagingService messaging, NotificationExtension notificationExtension,
        PluginManager pluginManager, final SystemEnvironment systemEnvironment, ServerHealthService serverHealthService) {
        super(notificationExtension, messaging, pluginManager, new QueueFactory() {
            @Override
            public PluginAwareMessageQueue create(GoPluginDescriptor pluginDescriptor) {
                return new PluginAwareMessageQueue(messaging, pluginDescriptor.id(),
                QUEUE_NAME_PREFIX + pluginDescriptor.id(),
                 systemEnvironment.getNotificationListenerCountForPlugin(pluginDescriptor.id()), listener());
            }

            public ListenerFactory listener() {
                return () -> new PluginNotificationMessageListener(notificationExtension, serverHealthService);
            }
        });
    }

    Map<String, PluginAwareMessageQueue> getQueues() {
        return queues;
    }
}
