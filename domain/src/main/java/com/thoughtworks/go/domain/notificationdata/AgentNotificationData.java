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
package com.thoughtworks.go.domain.notificationdata;

import java.io.Serializable;
import java.util.Date;

public class AgentNotificationData implements Serializable {
    private final String uuid;
    private final String hostName;
    private final boolean isElastic;
    private final String ipAddress;
    private final String operatingSystem;
    private final String freeSpace;
    private final String agentConfigState;
    private final String agentState;
    private final String buildState;
    private final Date transitionTime;

    public AgentNotificationData(String uuid, String hostName, boolean isElastic, String ipAddress,
                                 String operatingSystem, String freeSpace, String agentConfigState, String agentState,
                                 String buildState, Date transitionTime) {
        this.uuid = uuid;
        this.hostName = hostName;
        this.isElastic = isElastic;
        this.ipAddress = ipAddress;
        this.operatingSystem = operatingSystem;
        this.freeSpace = freeSpace;
        this.agentConfigState = agentConfigState;
        this.agentState = agentState;
        this.buildState = buildState;
        this.transitionTime = transitionTime;
    }

    public String getUuid() {
        return uuid;
    }

    public String getHostName() {
        return hostName;
    }

    public boolean isElastic() {
        return isElastic;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public String getFreeSpace() {
        return freeSpace;
    }

    public String getAgentConfigState() {
        return agentConfigState;
    }

    public String getAgentState() {
        return agentState;
    }

    public String getBuildState() {
        return buildState;
    }

    public Date getTransitionTime() {
        return transitionTime;
    }

    @Override
    public String toString() {
        return "AgentNotificationData{" +
                "uuid='" + uuid + '\'' +
                ", hostName='" + hostName + '\'' +
                ", isElastic=" + isElastic +
                ", ipAddress='" + ipAddress + '\'' +
                ", operatingSystem='" + operatingSystem + '\'' +
                ", freeSpace='" + freeSpace + '\'' +
                ", agentConfigState='" + agentConfigState + '\'' +
                ", agentState='" + agentState + '\'' +
                ", buildState='" + buildState + '\'' +
                ", transitionTime=" + transitionTime +
                '}';
    }
}
