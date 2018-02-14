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

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.notificationdata.AgentNotificationData;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AgentStatusChangeNotifier implements AgentStatusChangeListener{
    private NotificationPluginRegistry notificationPluginRegistry;
    private PluginNotificationQueue pluginNotificationQueue;

    @Autowired
    public AgentStatusChangeNotifier(NotificationPluginRegistry notificationPluginRegistry, PluginNotificationQueue pluginNotificationQueue) {
        this.notificationPluginRegistry = notificationPluginRegistry;
        this.pluginNotificationQueue = pluginNotificationQueue;
    }

    @Override
    public void onAgentStatusChange(AgentInstance agentInstance) {
        if (isAnyPluginInterestedInAgentStatus()) {
            AgentNotificationData agentNotificationData = notificationDataFrom(agentInstance);
            pluginNotificationQueue.post(new PluginNotificationMessage(NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION, agentNotificationData));
        }
    }

    private AgentNotificationData notificationDataFrom(AgentInstance agentInstance) {
        return new AgentNotificationData(agentInstance.getUuid(),
                agentInstance.getHostname(),
                agentInstance.isElastic(),
                agentInstance.getIpAddress(),
                agentInstance.getOperatingSystem(),
                agentInstance.freeDiskSpace().toString(),
                agentInstance.getAgentConfigStatus().name(),
                agentInstance.getRuntimeStatus().agentState().name(),
                agentInstance.getRuntimeStatus().buildState().name(),
                new Date()
        );
    }

    private boolean isAnyPluginInterestedInAgentStatus() {
        return notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.AGENT_STATUS_CHANGE_NOTIFICATION);
    }
}
