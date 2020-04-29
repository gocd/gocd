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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.exception.InvalidAgentInstructionException;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.domain.AgentInstance.AgentType.LOCAL;
import static com.thoughtworks.go.domain.AgentInstance.AgentType.REMOTE;
import static com.thoughtworks.go.domain.AgentInstance.FilterBy.*;
import static com.thoughtworks.go.domain.AgentInstance.createFromLiveAgent;
import static com.thoughtworks.go.domain.AgentRuntimeStatus.Building;
import static com.thoughtworks.go.helper.AgentInstanceMother.building;
import static com.thoughtworks.go.helper.AgentInstanceMother.cancelled;
import static com.thoughtworks.go.remote.AgentInstruction.*;
import static com.thoughtworks.go.util.CommaSeparatedString.commaSeparatedStrToList;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

public class AgentInstanceTest {
    private SystemEnvironment systemEnvironment;
    public Agent agent;
    private AgentBuildingInfo defaultBuildingInfo;
    private static final String DEFAULT_IP_ADDRESS = "10.18.5.1";
    private AgentStatusChangeListener agentStatusChangeListener;
    private TimeProvider timeProvider;

    @BeforeEach
    void setUp() {
        systemEnvironment = new SystemEnvironment();
        agent = new Agent("uuid2", "CCeDev01", DEFAULT_IP_ADDRESS);
        defaultBuildingInfo = new AgentBuildingInfo("pipeline", "buildLocator");
        agentStatusChangeListener = mock(AgentStatusChangeListener.class);
        timeProvider = mock(TimeProvider.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        FileUtils.deleteQuietly(new File("config/agentkeystore"));
        new SystemEnvironment().setProperty("agent.connection.timeout", "300");
        new SystemEnvironment().clearProperty(SystemEnvironment.AGENT_SIZE_LIMIT);
    }

    @Test
    void shouldReturnBuildLocator() {
        AgentInstance building = building("buildLocator");
        assertThat(building.getBuildLocator()).isEqualTo("buildLocator");
    }

    @Test
    void shouldReturnEmptyStringForNullOperatingSystem() {
        AgentInstance building = AgentInstanceMother.missing();
        assertThat(building.getOperatingSystem()).isEqualTo("");
    }

    @Test
    void shouldReturnHumanReadableUsableSpace() {
        assertThat(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.pending(), 2 * 1024 * 1024 * 1024L).freeDiskSpace().toString()).isEqualTo("2.0 GB");
        assertThat(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.pending(), null).freeDiskSpace().toString()).isEqualTo(DiskSpace.UNKNOWN_DISK_SPACE);
    }

    @Test
    void shouldReturnUnknownUsableSpaceForMissingOrLostContactAgent() {
        assertThat(AgentInstanceMother.missing().freeDiskSpace().toString()).isEqualTo(DiskSpace.UNKNOWN_DISK_SPACE);
        assertThat(AgentInstanceMother.lostContact().freeDiskSpace().toString()).isEqualTo(DiskSpace.UNKNOWN_DISK_SPACE);
    }

    @Test
    void shouldNotifyAgentChangeListenerOnUpdate() {
        AgentInstance idleAgent = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        idleAgent.update(buildingRuntimeInfo());

        verify(agentStatusChangeListener).onAgentStatusChange(idleAgent);
    }

    @Test
    void shouldNotifyAgentChangeListenerOnAgentBuilding() {
        AgentInstance idleAgent = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        idleAgent.building(new AgentBuildingInfo("running pipeline/stage/build", "buildLocator"));

        verify(agentStatusChangeListener).onAgentStatusChange(idleAgent);
    }

    @Test
    void shouldNotifyAgentChangeListenerOnCancel() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        agentInstance.cancel();

        verify(agentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Test
    void shouldNotifyAgentChangeListenerOnRefreshAndMarkedMissing() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        agentInstance.idle();
        agentInstance.refresh();

        assertThat(agentInstance.getStatus()).isEqualTo(AgentStatus.Missing);
        verify(agentStatusChangeListener, times(2)).onAgentStatusChange(agentInstance);
    }

    @Test
    void shouldNotifyAgentChangeListenerOnRefreshAndMarkedLostContact() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, new SystemEnvironment() {
            @Override
            public int getAgentConnectionTimeout() {
                return -1;
            }
        }, agentStatusChangeListener);
        assertThat(instance.getStatus()).isEqualTo(AgentStatus.Missing);

        instance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        instance.refresh();

        assertThat(instance.getStatus()).isEqualTo(AgentStatus.LostContact);
        verify(agentStatusChangeListener, times(2)).onAgentStatusChange(instance);
    }

    @Test
    void shouldNotifyAgentChangeListenerOnEnablingAgent() {
        AgentInstance instance = AgentInstanceMother.disabled();

        AgentInstance disabledAgent = new AgentInstance(instance.agent, instance.getType(), systemEnvironment, agentStatusChangeListener);

        disabledAgent.enable();

        verify(agentStatusChangeListener).onAgentStatusChange(disabledAgent);
    }

    @Test
    void shouldNotifyAgentChangeListenerOnDisablingAgent() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        agentInstance.deny();

        verify(agentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Test
    void shouldNotifyAgentChangeListenerOnConfigSync() {
        AgentInstance instance = AgentInstanceMother.disabled();

        AgentInstance agentInstance = new AgentInstance(instance.agent, instance.getType(), systemEnvironment, agentStatusChangeListener);

        agentInstance.syncAgentFrom(agent);

        verify(agentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Test
    void shouldUpdateAgentBackToIdleAfterCancelledTaskFinishes() {
        AgentInstance cancelledAgentInstance = cancelled();

        AgentRuntimeInfo fromAgent = new AgentRuntimeInfo(cancelledAgentInstance.getAgent().getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        fromAgent.idle();
        cancelledAgentInstance.update(fromAgent);

        assertThat(cancelledAgentInstance.getStatus()).isEqualTo(AgentStatus.Idle);
    }

    @Test
    void shouldClearAllCancelledStateIfAgentSetToIdle() throws InvalidAgentInstructionException {
        Date currentTime = mock(Date.class);
        AgentInstance agentInstance = buildingWithTimeProvider(timeProvider);

        when(timeProvider.currentTime()).thenReturn(currentTime);

        agentInstance.cancel();
        agentInstance.killRunningTasks();

        assertThat(agentInstance.cancelledAt()).isEqualTo(currentTime);

        AgentRuntimeInfo fromAgent = new AgentRuntimeInfo(agentInstance.getAgent().getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        fromAgent.idle();
        agentInstance.update(fromAgent);

        assertThat(agentInstance.getStatus()).isEqualTo(AgentStatus.Idle);
        assertThat(agentInstance.cancelledAt()).isNull();
        assertThat(agentInstance.shouldKillRunningTasks()).isFalse();
    }

    @Test
    void shouldUpdateTheInstallLocation() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        String installPath = "/var/lib/GoServer";
        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        newRuntimeInfo.setLocation(installPath);
        agentInstance.update(newRuntimeInfo);

        assertThat(agentInstance.getLocation()).isEqualTo(installPath);
    }

    @Test
    void shouldUpdateTheUsableSpace() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));

        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        newRuntimeInfo.setUsableSpace(1000L);

        assertThat(agentInstance.getUsableSpace()).isNotEqualTo(newRuntimeInfo.getUsableSpace());
        agentInstance.update(newRuntimeInfo);
        assertThat(agentInstance.getUsableSpace()).isEqualTo(newRuntimeInfo.getUsableSpace());
    }

    @Test
    void shouldAssignCertificateToApprovedAgent() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));

        assertThat(agentInstance.assignCertification()).isTrue();
    }

    @Test
    void shouldNotAssignCertificateToPendingAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agent, false, "/var/lib", 0L, "linux");
        AgentInstance agentInstance = createFromLiveAgent(agentRuntimeInfo, systemEnvironment,
                mock(AgentStatusChangeListener.class));

        assertThat(agentInstance.assignCertification()).isFalse();
    }

    @Test
    void shouldInitializeTheLastHeardTimeWhenFirstPing() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        Date time = agentInstance.getLastHeardTime();
        assertThat(time).isNull();
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        time = agentInstance.getLastHeardTime();
        assertThat(time).isNotNull();
    }

    @Test
    void shouldUpdateTheLastHeardTime() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        Date time = agentInstance.getLastHeardTime();
        Thread.sleep(1000);
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        Date newtime = agentInstance.getLastHeardTime();
        assertThat(newtime.after(time)).isTrue();
    }

    @Test
    void shouldUpdateSupportBuildCommandProtocolFlag() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));

        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
    }


    @Test
    void shouldUpdateIPForPhysicalMachineWhenUpChanged() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(new AgentIdentifier("ccedev01", "10.18.7.52", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));

        assertThat(agentInstance.getAgent().getIpaddress()).isEqualTo("10.18.7.52");
    }

    @Test
    void shouldCleanBuildingInfoWhenAgentIsIdle() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(buildingRuntimeInfo());

        agentInstance.update(idleRuntimeInfo());
        assertThat(agentInstance.getBuildingInfo()).isEqualTo(AgentBuildingInfo.NOT_BUILDING);
    }

    private AgentRuntimeInfo idleRuntimeInfo() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.idle();
        return agentRuntimeInfo;
    }

    @Test
    void shouldUpdateBuildingInfoWhenAgentIsBuilding() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        AgentBuildingInfo buildingInfo = new AgentBuildingInfo("running pipeline/stage/build", "buildLocator");
        agentRuntimeInfo.busy(buildingInfo);
        agentInstance.update(agentRuntimeInfo);
        assertThat(agentInstance.getBuildingInfo()).isEqualTo(buildingInfo);
    }

    @Test
    void shouldUpdateBuildingInfoWhenAgentIsBuildingWhenCancelled() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(buildingRuntimeInfo());

        agentInstance.update(cancelRuntimeInfo());

        assertThat(agentInstance.getBuildingInfo()).isEqualTo(defaultBuildingInfo);
        assertThat(agentInstance.getStatus()).isEqualTo(AgentStatus.Cancelled);
    }

    private AgentRuntimeInfo cancelRuntimeInfo() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.busy(defaultBuildingInfo);
        agentRuntimeInfo.cancel();
        return agentRuntimeInfo;
    }

    @Test
    void shouldNotChangePendingAgentIpAddress() {
        AgentInstance pending = createFromLiveAgent(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"),
                systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("ccedev01", "10.18.7.52", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        assertThat(pending.isIpChangeRequired(info.getIpAdress())).isFalse();
    }

    @Test
    void shouldChangeIpWhenSameAgentIpChanged() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("ccedev01", "10.18.7.52", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        assertThat(instance.isIpChangeRequired(info.getIpAdress())).isTrue();
    }

    @Test
    void shouldNotChangeIpWhenIpNotChanged() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        assertThat(instance.isIpChangeRequired(DEFAULT_IP_ADDRESS)).isFalse();
    }

    @Test
    void shouldDefaultToMissingStatusWhenSyncAnApprovedAgent() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.syncAgentFrom(agent);
        assertThat(instance.getStatus()).isEqualTo(AgentStatus.Missing);
    }

    @Test
    void pendingAgentshouldNotBeRegistered() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        AgentInstance instance = createFromLiveAgent(agentRuntimeInfo, systemEnvironment,
                mock(AgentStatusChangeListener.class));
        assertThat(instance.isRegistered()).isFalse();
    }

    @Test
    void deniedAgentshouldBeRegistered() {
        agent.disable();
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));

        assertThat(instance.isRegistered()).isTrue();
    }

    @Test
    void shouldBeRegisteredForIdleAgent() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.update(idleRuntimeInfo());
        assertThat(instance.isRegistered()).isTrue();
    }

    @Test
    void shouldBecomeIdleAfterApprove() {
        AgentInstance instance = createFromLiveAgent(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"),
                systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.enable();
        instance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        assertThat(instance.getStatus()).isEqualTo(AgentStatus.Idle);
    }

    @Test
    void shouldBeMissingWhenNeverHeardFromAnyAgent() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        assertThat(instance.getStatus()).isEqualTo(AgentStatus.Missing);

        instance.refresh();
        assertThat(instance.getStatus()).isEqualTo(AgentStatus.Missing);
    }

    @Test
    void shouldBeLostContactWhenLastHeardTimeExeedTimeOut() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, new SystemEnvironment() {
            @Override
            public int getAgentConnectionTimeout() {
                return -1;
            }
        }, mock(AgentStatusChangeListener.class));
        assertThat(instance.getStatus()).isEqualTo(AgentStatus.Missing);

        instance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        instance.refresh();
        assertThat(instance.getStatus()).isEqualTo(AgentStatus.LostContact);
    }

    @Test
    void shouldRefreshDisabledAgent() {
        agent.disable();
        AgentInstance instance = AgentInstance.createFromAgent(agent, new SystemEnvironment() {
            @Override
            public int getAgentConnectionTimeout() {
                return -1;
            }
        }, mock(AgentStatusChangeListener.class));
        instance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), Building, currentWorkingDirectory(), "cookie"));

        instance.refresh();

        assertThat(instance.getRuntimeStatus()).isEqualTo(AgentRuntimeStatus.LostContact);
        assertThat(instance.getStatus()).isEqualTo(AgentStatus.Disabled);
    }

    @Test
    void shouldDenyPendingAgent() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        AgentInstance instance = createFromLiveAgent(agentRuntimeInfo, systemEnvironment,
                mock(AgentStatusChangeListener.class));
        instance.deny();

        assertThat(instance.getStatus()).isEqualTo(AgentStatus.Disabled);
    }

    @Test
    void shouldBeLiveStatus() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.update(idleRuntimeInfo());
        instance.refresh();
        assertThat(instance.getStatus()).isEqualTo(AgentStatus.Idle);
    }

    @Test
    void shouldSyncIPWithConfig() {
        AgentInstance originalAgentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));

        originalAgentInstance.update(new AgentRuntimeInfo(new AgentIdentifier("CCeDev01", "10.18.5.2", "uuid2"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));

        assertThat(originalAgentInstance.getAgent()).isEqualTo(new Agent("uuid2", "CCeDev01", "10.18.5.2"));
    }

    @Test
    void shouldKeepOriginalStatusWhenAgentIsNotDenied() {
        AgentInstance original = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        original.update(buildingRuntimeInfo(agent));

        original.syncAgentFrom(agent);
        assertThat(original.getStatus()).isEqualTo(AgentStatus.Building);
    }

    @Test
    void shouldDenyAgentWhenAgentIsDeniedInConfigFile() {
        AgentInstance original = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        original.update(buildingRuntimeInfo());

        Agent newAgent = new Agent(agent.getUuid(), agent.getHostname(), agent.getIpaddress());
        newAgent.disable();

        original.syncAgentFrom(newAgent);
        assertThat(original.getStatus()).isEqualTo(AgentStatus.Disabled);
    }

    @Test
    void shouldDenyAgentWhenItIsNotBuilding() {
        AgentInstance original = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        original.update(idleRuntimeInfo());

        original.deny();
        assertThat(agent.isDisabled()).isTrue();
        assertThat(original.getStatus()).isEqualTo(AgentStatus.Disabled);
    }

    @Test
    void shouldReturnFalseWhenAgentHasEnoughSpace() {
        AgentInstance original = AgentInstance.createFromAgent(agent, new SystemEnvironment() {
            @Override
            public long getAgentSizeLimit() {
                return 100 * 1024 * 1024;
            }
        }, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        long is110M = 110 * 1024 * 1024;
        newRuntimeInfo.setUsableSpace(is110M);
        original.update(newRuntimeInfo);

        assertThat(original.isLowDiskSpace()).isFalse();
    }

    @Test
    void shouldReturnTrueWhenFreeDiskOnAgentIsLow() {
        AgentInstance original = AgentInstance.createFromAgent(agent, new SystemEnvironment() {
            @Override
            public long getAgentSizeLimit() {
                return 100 * 1024 * 1024;
            }
        }, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        long is90M = 90 * 1024 * 1024;
        newRuntimeInfo.setUsableSpace(is90M);
        original.update(newRuntimeInfo);

        assertThat(original.isLowDiskSpace()).isTrue();
    }

    @Test
    void shouldBeAbleToDenyAgentWhenItIsBuilding() {
        AgentInstance original = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo runtimeInfo = buildingRuntimeInfo();
        original.update(runtimeInfo);
        assertThat(original.canDisable()).isTrue();
        original.deny();
        assertThat(agent.isDisabled()).isTrue();
        assertThat(original.getStatus()).isEqualTo(AgentStatus.Disabled);
        assertThat(original.getBuildingInfo()).isEqualTo(runtimeInfo.getBuildingInfo());
    }

    @Test
    void shouldOrderByHostname() {
        AgentInstance agentA = new AgentInstance(new Agent("UUID", "A", "127.0.0.1"), LOCAL, systemEnvironment, null);
        AgentInstance agentB = new AgentInstance(new Agent("UUID", "B", "127.0.0.2"), LOCAL, systemEnvironment, null);

        assertThat(agentA.compareTo(agentA)).isEqualTo(0);
        assertThat(agentA.compareTo(agentB)).isLessThan(0);
        assertThat(agentB.compareTo(agentA)).isGreaterThan(0);
    }

    @Test
    void shouldNotBeEqualIfUuidIsNotEqual() {
        AgentInstance agentA = new AgentInstance(new Agent("UUID", "A", "127.0.0.1"), LOCAL, systemEnvironment, null);
        AgentInstance copyOfAgentA = new AgentInstance(new Agent("UUID", "A", "127.0.0.1"),
                LOCAL, systemEnvironment, null);
        AgentInstance agentB = new AgentInstance(new Agent("UUID", "B", "127.0.0.2"), LOCAL, systemEnvironment, null);

        assertThat(agentA).isNotEqualTo(agentB);
        assertThat(agentB).isNotEqualTo(agentA);
        assertThat(agentA).isEqualTo(copyOfAgentA);
    }

    @Test
    void shouldBeAbleToDenyAgentThatIsRunningCancelledJob() {
        Agent agent = new Agent("UUID", "A", "127.0.0.1");
        AgentInstance agentInstance = new AgentInstance(agent, LOCAL, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.cancel();
        AgentBuildingInfo cancelled = agentInstance.getBuildingInfo();
        assertThat(agentInstance.canDisable()).isTrue();
        agentInstance.deny();
        assertThat(agent.isDisabled()).isTrue();
        assertThat(agentInstance.getStatus()).isEqualTo(AgentStatus.Disabled);
        assertThat(agentInstance.getBuildingInfo()).isEqualTo(cancelled);
    }

    @Test
    void shouldReturnNullWhenNoMatchingJobs() {
        AgentInstance agentInstance = new AgentInstance(agent("linux, mercurial"), LOCAL, systemEnvironment, null);

        JobPlan matchingJob = agentInstance.firstMatching(new ArrayList<>());
        assertThat(matchingJob).isNull();
    }

    @Test
    void shouldReturnFirstMatchingJobPlan() {
        AgentInstance agentInstance = new AgentInstance(agent("linux, mercurial"), LOCAL, systemEnvironment, null);

        List<JobPlan> plans = jobPlans("linux, svn", "linux, mercurial");
        JobPlan matchingJob = agentInstance.firstMatching(plans);
        assertThat(matchingJob).isEqualTo(plans.get(1));
    }

    @Test
    void shouldReturnAJobPlanWithMatchingUuidSet() {
        Agent agent = agent("linux, mercurial");
        AgentInstance agentInstance = new AgentInstance(agent, LOCAL, systemEnvironment, null);

        final JobPlan job = jobPlan("pipeline-name", "job-name", "resource", agent.getUuid());
        JobPlan matchingJob = agentInstance.firstMatching(new ArrayList<JobPlan>() {{
            add(job);
        }});
        assertThat(matchingJob).isEqualTo(job);
    }

    @Test
    void shouldNotReturnAJobWithMismatchedUuid() {
        Agent agent = agent("linux, mercurial");
        AgentInstance agentInstance = new AgentInstance(agent, LOCAL, systemEnvironment, null);

        final JobPlan job = jobPlan("pipeline-name", "job-name", "linux", agent.getUuid() + "-ensure-doesn't-match");
        JobPlan matchingJob = agentInstance.firstMatching(new ArrayList<JobPlan>() {{
            add(job);
        }});
        assertThat(matchingJob).isNull();
    }

    @Test
    void shouldSetAgentToIdleWhenItIsApproved() {
        AgentInstance pendingAgentInstance = AgentInstanceMother.pending();
        Agent agent = new Agent(pendingAgentInstance.getUuid(), pendingAgentInstance.getHostname(), pendingAgentInstance.getIpAddress());
        pendingAgentInstance.syncAgentFrom(agent);
        AgentStatus status = pendingAgentInstance.getStatus();
        assertThat(status).isEqualTo(AgentStatus.Idle);
    }

    @Test
    void syncConfigShouldUpdateElasticAgentRuntimeInfo() {
        AgentInstance agentInstance = AgentInstanceMother.idle();

        Agent agent = new Agent(agentInstance.getUuid(), agentInstance.getHostname(), agentInstance.getIpAddress());
        agent.setElasticAgentId("i-123456");
        agent.setElasticPluginId("com.example.aws");

        assertThat(agentInstance.isElastic()).isFalse();
        agentInstance.syncAgentFrom(agent);
        assertThat(agentInstance.isElastic()).isTrue();

        assertThat(agentInstance.elasticAgentMetadata().elasticAgentId()).isEqualTo("i-123456");
        assertThat(agentInstance.elasticAgentMetadata().elasticPluginId()).isEqualTo("com.example.aws");
    }

    @Test
    void shouldReturnFreeDiskSpace() {
        assertThat(AgentInstanceMother.updateRuntimeStatus(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.idle(new Date(), "CCeDev01"), 1024L), AgentRuntimeStatus.Missing).freeDiskSpace()).isEqualTo(DiskSpace.unknownDiskSpace());
        assertThat(AgentInstanceMother.updateRuntimeStatus(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.idle(new Date(), "CCeDev01"), 1024L), AgentRuntimeStatus.LostContact).freeDiskSpace()).isEqualTo(DiskSpace.unknownDiskSpace());
        assertThat(AgentInstanceMother.updateRuntimeStatus(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.idle(new Date(), "CCeDev01"), 1024L), AgentRuntimeStatus.Idle).freeDiskSpace()).isEqualTo(new DiskSpace(1024L));
        assertThat(AgentInstanceMother.updateRuntimeStatus(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.idle(new Date(), "CCeDev01"), null), AgentRuntimeStatus.Idle).freeDiskSpace()).isEqualTo(DiskSpace.unknownDiskSpace());

    }

    @Test
    void shouldReturnAppropriateMissingStatus() {
        AgentInstance missing = AgentInstanceMother.missing();
        assertThat(missing.isMissing()).isTrue();
        AgentInstance building = building();
        assertThat(building.isMissing()).isFalse();
    }

    @Test
    void shouldNotMatchJobPlanIfJobRequiresElasticAgent_MatchingIsManagedByBuildAssignmentService() {
        Agent agent = new Agent("uuid");
        agent.setElasticAgentId("elastic-agent-id-1");
        String elasticPluginId = "elastic-plugin-id-1";
        agent.setElasticPluginId(elasticPluginId);
        AgentInstance agentInstance = new AgentInstance(agent, REMOTE, mock(SystemEnvironment.class), null);
        DefaultJobPlan jobPlan1 = new DefaultJobPlan();
        jobPlan1.setElasticProfile(new ElasticProfile("foo", "prod-cluster"));
        List<JobPlan> jobPlans = asList(jobPlan1, new DefaultJobPlan());

        assertThat(agentInstance.firstMatching(jobPlans)).isNull();
    }

    @Test
    void shouldNotMatchJobPlanIfTheAgentWasLaunchedByADifferentPluginFromThatConfiguredForTheJob() {
        Agent agent = new Agent("uuid");
        agent.setElasticAgentId("elastic-agent-id-1");
        String elasticPluginId = "elastic-plugin-id-1";
        agent.setElasticPluginId(elasticPluginId);
        AgentInstance agentInstance = new AgentInstance(agent, REMOTE, mock(SystemEnvironment.class), null);
        DefaultJobPlan jobPlan1 = new DefaultJobPlan();
        jobPlan1.setElasticProfile(new ElasticProfile("foo", "prod-cluster"));
        List<JobPlan> jobPlans = asList(jobPlan1, new DefaultJobPlan());

        assertThat(agentInstance.firstMatching(jobPlans)).isNull();
    }

    @Test
    void shouldNotMatchJobPlanIfTheAgentIsElasticAndJobHasResourcesDefined() {
        Agent agent = new Agent("uuid", "hostname", "11.1.1.1", singletonList("r1"));
        agent.setElasticAgentId("elastic-agent-id-1");
        String elasticPluginId = "elastic-plugin-id-1";
        agent.setElasticPluginId(elasticPluginId);
        AgentInstance agentInstance = new AgentInstance(agent, REMOTE, mock(SystemEnvironment.class), null);
        DefaultJobPlan jobPlan1 = new DefaultJobPlan();
        jobPlan1.setResources(asList(new Resource("r1")));
        List<JobPlan> jobPlans = asList(jobPlan1, new DefaultJobPlan());

        assertThat(agentInstance.firstMatching(jobPlans)).isNull();
    }

    @Test
    void lostContact() {
        AgentInstance agentInstance = building();
        agentInstance.lostContact();
        assertThat(agentInstance.getStatus()).isEqualTo(AgentStatus.LostContact);

        AgentInstance pendingInstance = AgentInstanceMother.pending();
        pendingInstance.lostContact();
        assertThat(pendingInstance.getStatus()).isEqualTo(AgentStatus.Pending);

        AgentInstance disabledInstance = AgentInstanceMother.disabled();
        disabledInstance.lostContact();
        assertThat(disabledInstance.getStatus()).isEqualTo(AgentStatus.Disabled);
    }

    @Test
    void shouldNotRefreshWhenAgentStatusIsPending() {
        AgentInstance agentInstance = AgentInstanceMother.pendingInstance();
        agentInstance.refresh();
        assertThat(agentInstance.getStatus()).isEqualTo(AgentStatus.Pending);
    }

    @Test
    void shouldMarkAgentAsMissingWhenLastHeardTimeIsNull() {
        Agent agent = new Agent("1234", "localhost", "192.168.0.1");
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        agentInstance.refresh();
        assertThat(agentInstance.getRuntimeStatus()).isEqualTo(AgentRuntimeStatus.Missing);
        assertThat(agentInstance.getLastHeardTime()).isNotNull();
    }

    @Test
    void shouldNotRefreshAgentStateWhenAgentIsMissingAndLostContactDurationHasNotExceeded() {
        AgentInstance agentInstance = AgentInstanceMother.missing();
        agentInstance.refresh();
        assertThat(agentInstance.getRuntimeStatus()).isEqualTo(AgentRuntimeStatus.Missing);
    }

    @Test
    void shouldChangeAgentStatusToLostContactWhenLostAgentTimeoutHasExceeded() throws IllegalAccessException {
        AgentInstance agentInstance = AgentInstanceMother.missing();
        int agentConnectionTimeoutInMillis = systemEnvironment.getAgentConnectionTimeout() * 1000;
        Date timeLoggedForMissingStatus = new Date(new Date().getTime() - agentConnectionTimeoutInMillis);
        FieldUtils.writeField(agentInstance, "lastHeardTime", timeLoggedForMissingStatus, true);
        agentInstance.refresh();
        assertThat(agentInstance.getRuntimeStatus()).isEqualTo(AgentRuntimeStatus.LostContact);
    }

    @Test
    void buildingMethodShouldUpdateAgentRuntimeStatusToBuildingAndSetSpecifiedBuildInfoInItsRuntimeInfo(){
        Agent mockAgent = mock(Agent.class);
        AgentRuntimeInfo mockAgentRuntimeInfo = mock(AgentRuntimeInfo.class);
        AgentBuildingInfo mockAgentBuildingInfo = mock(AgentBuildingInfo.class);
        SystemEnvironment mockSysEnv = mock(SystemEnvironment.class);
        AgentStatusChangeListener mockAgentStatusChangeListener = mock(AgentStatusChangeListener.class);

        when(mockAgent.isFromLocalHost()).thenReturn(true);
        when(mockAgentRuntimeInfo.agent()).thenReturn(mockAgent);
        when(mockSysEnv.isAutoRegisterLocalAgentEnabled()).thenReturn(true);
        when(mockAgentRuntimeInfo.isCancelled()).thenReturn(false);
        doNothing().when(mockAgentRuntimeInfo).busy(mockAgentBuildingInfo);

        AgentInstance agentInstance = createFromLiveAgent(mockAgentRuntimeInfo, mockSysEnv, mockAgentStatusChangeListener);
        agentInstance.building(mockAgentBuildingInfo);

        verify(mockAgentRuntimeInfo, atLeastOnce()).getRuntimeStatus();
        verify(mockAgentRuntimeInfo).setRuntimeStatus(Building);
        verify(mockAgentRuntimeInfo).busy(mockAgentBuildingInfo);
        verify(mockAgentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Nested
    class Matches {
        @Test
        void shouldReturnTrueIfMatchesTheFilter() {
            AgentInstance pending = AgentInstanceMother.pending();
            assertThat(pending.matches(Pending)).isTrue();

            Agent pendingAgent = pending.getAgent();
            pendingAgent.setElasticAgentId("elastic-agent-id");
            pendingAgent.setElasticPluginId("elastic-plugin-id");

            pending.syncAgentFrom(pendingAgent);

            assertThat(pending.matches(Elastic)).isTrue();

            AgentInstance nullInstance = AgentInstanceMother.nullInstance();
            assertThat(nullInstance.matches(Null)).isTrue();
        }

        @Test
        void shouldReturnFalseIfDoesNotMatchTheFilter() {
            AgentInstance building = building();
            assertThat(building.matches(Pending)).isFalse();
            assertThat(building.matches(Elastic)).isFalse();

            AgentInstance pending = AgentInstanceMother.pending();
            assertThat(pending.matches(Elastic)).isFalse();
            assertThat(pending.matches(Null)).isFalse();

            AgentInstance idle = AgentInstanceMother.idle();
            Agent idleAgent = idle.getAgent();
            idleAgent.setElasticAgentId("elastic-agent-id");
            idleAgent.setElasticPluginId("elastic-plugin-id");
            idle.syncAgentFrom(idleAgent);
            assertThat(idle.matches(Pending)).isFalse();
            assertThat(idle.matches(Null)).isFalse();
        }
    }

    @Nested
    class killRunningTasks {
        @Test
        void shouldAddAKillRunningTasksInstructionToAgent() throws InvalidAgentInstructionException {
            AgentInstance agentInstance = cancelled();

            agentInstance.killRunningTasks();

            assertThat(agentInstance.shouldKillRunningTasks()).isTrue();
        }

        @Test
        void shouldErrorOutIfAddingKillRunningTasksInstructionIfAgentRuntimeStatusIsNotCancelled() {
            AgentInstance agentInstance = building();

            assertThatExceptionOfType(InvalidAgentInstructionException.class)
                    .isThrownBy(() -> agentInstance.killRunningTasks())
                    .withMessage("The agent should be in cancelled state before attempting to kill running tasks. Current Agent state is: 'Building'");
        }
    }

    @Nested
    class cancel {
        @Test
        void shouldKeepStatusAsCancelled() {
            AgentInstance buildingAgentInstance = building("buildLocator");

            buildingAgentInstance.cancel();

            buildingAgentInstance.update(buildingRuntimeInfo(buildingAgentInstance.getAgent()));

            assertThat(buildingAgentInstance.getStatus()).isEqualTo(AgentStatus.Cancelled);
        }

        @Test
        void shouldRecordTheTimeWhenAgentIsMovedToCancelState() {
            Date currentTime = mock(Date.class);
            TimeProvider timeProvider = mock(TimeProvider.class);
            AgentInstance agentInstance = buildingWithTimeProvider(timeProvider);

            when(timeProvider.currentTime()).thenReturn(currentTime);

            agentInstance.cancel();

            assertThat(agentInstance.cancelledAt()).isEqualTo(currentTime);
        }
    }

    @Nested
    class agentInstruction {
        @Test
        void shouldBeToDoNothingIfJobNotCancelled() {
            AgentInstance agentInstance = building("buildLocator");

            assertThat(agentInstance.agentInstruction()).isEqualTo(NONE);
        }

        @Test
        void shouldBeToCancelIfJobCancelled() {
            AgentInstance agentInstance = building();

            agentInstance.cancel();

            assertThat(agentInstance.agentInstruction()).isEqualTo(CANCEL);
        }

        @Test
        void shouldBeToKillRunningTasks() throws InvalidAgentInstructionException {
            AgentInstance agentInstance = building();

            agentInstance.cancel();
            agentInstance.killRunningTasks();

            assertThat(agentInstance.agentInstruction()).isEqualTo(KILL_RUNNING_TASKS);
        }
    }

    @Nested
    class isStuckInCancel {
        @Test
        void shouldBeTrueIfIsInCancelledStateForMoreThan10Mins() {
            Date currentTime = new Date();
            Date after10Mins = new Date(currentTime.getTime() + 600001);
            TimeProvider timeProvider = mock(TimeProvider.class);
            AgentInstance agentInstance = buildingWithTimeProvider(timeProvider);

            when(timeProvider.currentTime()).thenReturn(currentTime, after10Mins);

            agentInstance.cancel();

            assertThat(agentInstance.isStuckInCancel()).isTrue();
        }

        @Test
        void shouldNotBeTrueForAgentsWhichAreNotCancelled() {
            AgentInstance agentInstance = buildingWithTimeProvider(timeProvider);

            assertThat(agentInstance.isStuckInCancel()).isFalse();
        }
    }

    private List<JobPlan> jobPlans(String... resources) {
        ArrayList<JobPlan> plans = new ArrayList<>();
        int count = 1;
        for (String resource : resources) {
            plans.add(jobPlan("pipeline" + count, "job" + count, resource, null));
            count++;
        }
        return plans;
    }

    private DefaultJobPlan jobPlan(String pipelineName, String jobName, String resource, String uuid) {
        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, 1, "1", "stage1", "1", jobName, 1L);
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(resource), new ArrayList<>(), 100, jobIdentifier, null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
        plan.setAgentUuid(uuid);
        return plan;
    }

    private Agent agent(String resources) {
        return new Agent("UUID", "A", "127.0.0.1", commaSeparatedStrToList(resources));
    }

    private AgentRuntimeInfo buildingRuntimeInfo() {
        return buildingRuntimeInfo(agent);
    }

    private AgentRuntimeInfo buildingRuntimeInfo(Agent agent) {
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        runtimeInfo.busy(defaultBuildingInfo);
        return runtimeInfo;
    }

    public static AgentInstance buildingWithTimeProvider(TimeProvider timeProvider) {
        Agent idleAgentConfig = new Agent("uuid2", "localhost", "10.18.5.1");
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(idleAgentConfig.getAgentIdentifier(), Building, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.setLocation("/var/lib/foo");
        agentRuntimeInfo.idle();
        agentRuntimeInfo.setUsableSpace(10 * 1024l);
        AgentInstance agentInstance = new AgentInstance(idleAgentConfig, LOCAL, new SystemEnvironment(), mock(AgentStatusChangeListener.class), timeProvider);
        agentInstance.enable();
        return agentInstance;
    }
}
