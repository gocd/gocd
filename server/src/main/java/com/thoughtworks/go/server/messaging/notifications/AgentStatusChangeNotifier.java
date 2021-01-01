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

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.plugin.access.notification.NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION;

@Component
public class AgentStatusChangeNotifier implements AgentStatusChangeListener {
    private NotificationPluginRegistry notificationPluginRegistry;
    private PluginNotificationService pluginNotificationService;

    @Autowired
    public AgentStatusChangeNotifier(NotificationPluginRegistry pluginRegistry, PluginNotificationService notificationService) {
        this.notificationPluginRegistry = pluginRegistry;
        this.pluginNotificationService = notificationService;
    }

    @Override
    public void onAgentStatusChange(AgentInstance agentInstance) {
        if (isAnyPluginInterestedInAgentStatus()) {
            pluginNotificationService.notifyAgentStatus(agentInstance);
        }
    }

    private boolean isAnyPluginInterestedInAgentStatus() {
        return notificationPluginRegistry.isAnyPluginInterestedIn(AGENT_STATUS_CHANGE_NOTIFICATION);
    }
}
