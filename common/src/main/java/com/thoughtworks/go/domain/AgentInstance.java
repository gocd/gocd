/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.config.ResourceConfigs;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;
import java.util.List;

//TODO put the logic back to the AgentRuntimeInfo for all the sync method

/**
 * @understands runtime and configuration information of a builder machine
 */
public class AgentInstance implements Comparable<AgentInstance> {
    private AgentType agentType;
    protected Agent agent;
    private AgentRuntimeInfo agentRuntimeInfo;
    private AgentStatusChangeListener agentStatusChangeListener;

    private AgentConfigStatus agentConfigStatus;

    private volatile Date lastHeardTime;
    private TimeProvider timeProvider;
    private SystemEnvironment systemEnvironment;
    private ConfigErrors errors = new ConfigErrors();

    protected AgentInstance(Agent agent, AgentType agentType, SystemEnvironment systemEnvironment,
                            AgentStatusChangeListener agentStatusChangeListener) {
        this.systemEnvironment = systemEnvironment;
        this.agentRuntimeInfo = AgentRuntimeInfo.initialState(agent);
        this.agentStatusChangeListener = agentStatusChangeListener;
        this.agentConfigStatus = AgentConfigStatus.Pending;
        this.agent = agent;
        this.agentType = agentType;
        this.timeProvider = new TimeProvider();
    }

    protected AgentInstance(Agent agent, AgentType agentType, SystemEnvironment systemEnvironment,
                            AgentStatusChangeListener agentStatusChangeListener, AgentRuntimeInfo agentRuntimeInfo) {
        this(agent, agentType, systemEnvironment, agentStatusChangeListener);
        this.agentRuntimeInfo = agentRuntimeInfo;
    }

    public String getHostname() {
        return getAgent().getHostname();
    }

    public String getUuid() {
        return getAgent().getUuid();
    }

    @Override
    public int compareTo(AgentInstance other) {
        int comparison = this.getHostname().compareTo(other.getHostname());
        if (comparison == 0) {
            comparison = this.getLocation().compareTo(other.getLocation());
        }
        if (comparison == 0) {
            comparison = this.getUuid().compareTo(other.getUuid());
        }
        return comparison;
    }

    public void syncConfig(Agent agent) {
        this.agent = agent;

        if (agent.isElastic()) {
            agentRuntimeInfo = ElasticAgentRuntimeInfo.fromServer(agentRuntimeInfo, agent.getElasticAgentId(), agent.getElasticPluginId());
        }

        if (agentRuntimeInfo.getRuntimeStatus() == AgentRuntimeStatus.Unknown) {
            agentRuntimeInfo.idle();
        }

        updateConfigStatus(agent.isDisabled() ? AgentConfigStatus.Disabled : AgentConfigStatus.Enabled);
    }

    private void syncStatus(AgentRuntimeStatus runtimeStatus) {
        if (runtimeStatus == AgentRuntimeStatus.Idle) {
            updateRuntimeStatus(AgentRuntimeStatus.Idle);
            agentRuntimeInfo.clearBuildingInfo();
        } else if (!(agentRuntimeInfo.isCancelled())) {
            updateRuntimeStatus(runtimeStatus);
        }
    }

    public void building(AgentBuildingInfo agentBuildingInfo) {
        syncStatus(AgentRuntimeStatus.Building);
        agentRuntimeInfo.busy(agentBuildingInfo);
    }

    //  Used only in tests
    public void idle() {
        agentConfigStatus = AgentConfigStatus.Enabled;
        syncStatus(AgentRuntimeStatus.Idle);
        agentRuntimeInfo.clearBuildingInfo();
    }

    public void pending() {
        agentConfigStatus = AgentConfigStatus.Pending;
        agentRuntimeInfo.clearBuildingInfo();
    }

    public void enable() {
        updateConfigStatus(AgentConfigStatus.Enabled);
        agentRuntimeInfo.clearBuildingInfo();
    }

    public void cancel() {
        updateRuntimeStatus(AgentRuntimeStatus.Cancelled);
    }

    public void deny() {
        if (!canDisable()) {
            throw new RuntimeException("Should not deny agent when is building.");
        }
        getAgent().disable();
        updateConfigStatus(AgentConfigStatus.Disabled);
    }

    public AgentStatus getStatus() {
        return agentConfigStatus == AgentConfigStatus.Enabled
                ? AgentStatus.fromRuntime(agentRuntimeInfo.getRuntimeStatus())
                : AgentStatus.fromConfig(agentConfigStatus);
    }

    public AgentConfigStatus getAgentConfigStatus() {
        return agentConfigStatus;
    }

    public AgentBuildingInfo getBuildingInfo() {
        return agentRuntimeInfo.getBuildingInfo();
    }

    public Agent getAgent() {
        return agent;
    }

    public Date getLastHeardTime() {
        return lastHeardTime;
    }

    public boolean canDisable() {
        return agentConfigStatus != AgentConfigStatus.Disabled;
    }

    /**
     * Used only from the old ui. New ui does not have the notion of "approved" agents only "enabled" ones.
     *
     * @deprecated
     */
    public boolean canApprove() {
        return agentConfigStatus != AgentConfigStatus.Enabled && !agent.isDisabled();
    }

    public void refresh() {
        if (agentConfigStatus == AgentConfigStatus.Pending) {
            return;
        }

        if (lastHeardTime == null) {
            updateRuntimeStatus(AgentRuntimeStatus.Missing);
            lastHeardTime = new Date();
        }

        if (isTimeout(lastHeardTime)) {
            updateRuntimeStatus(AgentRuntimeStatus.LostContact);
        }
    }

    public void lostContact() {
        if (agentConfigStatus == AgentConfigStatus.Pending || agentConfigStatus == AgentConfigStatus.Disabled) {
            return;
        }
        updateRuntimeStatus(AgentRuntimeStatus.LostContact);
    }

    boolean isTimeout(Date lastHeardTime) {
        return (timeProvider.currentTime().getTime() - lastHeardTime.getTime()) / 1000 >= systemEnvironment.getAgentConnectionTimeout();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public Registration assignCertification() {
        if (AgentConfigStatus.Pending.equals(agentConfigStatus)) {
            return Registration.createNullPrivateKeyEntry();
        }
        X509CertificateGenerator certificateGenerator = new X509CertificateGenerator();
        Registration entry = certificateGenerator.createAgentCertificate(new SystemEnvironment().agentkeystore(), agent.getHostname());
        return new Registration(entry.getPrivateKey(), entry.getChain());
    }

    public AgentType getType() {
        return agentType;
    }

    public void update(AgentRuntimeInfo newRuntimeInfo) {
        syncStatus(newRuntimeInfo.getRuntimeStatus());
        syncIp(newRuntimeInfo);
        this.lastHeardTime = new Date();
        this.agentRuntimeInfo.updateSelf(newRuntimeInfo);
    }

    private void syncIp(AgentRuntimeInfo info) {
        String ipAddress = (agentType == AgentType.LOCAL || agentType == AgentType.REMOTE) ? info.getIpAdress() : agent.getIpaddress();
        this.agent.setIpaddress(ipAddress);
    }

    public boolean isIpChangeRequired(String newIpAdress) {
        return !StringUtils.equals(this.agent.getIpaddress(), newIpAdress) && this.isRegistered();
    }

    public String getLocation() {
        return agentRuntimeInfo.getLocation();
    }


    /**
     * @deprecated use freeDiskSpace instead
     */
    public Long getUsableSpace() {
        return agentRuntimeInfo.getUsableSpace();
    }

    public boolean isRegistered() {
        return !isPending();
    }

    public boolean isPending() {
        return agentConfigStatus == AgentConfigStatus.Pending;
    }

    public boolean isDisabled() {
        return getAgent().isDisabled();
    }

    public boolean isIdle() {
        return agentRuntimeInfo.getRuntimeStatus() == AgentRuntimeStatus.Idle;
    }

    public boolean isBuilding() {
        return agentRuntimeInfo.getRuntimeStatus() == AgentRuntimeStatus.Building;
    }

    public boolean isCancelled() {
        return agentRuntimeInfo.getRuntimeStatus() == AgentRuntimeStatus.Cancelled;
    }

    public boolean isMissing() {
        return agentRuntimeInfo.getRuntimeStatus() == AgentRuntimeStatus.Missing;
    }

    public AgentIdentifier getAgentIdentifier() {
        return getAgent().getAgentIdentifier();
    }

    public ResourceConfigs getResourceConfigs() {
        return getAgent().getResourceConfigs();
    }

    public String getIpAddress() {
        return agent.getIpaddress();
    }

    public JobPlan firstMatching(List<JobPlan> jobPlans) {
        for (JobPlan jobPlan : jobPlans) {
            if (jobPlan.requiresElasticAgent()) {
                continue;
            }
            if (jobPlan.assignedToAgent() && isNotElasticAndResourcesMatchForNonElasticAgents(jobPlan)) {
                return jobPlan;
            } else {
                if (agent.getUuid().equals(jobPlan.getAgentUuid())) {
                    return jobPlan;
                }
            }
        }
        return null;
    }

    private boolean isNotElasticAndResourcesMatchForNonElasticAgents(JobPlan jobPlan) {
        return !jobPlan.requiresElasticAgent() && !isElastic() && agent.hasAllResources(jobPlan.getResources().toResourceConfigs());
    }

    public String getBuildLocator() {
        return agentRuntimeInfo.getBuildingInfo().getBuildLocator();
    }

    public boolean canEnable() {
        return agentConfigStatus != AgentConfigStatus.Enabled;
    }

    public DiskSpace freeDiskSpace() {
        return agentRuntimeInfo.freeDiskSpace();
    }

    public AgentRuntimeStatus getRuntimeStatus() {
        return agentRuntimeInfo.getRuntimeStatus();
    }

    public String agentInfoForDisplay() {
        return agentRuntimeInfo.agentInfoForDisplay();
    }

    public boolean isElastic() {
        return agentRuntimeInfo.isElastic();
    }

    public ElasticAgentMetadata elasticAgentMetadata() {
        ElasticAgentRuntimeInfo runtimeInfo = (ElasticAgentRuntimeInfo) this.agentRuntimeInfo;
        return new ElasticAgentMetadata(getUuid(), runtimeInfo.getElasticAgentId(), runtimeInfo.getElasticPluginId(), this.agentRuntimeInfo.getRuntimeStatus(), getAgentConfigStatus());
    }

    public boolean canBeDeleted() {
        return isDisabled() && !(isBuilding() || isCancelled());
    }

    enum AgentType {
        LOCAL, REMOTE
    }

    public static AgentInstance createFromAgent(Agent agent, SystemEnvironment systemEnvironment,
                                                AgentStatusChangeListener agentStatusChangeListener) {
        AgentType type = agent.isFromLocalHost() ? AgentType.LOCAL : AgentType.REMOTE;
        AgentInstance result = new AgentInstance(agent, type, systemEnvironment, agentStatusChangeListener);
        result.agentConfigStatus = agent.isDisabled() ? AgentConfigStatus.Disabled : AgentConfigStatus.Enabled;

        result.errors.addAll(agent.errors());
        for (ResourceConfig resourceConfig : agent.getResourceConfigs()) {
            result.errors.addAll(resourceConfig.errors());
        }
        return result;
    }

    public static AgentInstance createFromLiveAgent(AgentRuntimeInfo agentRuntimeInfo, SystemEnvironment systemEnvironment,
                                                    AgentStatusChangeListener agentStatusChangeListener) {
        Agent agent = agentRuntimeInfo.agent();
        AgentType type = agent.isFromLocalHost() ? AgentType.LOCAL : AgentType.REMOTE;
        AgentInstance instance;
        if (systemEnvironment.isAutoRegisterLocalAgentEnabled() && agent.isFromLocalHost()) {
            instance = new AgentInstance(agent, type, systemEnvironment, agentStatusChangeListener, agentRuntimeInfo);
            instance.agentConfigStatus = AgentConfigStatus.Enabled;
            instance.agentRuntimeInfo.idle();
            instance.update(agentRuntimeInfo);
            return instance;
        } else {
            instance = new AgentInstance(agent, type, systemEnvironment, agentStatusChangeListener);
            instance.update(agentRuntimeInfo);
        }
        return instance;
    }

    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }

        return equals((AgentInstance) that);
    }

    private boolean equals(AgentInstance that) {
        if (this.agent == null ? that.agent != null : !this.agent.equals(that.agent)) {
            return false;
        }
        if (this.agentRuntimeInfo == null ? that.agentRuntimeInfo != null : !this.agentRuntimeInfo.equals(that.agentRuntimeInfo)) {
            return false;
        }
        if (this.agentConfigStatus != that.agentConfigStatus) {
            return false;
        }
        if (this.agentType != that.agentType) {
            return false;
        }
        if (this.lastHeardTime == null ? that.lastHeardTime != null : !this.lastHeardTime.equals(that.lastHeardTime)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (agent != null ? agent.hashCode() : 0);
        result = 31 * result + (agentType != null ? agentType.hashCode() : 0);
        result = 31 * result + (agentRuntimeInfo != null ? agentRuntimeInfo.hashCode() : 0);
        result = 31 * result + (lastHeardTime != null ? lastHeardTime.hashCode() : 0);
        result = 31 * result + (timeProvider != null ? timeProvider.hashCode() : 0);
        result = 31 * result + (agentConfigStatus != null ? agentConfigStatus.hashCode() : 0);
        return result;
    }

    public void checkForRemoval(List<AgentInstance> agentsToRemove) {
        if (agentConfigStatus == AgentConfigStatus.Pending && isTimeout(lastHeardTime)) {
            agentsToRemove.add(this);
        }
    }

    public String getOperatingSystem() {
        String os = agentRuntimeInfo.getOperatingSystem();
        return os == null ? "" : os;
    }

    public boolean isLowDiskSpace() {
        long limit = new SystemEnvironment().getAgentSizeLimit();
        return agentRuntimeInfo.isLowDiskSpace(limit);
    }

    public boolean isNullAgent() {
        return false;
    }

    private void updateRuntimeStatus(AgentRuntimeStatus agentRuntimeStatus) {
        if (this.agentRuntimeInfo.getRuntimeStatus() != agentRuntimeStatus) {
            this.agentRuntimeInfo.setRuntimeStatus(agentRuntimeStatus);
            notifyStatusChange();
        }
    }

    private void updateConfigStatus(AgentConfigStatus agentConfigStatus) {
        if (this.agentConfigStatus != agentConfigStatus) {
            this.agentConfigStatus = agentConfigStatus;
            notifyStatusChange();
        }
    }

    private void notifyStatusChange() {
        if (this.isRegistered()) {
            this.agentStatusChangeListener.onAgentStatusChange(this);
        }
    }
}
