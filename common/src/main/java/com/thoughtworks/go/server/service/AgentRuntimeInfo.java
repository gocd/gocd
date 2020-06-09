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
package com.thoughtworks.go.server.service;

import com.google.gson.annotations.Expose;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.DiskSpace;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

import static com.thoughtworks.go.domain.AgentRuntimeStatus.Building;
import static com.thoughtworks.go.domain.AgentRuntimeStatus.Cancelled;
import static com.thoughtworks.go.domain.AgentStatus.Pending;
import static com.thoughtworks.go.server.service.AgentBuildingInfo.NOT_BUILDING;
import static com.thoughtworks.go.util.SystemUtil.isLocalIpAddress;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class AgentRuntimeInfo implements Serializable {
    private static Logger LOGGER = LoggerFactory.getLogger(AgentRuntimeInfo.class);

    @Expose
    private AgentIdentifier identifier;
    @Expose
    private volatile AgentRuntimeStatus runtimeStatus;
    @Expose
    private volatile AgentBuildingInfo buildingInfo;
    @Expose
    private volatile String location;
    @Expose
    private volatile Long usableSpace;
    @Expose
    private volatile String operatingSystemName;
    @Expose
    private volatile String agentBootstrapperVersion = "UNKNOWN";
    @Expose
    private volatile String agentVersion = "UNKNOWN";
    @Expose
    private volatile String cookie;

    public AgentRuntimeInfo(AgentIdentifier identifier, AgentRuntimeStatus runtimeStatus, String location, String cookie) {
        this.identifier = identifier;
        this.runtimeStatus = runtimeStatus;
        this.buildingInfo = NOT_BUILDING;
        this.location = location;
        this.cookie = cookie;
    }

    public static AgentRuntimeInfo fromAgent(AgentIdentifier identifier, AgentRuntimeStatus runtimeStatus, String currentWorkingDirectory, String agentBootstrapperVersion, String agentVersion) {
        return new AgentRuntimeInfo(identifier, runtimeStatus, currentWorkingDirectory, null)
                .refreshOperatingSystem()
                .refreshUsableSpace()
                .updateBootstrapperVersion(agentBootstrapperVersion)
                .updateAgentVersion(agentVersion);
    }

    public static AgentRuntimeInfo fromServer(Agent agent, boolean registeredAlready, String location,
                                              Long usablespace, String operatingSystem ) {

        if (isEmpty(location)) {
            throw new RuntimeException("Agent should not register without installation path.");
        }

        AgentStatus status = Pending;
        if (isLocalIpAddress(agent.getIpaddress()) || registeredAlready) {
            status = AgentStatus.Idle;
        }

        AgentRuntimeStatus runtimeStatus = status.getRuntimeStatus();
        AgentIdentifier identifier = agent.getAgentIdentifier();
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(identifier, runtimeStatus, location, null);
        runtimeInfo.setUsableSpace(usablespace);
        runtimeInfo.operatingSystemName = operatingSystem;
        return runtimeInfo;
    }

    public static AgentRuntimeInfo initialState(Agent agent) {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentStatus.fromRuntime(AgentRuntimeStatus.Missing).getRuntimeStatus(), "", null);
        if (agent.isElastic()) {
            agentRuntimeInfo = ElasticAgentRuntimeInfo.fromServer(agentRuntimeInfo, agent.getElasticAgentId(), agent.getElasticPluginId());
        }
        return agentRuntimeInfo;
    }

    public void busy(AgentBuildingInfo agentBuildingInfo) {
        this.buildingInfo = agentBuildingInfo;
        this.runtimeStatus = Building;
    }

    public void cancel() {
        this.runtimeStatus = Cancelled;
    }

    public void idle() {
        this.runtimeStatus = AgentRuntimeStatus.Idle;
        this.buildingInfo = NOT_BUILDING;
    }

    public AgentRuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    public AgentBuildingInfo getBuildingInfo() {
        return buildingInfo;
    }

    public boolean isCancelled() {
        return runtimeStatus == Cancelled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentRuntimeInfo that = (AgentRuntimeInfo) o;
        return Objects.equals(identifier, that.identifier) &&
                runtimeStatus == that.runtimeStatus &&
                Objects.equals(buildingInfo, that.buildingInfo) &&
                Objects.equals(location, that.location) &&
                Objects.equals(usableSpace, that.usableSpace) &&
                Objects.equals(operatingSystemName, that.operatingSystemName) &&
                Objects.equals(agentBootstrapperVersion, that.agentBootstrapperVersion) &&
                Objects.equals(agentVersion, that.agentVersion) &&
                Objects.equals(cookie, that.cookie);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, runtimeStatus, buildingInfo, location, usableSpace, operatingSystemName, agentBootstrapperVersion, agentVersion, cookie);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getUUId() {
        return identifier.getUuid();
    }

    public Agent agent() {
        return new Agent(getUUId(), identifier.getHostName(), identifier.getIpAddress());
    }

    public String getIpAdress() {
        return identifier.getIpAddress();
    }

    public String getHostName() {
        return identifier.getHostName();
    }

    public AgentIdentifier getIdentifier() {
        return identifier;
    }

    public String getLocation() {
        return location;
    }

    public String getCookie() {
        return cookie;
    }

    public void setStatus(AgentStatus status) {
        this.runtimeStatus = status.getRuntimeStatus();
    }

    public void setBuildingInfo(AgentBuildingInfo buildingInfo) {
        this.buildingInfo = buildingInfo;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public AgentRuntimeInfo refreshOperatingSystem() {
        setOperatingSystem(new SystemEnvironment().getOperatingSystemCompleteName());
        return this;
    }

    public AgentRuntimeInfo refreshUsableSpace() {
        setUsableSpace(usableSpace(location));
        return this;
    }

    protected AgentRuntimeInfo updateBootstrapperVersion(String agentBootstrapperVersion) {
        this.agentBootstrapperVersion = agentBootstrapperVersion;
        return this;
    }

    protected AgentRuntimeInfo updateAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
        return this;
    }

    public static long usableSpace(String currentWorkingDir) {
        File file = new File(currentWorkingDir, "pipelines");
        if (!file.exists()) {
            LOGGER.warn("the [{}] should be created when agent starts up, but it seems missing at the moment. Cruise should be able to automatically create it later", file.getAbsolutePath());
        }
        return file.getUsableSpace();
    }

    public Long getUsableSpace() {
        return usableSpace;
    }

    public void setUsableSpace(Long usableSpace) {
        this.usableSpace = usableSpace;
    }

    public void clearBuildingInfo() {
        this.buildingInfo = NOT_BUILDING;
    }

    public boolean isLowDiskSpace(long limit) {
        if (usableSpace == null) {
            return false;
        }
        return usableSpace < limit;
    }

    public String agentInfoDebugString() {
        return format("Agent [%s, %s, %s, %s]", getHostName(), getIpAdress(), getUUId(), cookie);
    }

    public String agentInfoForDisplay() {
        return format("Agent located at [%s, %s, %s]", getHostName(), getIpAdress(), getLocation());
    }

    public String getOperatingSystem() {
        return operatingSystemName;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystemName = operatingSystem;
    }

    public boolean hasDuplicateCookie(String cookie) {
        return hasCookie() && cookie != null && !this.cookie.equals(cookie);
    }

    public boolean hasCookie() {
        return this.cookie != null;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void setRuntimeStatus(AgentRuntimeStatus runtimeStatus) {
        if (this.runtimeStatus != runtimeStatus) {
            this.runtimeStatus = runtimeStatus;
        }
    }

    public DiskSpace freeDiskSpace() {
        Long space = usableSpace;
        AgentRuntimeStatus status = runtimeStatus;
        return (status == AgentRuntimeStatus.Missing || status == AgentRuntimeStatus.LostContact || space == null) ? DiskSpace.unknownDiskSpace() : new DiskSpace(space);
    }

    public void updateSelf(AgentRuntimeInfo newRuntimeInfo) {
        this.buildingInfo = newRuntimeInfo.getBuildingInfo();
        if (newRuntimeInfo.isCancelled()) {
            this.setRuntimeStatus(Cancelled);
        }
        this.location = newRuntimeInfo.getLocation();
        this.usableSpace = newRuntimeInfo.getUsableSpace();
        this.operatingSystemName = newRuntimeInfo.getOperatingSystem();
        this.agentBootstrapperVersion = newRuntimeInfo.agentBootstrapperVersion;
        this.agentVersion = newRuntimeInfo.agentVersion;
    }

    public boolean isElastic() {
        return false;
    }

    public String getAgentVersion() {
        return this.agentVersion;
    }

    public String getAgentBootstrapperVersion() {
        return this.agentBootstrapperVersion;
    }
}
