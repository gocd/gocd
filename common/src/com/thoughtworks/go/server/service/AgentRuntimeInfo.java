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

package com.thoughtworks.go.server.service;

import java.io.File;
import java.io.Serializable;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.domain.AgentConfigStatus;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.DiskSpace;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.lang.String.format;

public class AgentRuntimeInfo implements Serializable {
    private AgentIdentifier identifier;
    private volatile AgentRuntimeStatus runtimeStatus;
    private volatile AgentBuildingInfo buildingInfo;
    private volatile String location;
    private volatile Long usableSpace;
    private static Logger LOGGER = Logger.getLogger(AgentRuntimeInfo.class);
    private volatile String operatingSystemName;
    private volatile String cookie;
    private volatile String agentLauncherVersion;

    private AgentRuntimeInfo(AgentIdentifier identifier, AgentStatus status, String location, String cookie) {
        this(identifier, status, location, cookie, null);
    }

    private AgentRuntimeInfo(AgentIdentifier identifier, AgentStatus status, String location) {
        this(identifier, status, location, null, null);
    }

    private AgentRuntimeInfo(AgentIdentifier identifier, AgentStatus status, String location, String cookie, String agentLauncherVersion) {
        this.identifier = identifier;
        this.runtimeStatus = status.getRuntimeStatus();
        this.buildingInfo = AgentBuildingInfo.NOT_BUILDING;
        this.location = location;
        this.cookie = cookie;
        this.agentLauncherVersion = agentLauncherVersion;
    }

    public static AgentRuntimeInfo fromServer(AgentConfig agentConfig, boolean registeredAlready, String location,
                                              Long usablespace, String operatingSystem) {

        if (StringUtils.isEmpty(location)) {
            throw new RuntimeException("Agent should not register without installation path.");
        }
        AgentStatus status = AgentStatus.fromConfig(AgentConfigStatus.Pending);
        if (SystemUtil.isLocalIpAddress(agentConfig.getIpAddress()) || registeredAlready) {
            status = AgentStatus.Idle;
        }

        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), status, location);
        agentRuntimeInfo.setUsableSpace(usablespace);
        agentRuntimeInfo.operatingSystemName = operatingSystem;
        return agentRuntimeInfo;
    }

    public static AgentRuntimeInfo fromAgent(AgentIdentifier agentIdentifier) {
        return fromAgent(agentIdentifier, null, null);
    }

    public static AgentRuntimeInfo fromAgent(AgentIdentifier identifier, String agentLauncherVersion) {
        return fromAgent(identifier, null, agentLauncherVersion);
    }

    public static AgentRuntimeInfo fromAgent(AgentIdentifier identifier, String cookie, String agentLauncherVersion,String workingDirectory) {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(identifier, AgentStatus.Idle, workingDirectory, cookie, agentLauncherVersion);
        agentRuntimeInfo.refresh();
        return agentRuntimeInfo;
    }

    public static AgentRuntimeInfo fromAgent(AgentIdentifier agentIdentifier, String cookie, String agentLauncherVersion) {
        return fromAgent(agentIdentifier,cookie,agentLauncherVersion,currentWorkingDirectory());
    }

    private void refresh() {
        refreshUsableSpace();
        refreshOperatingSystem();
    }

    private void refreshOperatingSystem() {
        operatingSystemName = new SystemEnvironment().getOperatingSystemName();
    }

    public static AgentRuntimeInfo initialState(AgentConfig agentConfig) {
        return new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentStatus.fromRuntime(AgentRuntimeStatus.Missing), "");
    }

    public void busy(AgentBuildingInfo agentBuildingInfo) {
        this.buildingInfo = agentBuildingInfo;
        this.runtimeStatus = AgentRuntimeStatus.Building;
    }

    public void cancel() {
        this.runtimeStatus = AgentRuntimeStatus.Cancelled;
    }

    public void idle() {
        this.runtimeStatus = AgentRuntimeStatus.Idle;
        this.buildingInfo = AgentBuildingInfo.NOT_BUILDING;
    }

    public AgentRuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    public AgentBuildingInfo getBuildingInfo() {
        return buildingInfo;
    }

    public boolean isCancelled() {
        return runtimeStatus == AgentRuntimeStatus.Cancelled;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AgentRuntimeInfo that = (AgentRuntimeInfo) o;

        if (buildingInfo != null ? !buildingInfo.equals(that.buildingInfo) : that.buildingInfo != null) {
            return false;
        }
        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) {
            return false;
        }
        if (location != null ? !location.equals(that.location) : that.location != null) {
            return false;
        }
        if (operatingSystemName != null ? !operatingSystemName.equals(that.operatingSystemName) : that.operatingSystemName != null) {
            return false;
        }
        if (hasCookie() ? !cookie.equals(that.cookie) : that.hasCookie()) {
            return false;
        }
        if (runtimeStatus != that.runtimeStatus) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (identifier != null ? identifier.hashCode() : 0);
        result = 31 * result + (runtimeStatus != null ? runtimeStatus.hashCode() : 0);
        result = 31 * result + (buildingInfo != null ? buildingInfo.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (hasCookie() ? cookie.hashCode() : 0);
        result = 31 * result + (int) (usableSpace != null ? usableSpace ^ (usableSpace >>> 32) : 0);
        return result;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getUUId() {
        return identifier.getUuid();
    }

    public AgentConfig agent() {
        return new AgentConfig(getUUId(), identifier.getHostName(), identifier.getIpAddress());
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


    public void refreshUsableSpace() {
        setUsableSpace(usableSpace(location));
    }

    public static long usableSpace(String currentWorkingDir) {
        File file = new File(currentWorkingDir, "pipelines");
        if (!file.exists()) {
            LOGGER.warn(
                    "the [" + file.getAbsolutePath() + "] should be created when agent starts up, "
                            + "but it seems missing at the moment. "
                            + "Cruise should be able to automatically create it later");
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
        this.buildingInfo = AgentBuildingInfo.NOT_BUILDING;
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

    public void setRuntimeStatus(AgentRuntimeStatus runtimeStatus, AgentRuntimeStatus.ChangeListener changeListener) {
        if (this.runtimeStatus != runtimeStatus) {
            if (changeListener != null) {
                changeListener.statusUpdateRequested(this, runtimeStatus);
            }
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
            this.setRuntimeStatus(AgentRuntimeStatus.Cancelled, null);
        }
        this.location = newRuntimeInfo.getLocation();
        this.usableSpace = newRuntimeInfo.getUsableSpace();
        this.operatingSystemName = newRuntimeInfo.getOperatingSystem();
        this.agentLauncherVersion = newRuntimeInfo.getAgentLauncherVersion();
    }

    public String getAgentLauncherVersion() {
        return agentLauncherVersion;
    }

    public void setAgentLauncherVersion(String agentLauncherVersion) {
        this.agentLauncherVersion = agentLauncherVersion;
    }
}
