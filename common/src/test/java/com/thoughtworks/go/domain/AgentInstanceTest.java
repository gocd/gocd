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
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
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
import static com.thoughtworks.go.util.CommaSeparatedString.commaSeparatedStrToList;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AgentInstanceTest {
    private SystemEnvironment systemEnvironment;
    public Agent agent;
    public AgentBuildingInfo defaultBuildingInfo;
    private static final String DEFAULT_IP_ADDRESS = "10.18.5.1";
    private AgentStatusChangeListener agentStatusChangeListener;

    @BeforeEach
    public void setUp() {
        systemEnvironment = new SystemEnvironment();
        agent = new Agent("uuid2", "CCeDev01", DEFAULT_IP_ADDRESS);
        defaultBuildingInfo = new AgentBuildingInfo("pipeline", "buildLocator");
        agentStatusChangeListener = mock(AgentStatusChangeListener.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(new File("config/agentkeystore"));
        new SystemEnvironment().setProperty("agent.connection.timeout", "300");
        new SystemEnvironment().clearProperty(SystemEnvironment.AGENT_SIZE_LIMIT);
    }

    @Test
    public void shouldReturnBuildLocator() {
        AgentInstance building = AgentInstanceMother.building("buildLocator");
        assertThat(building.getBuildLocator(), is("buildLocator"));
    }

    @Test
    public void shouldReturnEmptyStringForNullOperatingSystem() {
        AgentInstance building = AgentInstanceMother.missing();
        assertThat(building.getOperatingSystem(), is(""));
    }

    @Test
    public void shouldReturnHumanReadableUsableSpace() {
        assertThat(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.pending(), 2 * 1024 * 1024 * 1024L).freeDiskSpace().toString(), is("2.0 GB"));
        assertThat(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.pending(), null).freeDiskSpace().toString(), is(DiskSpace.UNKNOWN_DISK_SPACE));
    }

    @Test
    public void shouldReturnUnknownUsableSpaceForMissingOrLostContactAgent() {
        assertThat(AgentInstanceMother.missing().freeDiskSpace().toString(), is(DiskSpace.UNKNOWN_DISK_SPACE));
        assertThat(AgentInstanceMother.lostContact().freeDiskSpace().toString(), is(DiskSpace.UNKNOWN_DISK_SPACE));
    }

    @Test
    public void shouldKeepStatusAsCancelled() {
        AgentInstance buildingAgentInstance = AgentInstanceMother.building("buildLocator");
        buildingAgentInstance.cancel();

        buildingAgentInstance.update(buildingRuntimeInfo(buildingAgentInstance.getAgent()));

        assertThat(buildingAgentInstance.getStatus(), is(AgentStatus.Cancelled));
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnUpdate() {
        AgentInstance idleAgent = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        idleAgent.update(buildingRuntimeInfo());

        verify(agentStatusChangeListener).onAgentStatusChange(idleAgent);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnAgentBuilding() {
        AgentInstance idleAgent = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        idleAgent.building(new AgentBuildingInfo("running pipeline/stage/build", "buildLocator"));

        verify(agentStatusChangeListener).onAgentStatusChange(idleAgent);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnCancel() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        agentInstance.cancel();

        verify(agentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnRefreshAndMarkedMissing() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        agentInstance.idle();
        agentInstance.refresh();

        assertThat(agentInstance.getStatus(), is(AgentStatus.Missing));
        verify(agentStatusChangeListener, times(2)).onAgentStatusChange(agentInstance);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnRefreshAndMarkedLostContact() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, new SystemEnvironment() {
            @Override
            public int getAgentConnectionTimeout() {
                return -1;
            }
        }, agentStatusChangeListener);
        assertThat(instance.getStatus(), is(AgentStatus.Missing));

        instance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        instance.refresh();

        assertThat(instance.getStatus(), is(AgentStatus.LostContact));
        verify(agentStatusChangeListener, times(2)).onAgentStatusChange(instance);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnEnablingAgent() {
        AgentInstance instance = AgentInstanceMother.disabled();

        AgentInstance disabledAgent = new AgentInstance(instance.agent, instance.getType(), systemEnvironment, agentStatusChangeListener);

        disabledAgent.enable();

        verify(agentStatusChangeListener).onAgentStatusChange(disabledAgent);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnDisablingAgent() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent("abc"), new SystemEnvironment(), agentStatusChangeListener);

        agentInstance.deny();

        verify(agentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnConfigSync() {
        AgentInstance instance = AgentInstanceMother.disabled();

        AgentInstance agentInstance = new AgentInstance(instance.agent, instance.getType(), systemEnvironment, agentStatusChangeListener);

        agentInstance.syncConfig(agent);

        verify(agentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Test
    public void shouldUpdateAgentBackToIdleAfterCancelledTaskFinishes() {
        AgentInstance cancelledAgentInstance = AgentInstanceMother.cancelled();

        AgentRuntimeInfo fromAgent = new AgentRuntimeInfo(cancelledAgentInstance.getAgent().getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        fromAgent.idle();
        cancelledAgentInstance.update(fromAgent);

        assertThat(cancelledAgentInstance.getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldUpdateTheInstallLocation() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        String installPath = "/var/lib/GoServer";
        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        newRuntimeInfo.setLocation(installPath);
        agentInstance.update(newRuntimeInfo);

        assertThat(agentInstance.getLocation(), is(installPath));
    }

    @Test
    public void shouldUpdateTheUsableSpace() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));

        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        newRuntimeInfo.setUsableSpace(1000L);

        assertThat(agentInstance.getUsableSpace(), is(not(newRuntimeInfo.getUsableSpace())));
        agentInstance.update(newRuntimeInfo);
        assertThat(agentInstance.getUsableSpace(), is(newRuntimeInfo.getUsableSpace()));
    }

    @Test
    public void shouldAssignCertificateToApprovedAgent() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));

        Registration entry = agentInstance.assignCertification();
        assertThat(entry.getChain().length, is(not(0)));
    }

    @Test
    public void shouldNotAssignCertificateToPendingAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agent, false, "/var/lib", 0L, "linux");
        AgentInstance agentInstance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, systemEnvironment,
                mock(AgentStatusChangeListener.class));

        Registration entry = agentInstance.assignCertification();
        assertThat(entry.getChain().length, is(0));
    }


    @Test
    public void shouldInitializeTheLastHeardTimeWhenFirstPing() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        Date time = agentInstance.getLastHeardTime();
        assertThat(time, is(nullValue()));
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));
        time = agentInstance.getLastHeardTime();
        assertThat(time, is(not(nullValue())));
    }

    @Test
    public void shouldUpdateTheLastHeardTime() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        Date time = agentInstance.getLastHeardTime();
        Thread.sleep(1000);
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        Date newtime = agentInstance.getLastHeardTime();
        assertThat(newtime.after(time), is(true));
    }

    @Test
    public void shouldUpdateSupportBuildCommandProtocolFlag() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        assertThat(agentInstance.getSupportsBuildCommandProtocol(), is(false));
        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));
        assertThat(agentInstance.getSupportsBuildCommandProtocol(), is(false));

        agentInstance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", true));
        assertThat(agentInstance.getSupportsBuildCommandProtocol(), is(true));
    }


    @Test
    public void shouldUpdateIPForPhysicalMachineWhenUpChanged() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(new AgentIdentifier("ccedev01", "10.18.7.52", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));

        assertThat(agentInstance.getAgent().getIpaddress(), is("10.18.7.52"));
    }

    @Test
    public void shouldCleanBuildingInfoWhenAgentIsIdle() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(buildingRuntimeInfo());

        agentInstance.update(idleRuntimeInfo());
        assertThat(agentInstance.getBuildingInfo(), is(AgentBuildingInfo.NOT_BUILDING));
    }

    private AgentRuntimeInfo idleRuntimeInfo() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.idle();
        return agentRuntimeInfo;
    }

    @Test
    public void shouldUpdateBuildingInfoWhenAgentIsBuilding() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        AgentBuildingInfo buildingInfo = new AgentBuildingInfo("running pipeline/stage/build", "buildLocator");
        agentRuntimeInfo.busy(buildingInfo);
        agentInstance.update(agentRuntimeInfo);
        assertThat(agentInstance.getBuildingInfo(), is(buildingInfo));
    }

    @Test
    public void shouldUpdateBuildingInfoWhenAgentIsBuildingWhenCancelled() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(buildingRuntimeInfo());

        agentInstance.update(cancelRuntimeInfo());

        assertThat(agentInstance.getBuildingInfo(), is(defaultBuildingInfo));
        assertThat(agentInstance.getStatus(), is(AgentStatus.Cancelled));
    }

    private AgentRuntimeInfo cancelRuntimeInfo() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.busy(defaultBuildingInfo);
        agentRuntimeInfo.cancel();
        return agentRuntimeInfo;
    }

    @Test
    public void shouldNotChangePendingAgentIpAddress() throws Exception {
        AgentInstance pending = AgentInstance.createFromLiveAgent(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"),
                systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("ccedev01", "10.18.7.52", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        assertThat(pending.isIpChangeRequired(info.getIpAdress()), is(false));
    }

    @Test
    public void shouldChangeIpWhenSameAgentIpChanged() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("ccedev01", "10.18.7.52", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        assertThat(instance.isIpChangeRequired(info.getIpAdress()), is(true));
    }

    @Test
    public void shouldNotChangeIpWhenIpNotChanged() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        assertThat(instance.isIpChangeRequired(DEFAULT_IP_ADDRESS), is(false));
    }

    @Test
    public void shouldDefaultToMissingStatusWhenSyncAnApprovedAgent() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.syncConfig(agent);
        assertThat(instance.getStatus(), is(AgentStatus.Missing));
    }

    @Test
    public void pendingAgentshouldNotBeRegistered() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        AgentInstance instance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, systemEnvironment,
                mock(AgentStatusChangeListener.class));
        assertThat(instance.isRegistered(), is(false));
    }

    @Test
    public void deniedAgentshouldBeRegistered() {
        agent.disable();
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));

        assertThat(instance.isRegistered(), is(true));
    }

    @Test
    public void shouldBeRegisteredForIdleAgent() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.update(idleRuntimeInfo());
        assertThat(instance.isRegistered(), is(true));
    }

    @Test
    public void shouldBecomeIdleAfterApprove() throws Exception {
        AgentInstance instance = AgentInstance.createFromLiveAgent(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"),
                systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.enable();
        instance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        assertThat(instance.getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldBeMissingWhenNeverHeardFromAnyAgent() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        assertThat(instance.getStatus(), is(AgentStatus.Missing));

        instance.refresh();
        assertThat(instance.getStatus(), is(AgentStatus.Missing));
    }

    @Test
    public void shouldBeLostContactWhenLastHeardTimeExeedTimeOut() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, new SystemEnvironment() {
            @Override
            public int getAgentConnectionTimeout() {
                return -1;
            }
        }, mock(AgentStatusChangeListener.class));
        assertThat(instance.getStatus(), is(AgentStatus.Missing));

        instance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
        instance.refresh();
        assertThat(instance.getStatus(), is(AgentStatus.LostContact));
    }

    @Test
    public void shouldRefreshDisabledAgent() {
        agent.disable();
        AgentInstance instance = AgentInstance.createFromAgent(agent, new SystemEnvironment() {
            @Override
            public int getAgentConnectionTimeout() {
                return -1;
            }
        }, mock(AgentStatusChangeListener.class));
        instance.update(new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Building, currentWorkingDirectory(), "cookie"));

        instance.refresh();

        assertThat(instance.getRuntimeStatus(), is(AgentRuntimeStatus.LostContact));
        assertThat(instance.getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldDenyPendingAgent() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        AgentInstance instance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, systemEnvironment,
                mock(AgentStatusChangeListener.class));
        instance.deny();

        assertThat(instance.getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldBeLiveStatus() {
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.update(idleRuntimeInfo());
        instance.refresh();
        assertThat(instance.getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldSyncIPWithConfig() {
        AgentInstance originalAgentInstance = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));

        originalAgentInstance.update(new AgentRuntimeInfo(new AgentIdentifier("CCeDev01", "10.18.5.2", "uuid2"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));

        assertThat(originalAgentInstance.getAgent(), is(new Agent("uuid2", "CCeDev01", "10.18.5.2")));
    }

    @Test
    public void shouldKeepOriginalStatusWhenAgentIsNotDenied() {
        AgentInstance original = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        original.update(buildingRuntimeInfo(agent));

        original.syncConfig(agent);
        assertThat(original.getStatus(), is(AgentStatus.Building));
    }

    @Test
    public void shouldDenyAgentWhenAgentIsDeniedInConfigFile() {
        AgentInstance original = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        original.update(buildingRuntimeInfo());

        Agent newAgentConfig = new Agent(agent.getUuid(), agent.getHostname(), agent.getIpaddress());
        newAgentConfig.disable();

        original.syncConfig(newAgentConfig);
        assertThat(original.getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldDenyAgentWhenItIsNotBuilding() {
        AgentInstance original = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        original.update(idleRuntimeInfo());

        original.deny();
        assertThat(agent.isDisabled(), is(true));
        assertThat(original.getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldReturnFalseWhenAgentHasEnoughSpace() {
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

        assertThat(original.isLowDiskSpace(), is(false));
    }

    @Test
    public void shouldReturnTrueWhenFreeDiskOnAgentIsLow() {
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

        assertThat(original.isLowDiskSpace(), is(true));
    }

    @Test
    public void shouldBeAbleToDenyAgentWhenItIsBuilding() {
        AgentInstance original = AgentInstance.createFromAgent(agent, systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo runtimeInfo = buildingRuntimeInfo();
        original.update(runtimeInfo);
        assertThat(original.canDisable(), is(true));
        original.deny();
        assertThat(agent.isDisabled(), is(true));
        assertThat(original.getStatus(), is(AgentStatus.Disabled));
        assertThat(original.getBuildingInfo(), is(runtimeInfo.getBuildingInfo()));
    }

    @Test
    public void shouldOrderByHostname() {
        AgentInstance agentA = new AgentInstance(new Agent("UUID", "A", "127.0.0.1"), LOCAL, systemEnvironment, null);
        AgentInstance agentB = new AgentInstance(new Agent("UUID", "B", "127.0.0.2"), LOCAL, systemEnvironment, null);

        assertThat(agentA.compareTo(agentA), is(0));
        assertThat(agentA.compareTo(agentB), lessThan(0));
        assertThat(agentB.compareTo(agentA), greaterThan(0));
    }

    @Test
    public void shouldNotBeEqualIfUuidIsNotEqual() {
        AgentInstance agentA = new AgentInstance(new Agent("UUID", "A", "127.0.0.1"), LOCAL, systemEnvironment, null);
        AgentInstance copyOfAgentA = new AgentInstance(new Agent("UUID", "A", "127.0.0.1"),
                LOCAL, systemEnvironment, null);
        AgentInstance agentB = new AgentInstance(new Agent("UUID", "B", "127.0.0.2"), LOCAL, systemEnvironment, null);

        assertThat(agentA, is(not(agentB)));
        assertThat(agentB, is(not(agentA)));
        assertThat(agentA, is(copyOfAgentA));
    }

    @Test
    public void shouldBeAbleToDenyAgentThatIsRunningCancelledJob() {
        Agent agent = new Agent("UUID", "A", "127.0.0.1");
        AgentInstance agentInstance = new AgentInstance(agent, LOCAL, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.cancel();
        AgentBuildingInfo cancelled = agentInstance.getBuildingInfo();
        assertThat(agentInstance.canDisable(), is(true));
        agentInstance.deny();
        assertThat(agent.isDisabled(), is(true));
        assertThat(agentInstance.getStatus(), is(AgentStatus.Disabled));
        assertThat(agentInstance.getBuildingInfo(), is(cancelled));
    }

    @Test
    public void shouldReturnNullWhenNoMatchingJobs() {
        AgentInstance agentInstance = new AgentInstance(agent("linux, mercurial"), LOCAL, systemEnvironment, null);

        JobPlan matchingJob = agentInstance.firstMatching(new ArrayList<>());
        assertThat(matchingJob, is(nullValue()));
    }

    @Test
    public void shouldReturnFirstMatchingJobPlan() {
        AgentInstance agentInstance = new AgentInstance(agent("linux, mercurial"), LOCAL, systemEnvironment, null);

        List<JobPlan> plans = jobPlans("linux, svn", "linux, mercurial");
        JobPlan matchingJob = agentInstance.firstMatching(plans);
        assertThat(matchingJob, is(plans.get(1)));
    }

    @Test
    public void shouldReturnAJobPlanWithMatchingUuidSet() {
        Agent agent = agent("linux, mercurial");
        AgentInstance agentInstance = new AgentInstance(agent, LOCAL, systemEnvironment, null);

        final JobPlan job = jobPlan("pipeline-name", "job-name", "resource", agent.getUuid());
        JobPlan matchingJob = agentInstance.firstMatching(new ArrayList<JobPlan>() {{
            add(job);
        }});
        assertThat(matchingJob, is(job));
    }

    @Test
    public void shouldNotReturnAJobWithMismatchedUuid() {
        Agent agent = agent("linux, mercurial");
        AgentInstance agentInstance = new AgentInstance(agent, LOCAL, systemEnvironment, null);

        final JobPlan job = jobPlan("pipeline-name", "job-name", "linux", agent.getUuid() + "-ensure-doesn't-match");
        JobPlan matchingJob = agentInstance.firstMatching(new ArrayList<JobPlan>() {{
            add(job);
        }});
        assertThat(matchingJob, is(nullValue()));
    }

    @Test
    public void shouldSetAgentToIdleWhenItIsApproved() {
        AgentInstance pendingAgentInstance = AgentInstanceMother.pending();
        Agent agent = new Agent(pendingAgentInstance.getUuid(), pendingAgentInstance.getHostname(), pendingAgentInstance.getIpAddress());
        pendingAgentInstance.syncConfig(agent);
        AgentStatus status = pendingAgentInstance.getStatus();
        assertThat(status, is(AgentStatus.Idle));
    }

    @Test
    public void syncConfigShouldUpdateElasticAgentRuntimeInfo() {
        AgentInstance agentInstance = AgentInstanceMother.idle();

        Agent agent = new Agent(agentInstance.getUuid(), agentInstance.getHostname(), agentInstance.getIpAddress());
        agent.setElasticAgentId("i-123456");
        agent.setElasticPluginId("com.example.aws");

        assertFalse(agentInstance.isElastic());
        agentInstance.syncConfig(agent);
        assertTrue(agentInstance.isElastic());

        assertEquals("i-123456", agentInstance.elasticAgentMetadata().elasticAgentId());
        assertEquals("com.example.aws", agentInstance.elasticAgentMetadata().elasticPluginId());
    }

    @Test
    public void shouldReturnFreeDiskSpace() {
        assertThat(AgentInstanceMother.updateRuntimeStatus(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.idle(new Date(), "CCeDev01"), 1024L), AgentRuntimeStatus.Missing).freeDiskSpace(), is(DiskSpace.unknownDiskSpace()));
        assertThat(AgentInstanceMother.updateRuntimeStatus(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.idle(new Date(), "CCeDev01"), 1024L), AgentRuntimeStatus.LostContact).freeDiskSpace(), is(DiskSpace.unknownDiskSpace()));
        assertThat(AgentInstanceMother.updateRuntimeStatus(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.idle(new Date(), "CCeDev01"), 1024L), AgentRuntimeStatus.Idle).freeDiskSpace(), is(new DiskSpace(1024L)));
        assertThat(AgentInstanceMother.updateRuntimeStatus(AgentInstanceMother.updateUsableSpace(AgentInstanceMother.idle(new Date(), "CCeDev01"), null), AgentRuntimeStatus.Idle).freeDiskSpace(), is(DiskSpace.unknownDiskSpace()));

    }

    @Test
    public void shouldReturnAppropriateMissingStatus() {
        AgentInstance missing = AgentInstanceMother.missing();
        assertTrue(missing.isMissing());
        AgentInstance building = AgentInstanceMother.building();
        assertFalse(building.isMissing());
    }

    @Test
    public void shouldNotMatchJobPlanIfJobRequiresElasticAgent_MatchingIsManagedByBuildAssignmentService() {
        Agent agent = new Agent("uuid");
        agent.setElasticAgentId("elastic-agent-id-1");
        String elasticPluginId = "elastic-plugin-id-1";
        agent.setElasticPluginId(elasticPluginId);
        AgentInstance agentInstance = new AgentInstance(agent, REMOTE, mock(SystemEnvironment.class), null);
        DefaultJobPlan jobPlan1 = new DefaultJobPlan();
        jobPlan1.setElasticProfile(new ElasticProfile("foo", "prod-cluster"));
        List<JobPlan> jobPlans = asList(jobPlan1, new DefaultJobPlan());

        assertThat(agentInstance.firstMatching(jobPlans), is(nullValue()));
    }

    @Test
    public void shouldNotMatchJobPlanIfTheAgentWasLaunchedByADifferentPluginFromThatConfiguredForTheJob() {
        Agent agent = new Agent("uuid");
        agent.setElasticAgentId("elastic-agent-id-1");
        String elasticPluginId = "elastic-plugin-id-1";
        agent.setElasticPluginId(elasticPluginId);
        AgentInstance agentInstance = new AgentInstance(agent, REMOTE, mock(SystemEnvironment.class), null);
        DefaultJobPlan jobPlan1 = new DefaultJobPlan();
        jobPlan1.setElasticProfile(new ElasticProfile("foo", "prod-cluster"));
        List<JobPlan> jobPlans = asList(jobPlan1, new DefaultJobPlan());

        assertThat(agentInstance.firstMatching(jobPlans), is(nullValue()));
    }

    @Test
    public void shouldNotMatchJobPlanIfTheAgentIsElasticAndJobHasResourcesDefined() {
        Agent agent = new Agent("uuid", "hostname", "11.1.1.1", singletonList("r1"));
        agent.setElasticAgentId("elastic-agent-id-1");
        String elasticPluginId = "elastic-plugin-id-1";
        agent.setElasticPluginId(elasticPluginId);
        AgentInstance agentInstance = new AgentInstance(agent, REMOTE, mock(SystemEnvironment.class), null);
        DefaultJobPlan jobPlan1 = new DefaultJobPlan();
        jobPlan1.setResources(asList(new Resource("r1")));
        List<JobPlan> jobPlans = asList(jobPlan1, new DefaultJobPlan());

        assertThat(agentInstance.firstMatching(jobPlans), is(nullValue()));
    }

    @Test
    public void lostContact() {
        AgentInstance agentInstance = AgentInstanceMother.building();
        agentInstance.lostContact();
        assertThat(agentInstance.getStatus(), is(AgentStatus.LostContact));

        AgentInstance pendingInstance = AgentInstanceMother.pending();
        pendingInstance.lostContact();
        assertThat(pendingInstance.getStatus(), is(AgentStatus.Pending));

        AgentInstance disabledInstance = AgentInstanceMother.disabled();
        disabledInstance.lostContact();
        assertThat(disabledInstance.getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldNotRefreshWhenAgentStatusIsPending() {
        AgentInstance agentInstance = AgentInstanceMother.pendingInstance();
        agentInstance.refresh();
        assertThat(agentInstance.getStatus(), is(AgentStatus.Pending));
    }

    @Test
    public void shouldMarkAgentAsMissingWhenLastHeardTimeIsNull() {
        Agent agent = new Agent("1234", "localhost", "192.168.0.1");
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        agentInstance.refresh();
        assertThat(agentInstance.getRuntimeStatus(), is(AgentRuntimeStatus.Missing));
        assertThat(agentInstance.getLastHeardTime(), not(nullValue()));
    }

    @Test
    public void shouldNotRefreshAgentStateWhenAgentIsMissingAndLostContactDurationHasNotExceeded() {
        AgentInstance agentInstance = AgentInstanceMother.missing();
        agentInstance.refresh();
        assertThat(agentInstance.getRuntimeStatus(), is(AgentRuntimeStatus.Missing));
    }

    @Test
    public void shouldChangeAgentStatusToLostContactWhenLostAgentTimeoutHasExceeded() throws IllegalAccessException {
        AgentInstance agentInstance = AgentInstanceMother.missing();
        int agentConnectionTimeoutInMillis = systemEnvironment.getAgentConnectionTimeout() * 1000;
        Date timeLoggedForMissingStatus = new Date(new Date().getTime() - agentConnectionTimeoutInMillis);
        FieldUtils.writeField(agentInstance, "lastHeardTime", timeLoggedForMissingStatus, true);
        agentInstance.refresh();
        assertThat(agentInstance.getRuntimeStatus(), is(AgentRuntimeStatus.LostContact));
    }

    @Nested
    class Matches {
        @Test
        void shouldReturnTrueIfMatchesTheFilter() {
            AgentInstance pending = AgentInstanceMother.pending();
            assertTrue(pending.matches(Pending));

            Agent pendingAgent = pending.getAgent();
            pendingAgent.setElasticAgentId("elastic-agent-id");
            pendingAgent.setElasticPluginId("elastic-plugin-id");

            pending.syncConfig(pendingAgent);

            assertTrue(pending.matches(Elastic));

            AgentInstance nullInstance = AgentInstanceMother.nullInstance();
            assertTrue(nullInstance.matches(Null));
        }

        @Test
        void shouldReturnFalseIfDoesNotMatchTheFilter() {
            AgentInstance building = AgentInstanceMother.building();
            assertFalse(building.matches(Pending));
            assertFalse(building.matches(Elastic));

            AgentInstance pending = AgentInstanceMother.pending();
            assertFalse(pending.matches(Elastic));
            assertFalse(pending.matches(Null));

            AgentInstance idle = AgentInstanceMother.idle();
            Agent idleAgent = idle.getAgent();
            idleAgent.setElasticAgentId("elastic-agent-id");
            idleAgent.setElasticPluginId("elastic-plugin-id");
            idle.syncConfig(idleAgent);
            assertFalse(idle.matches(Pending));
            assertFalse(idle.matches(Null));
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
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(resource), new ArrayList<>(), null, 100, jobIdentifier, null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
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
}
