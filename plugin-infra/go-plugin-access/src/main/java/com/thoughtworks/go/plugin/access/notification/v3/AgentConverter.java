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
package com.thoughtworks.go.plugin.access.notification.v3;

import com.thoughtworks.go.domain.notificationdata.AgentNotificationData;
import com.thoughtworks.go.plugin.access.notification.DataConverter;

public class AgentConverter extends DataConverter<AgentNotificationDTO> {
    private AgentNotificationData agentNotificationData;

    public AgentConverter(AgentNotificationData agentNotificationData) {
        this.agentNotificationData = agentNotificationData;
    }

    @Override
    protected AgentNotificationDTO transformData() {
        return new AgentNotificationDTO(
                agentNotificationData.getUuid(),
                agentNotificationData.getHostName(),
                agentNotificationData.isElastic(),
                agentNotificationData.getIpAddress(),
                agentNotificationData.getOperatingSystem(),
                agentNotificationData.getFreeSpace(),
                agentNotificationData.getAgentConfigState(),
                agentNotificationData.getAgentState(),
                agentNotificationData.getBuildState(),
                DateUtil.dateToString(agentNotificationData.getTransitionTime())
                );
    }
}
