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
package com.thoughtworks.go.plugin.access.notification.v3;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AgentNotificationDTO {
    @Expose
    @SerializedName("uuid")
    private String uuid;

    @Expose
    @SerializedName("host_name")
    private String hostName;

    @Expose
    @SerializedName("is_elastic")
    private boolean isElastic;

    @Expose
    @SerializedName("ip_address")
    private String ipAddress;

    @Expose
    @SerializedName("operating_system")
    private String operatingSystem;

    @Expose
    @SerializedName("free_space")
    private String freeSpace;

    @Expose
    @SerializedName("agent_config_state")
    private String agentConfigState;

    @Expose
    @SerializedName("agent_state")
    private String agentState;

    @Expose
    @SerializedName("build_state")
    private String buildState;

    @Expose
    @SerializedName("transition_time")
    private String transitionTime;

    public AgentNotificationDTO(String uuid, String hostName, boolean isElastic, String ipAddress,
                                String operatingSystem, String freeSpace, String agentConfigState, String agentState,
                                String buildState, String transitionTime) {
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
}
