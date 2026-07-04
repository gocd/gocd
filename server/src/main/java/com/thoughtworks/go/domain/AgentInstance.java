/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.domain.exception.InvalidAgentInstructionException;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.thoughtworks.go.domain.AgentConfigStatus.*;
import static com.thoughtworks.go.domain.AgentRuntimeStatus.*;
import static com.thoughtworks.go.domain.AgentStatus.fromConfig;
import static com.thoughtworks.go.domain.AgentStatus.fromRuntime;
import static com.thoughtworks.go.remote.AgentInstruction.*;
import static java.lang.String.format;

/**
 * Understands runtime and configuration information of a builder machine
 */
public class AgentInstance implements Comparable<AgentInstance> {
    private final AgentType agentType;
    private final AgentStatusChangeListener agentStatusChangeListener;
    private final TimeProvider timeProvider;
    private final SystemEnvironment systemEnvironment;
    private final ConfigErrors errors = new ConfigErrors();

    private @NotNull Agent agent;
    private @NotNull AgentRuntimeInfo agentRuntimeInfo;
    private final @NotNull AtomicReference<AgentConfigStatus> agentConfigStatus;

    private final AtomicBoolean killRunningTasks = new AtomicBoolean(false);
    private volatile @Nullable Instant lastHeardTime;
    private volatile @Nullable Instant cancelledAt;

    @VisibleForTesting
    AgentInstance(@NotNull Agent agent, AgentType agentType, SystemEnvironment systemEnvironment,
                  AgentStatusChangeListener agentStatusChangeListener, TimeProvider timeProvider) {
        this.systemEnvironment = systemEnvironment;
        this.agentRuntimeInfo = AgentRuntimeInfo.initialState(agent);
        this.agentStatusChangeListener = agentStatusChangeListener;
        this.agentConfigStatus = new AtomicReference<>(Pending);
        this.agent = agent;
        this.agentType = agentType;
        this.timeProvider = timeProvider;
    }

    AgentInstance(@NotNull Agent agent, AgentType agentType, SystemEnvironment systemEnvironment,
                            AgentStatusChangeListener agentStatusChangeListener) {
        this(agent, agentType, systemEnvironment, agentStatusChangeListener, new TimeProvider());
    }

    AgentInstance(@NotNull Agent agent, AgentType agentType, SystemEnvironment systemEnvironment,
                            AgentStatusChangeListener agentStatusChangeListener, AgentRuntimeInfo agentRuntimeInfo) {
        this(agent, agentType, systemEnvironment, agentStatusChangeListener);
        this.agentRuntimeInfo = agentRuntimeInfo;
    }

    public String getHostname() {
        return agent.getHostname();
    }

    public String getUuid() {
        return agent.getUuid();
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

    public void syncAgentFrom(@NotNull Agent agent) {
        this.agent = agent;

        if (agent.isElastic()) {
            agentRuntimeInfo = ElasticAgentRuntimeInfo.fromServer(agentRuntimeInfo, agent.getElasticAgentId(), agent.getElasticPluginId());
        }

        if (agentRuntimeInfo.getRuntimeStatus() == AgentRuntimeStatus.Unknown) {
            agentRuntimeInfo.idle();
        }

        notifyConfigStatus(agent.isDisabled() ? Disabled : Enabled);
    }

    private void syncRuntimeStatus(AgentRuntimeStatus runtimeStatus) {
        if (runtimeStatus == Idle) {
            updateRuntimeStatus(Idle);
            agentRuntimeInfo.clearBuildingInfo();
            clearCancelledState();
        } else if (!agentRuntimeInfo.isCancelled()) {
            updateRuntimeStatus(runtimeStatus);
        }
    }

    public void building(AgentBuildingInfo agentBuildingInfo) {
        syncRuntimeStatus(Building);
        agentRuntimeInfo.busy(agentBuildingInfo);
    }

    @TestOnly
    public void idle() {
        agentConfigStatus.set(Enabled);
        syncRuntimeStatus(Idle);
        agentRuntimeInfo.clearBuildingInfo();
    }

    public void pending() {
        agentConfigStatus.set(Pending);
        agentRuntimeInfo.clearBuildingInfo();
    }

    public void enable() {
        notifyConfigStatus(Enabled);
        agentRuntimeInfo.clearBuildingInfo();
    }

    public void cancel() {
        updateRuntimeStatus(AgentRuntimeStatus.Cancelled);
        cancelledAt = timeProvider.currentTime();
    }

    public void killRunningTasks() throws InvalidAgentInstructionException {
        if (!isCancelled()) {
            throw new InvalidAgentInstructionException(format("The agent should be in cancelled state before attempting " +
                "to kill running tasks. Current Agent state is: '%s'", agentRuntimeInfo.getRuntimeStatus().buildState().name()));
        }

        if (!killRunningTasks.compareAndSet(false, true)) {
            throw new InvalidAgentInstructionException("There is a pending request to kill running task.");
        }
    }

    public void deny() {
        if (!canDisable()) {
            throw new RuntimeException("Should not deny agent when is building.");
        }
        getAgent().disable();
        notifyConfigStatus(Disabled);
    }

    public AgentStatus getStatus() {
        return fromConfig(agentConfigStatus.get())
            .orElseGet(() -> fromRuntime(agentRuntimeInfo.getRuntimeStatus()));
    }

    public @NotNull AgentConfigStatus getAgentConfigStatus() {
        return agentConfigStatus.get();
    }

    public AgentBuildingInfo getBuildingInfo() {
        return agentRuntimeInfo.getBuildingInfo();
    }

    public @NotNull Agent getAgent() {
        return agent;
    }

    public @Nullable Instant getLastHeardTime() {
        return lastHeardTime;
    }

    public boolean canDisable() {
        return agentConfigStatus.get() != Disabled;
    }

    public void refresh() {
        if (isPending()) {
            return;
        }

        if (lastHeardTime == null) {
            updateRuntimeStatus(Missing);
            lastHeardTime = timeProvider.currentTime();
        }

        if (isTimeout(lastHeardTime)) {
            updateRuntimeStatus(LostContact);
        }
    }

    public void lostContact() {
        if (agentConfigStatus.get() != Enabled) {
            return;
        }
        updateRuntimeStatus(LostContact);
    }

    boolean isTimeout(Instant lastHeardTime) {
        return lastHeardTime != null && Duration.between(lastHeardTime, timeProvider.currentTime()).compareTo(systemEnvironment.getAgentConnectionTimeout()) >= 0;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public AgentType getType() {
        return agentType;
    }

    public void update(AgentRuntimeInfo newRuntimeInfo) {
        syncRuntimeStatus(newRuntimeInfo.getRuntimeStatus());
        syncIp(newRuntimeInfo);
        this.lastHeardTime = timeProvider.currentTime();
        this.agentRuntimeInfo.updateSelf(newRuntimeInfo);
    }

    private void syncIp(AgentRuntimeInfo info) {
        String ipAddress = agentType == AgentType.LOCAL || agentType == AgentType.REMOTE ? info.getIpAddress() : agent.getIpaddress();
        this.agent.setIpaddress(ipAddress);
    }

    public boolean isIpChangeRequired(String newIpAddress) {
        return !Strings.CS.equals(this.agent.getIpaddress(), newIpAddress) && this.isRegistered();
    }

    public String getLocation() {
        return agentRuntimeInfo.getLocation();
    }

    public boolean isRegistered() {
        return !isPending();
    }

    public boolean isPending() {
        return agentConfigStatus.get() == Pending;
    }

    public boolean isDisabled() {
        return agent.isDisabled();
    }

    public boolean isIdle() {
        return agentRuntimeInfo.getRuntimeStatus() == Idle;
    }

    public boolean isBuilding() {
        return agentRuntimeInfo.getRuntimeStatus() == Building;
    }

    public boolean isCancelled() {
        return agentRuntimeInfo.getRuntimeStatus() == AgentRuntimeStatus.Cancelled;
    }

    public boolean isMissing() {
        return agentRuntimeInfo.getRuntimeStatus() == Missing;
    }

    public boolean shouldKillRunningTasks() {
        return killRunningTasks.get();
    }

    public AgentIdentifier getAgentIdentifier() {
        return agent.getAgentIdentifier();
    }

    @TestOnly
    public Stream<String> getResourceNames() {
        return agent.getResourcesAsStream();
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
        return !jobPlan.requiresElasticAgent() && !isElastic() && agent.hasAllResources(jobPlan.getResources().toResourceConfigs().resourceNames());
    }

    private void clearCancelledState() {
        this.cancelledAt = null;
        this.killRunningTasks.set(false);
    }

    public String getBuildLocator() {
        return agentRuntimeInfo.getBuildingInfo().getBuildLocator();
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

    public boolean matches(FilterBy filter) {
        return switch (filter) {
            case Pending -> isPending();
            case Elastic -> isElastic();
            case Null -> isNullAgent();
        };
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }

    public boolean isStuckInCancel() {
        Instant cancelled = cancelledAt;
        return isCancelled() && cancelled != null && Duration.between(cancelled, timeProvider.currentTime()).compareTo(Duration.ofMinutes(10)) > 0;
    }

    public AgentInstruction agentInstruction() {
        if (isCancelled() && shouldKillRunningTasks()) {
            return KILL_RUNNING_TASKS;
        }

        if (isCancelled()) {
            return CANCEL;
        }

        return NONE;
    }

    public enum AgentType {
        LOCAL, REMOTE
    }

    public static AgentInstance createFromAgent(Agent agent, SystemEnvironment systemEnvironment,
                                                AgentStatusChangeListener agentStatusChangeListener) {
        AgentType type = agent.isFromLocalHost() ? AgentType.LOCAL : AgentType.REMOTE;
        AgentInstance agentInstance = new AgentInstance(agent, type, systemEnvironment, agentStatusChangeListener);
        agentInstance.agentConfigStatus.set(agent.isDisabled() ? Disabled : Enabled);
        agentInstance.errors.addAll(agent.errors());

        return agentInstance;
    }

    public static AgentInstance createFromLiveAgent(AgentRuntimeInfo agentRuntimeInfo, SystemEnvironment sysEnv,
                                                    AgentStatusChangeListener agentStatusChangeListener) {
        Agent agent = agentRuntimeInfo.agent();
        AgentType type = agent.isFromLocalHost() ? AgentType.LOCAL : AgentType.REMOTE;
        AgentInstance instance;
        if (sysEnv.isAutoRegisterLocalAgentEnabled() && agent.isFromLocalHost()) {
            instance = new AgentInstance(agent, type, sysEnv, agentStatusChangeListener, agentRuntimeInfo);
            instance.agentConfigStatus.set(Enabled);
            instance.agentRuntimeInfo.idle();
            instance.update(agentRuntimeInfo);
            return instance;
        } else {
            instance = new AgentInstance(agent, type, sysEnv, agentStatusChangeListener);
            instance.update(agentRuntimeInfo);
        }
        return instance;
    }

    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AgentInstance that = (AgentInstance) o;
        return
            agent.equals(that.agent) &&
            agentRuntimeInfo.equals(that.agentRuntimeInfo) &&
            agentConfigStatus.get().equals(that.agentConfigStatus.get()) &&
            agentType == that.agentType &&
            Objects.equals(lastHeardTime, that.lastHeardTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agent, agentRuntimeInfo, agentConfigStatus, agentType, lastHeardTime);
    }

    public boolean canRemove() {
        return isPending() && isTimeout(lastHeardTime);
    }

    public String getOperatingSystem() {
        String os = agentRuntimeInfo.getOperatingSystem();
        return os == null ? "" : os;
    }

    public boolean isLowDiskSpace() {
        long limit = new SystemEnvironment().getAgentSizeLimitBytes();
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

    public String getAgentVersion() {
        return agentRuntimeInfo.getAgentVersion();
    }

    public String getAgentBootstrapperVersion() {
        return agentRuntimeInfo.getAgentBootstrapperVersion();
    }

    private void notifyConfigStatus(AgentConfigStatus agentConfigStatus) {
        if (!this.agentConfigStatus.getAndSet(agentConfigStatus).equals(agentConfigStatus) ) {
            notifyStatusChange();
        }
    }

    private void notifyStatusChange() {
        if (this.isRegistered()) {
            this.agentStatusChangeListener.onAgentStatusChange(this);
        }
    }

    public enum FilterBy {
        Pending,
        Elastic,
        Null
    }
}
