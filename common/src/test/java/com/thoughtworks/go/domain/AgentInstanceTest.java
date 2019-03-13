/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.config.ResourceConfigs;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.domain.AgentInstance.AgentType.LOCAL;
import static com.thoughtworks.go.domain.AgentInstance.AgentType.REMOTE;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AgentInstanceTest {
    private SystemEnvironment systemEnvironment;
    public AgentConfig agentConfig;
    public AgentBuildingInfo defaultBuildingInfo;
    private static final String DEFAULT_IP_ADDRESS = "10.18.5.1";
    private AgentStatusChangeListener agentStatusChangeListener;

    @Before
    public void setUp() {
        systemEnvironment = new SystemEnvironment();
        agentConfig = new AgentConfig("uuid2", "CCeDev01", DEFAULT_IP_ADDRESS);
        defaultBuildingInfo = new AgentBuildingInfo("pipeline", "buildLocator");
        agentStatusChangeListener = mock(AgentStatusChangeListener.class);
    }

    @After
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
    public void shouldKeepStatusAsCancelled() throws Exception {
        AgentInstance building = AgentInstanceMother.building("buildLocator");
        building.cancel();

        building.update(buildingRuntimeInfo(building.agentConfig()));

        assertThat(building.getStatus(), is(AgentStatus.Cancelled));
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnUpdate() throws Exception {
        AgentInstance idleAgent = AgentInstance.createFromConfig(agentConfig("abc"), new SystemEnvironment(), agentStatusChangeListener);

        idleAgent.update(buildingRuntimeInfo());

        verify(agentStatusChangeListener).onAgentStatusChange(idleAgent);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnAgentBuilding() throws Exception {
        AgentInstance idleAgent = AgentInstance.createFromConfig(agentConfig("abc"), new SystemEnvironment(), agentStatusChangeListener);

        idleAgent.building(new AgentBuildingInfo("running pipeline/stage/build", "buildLocator"));

        verify(agentStatusChangeListener).onAgentStatusChange(idleAgent);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnCancel() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig("abc"), new SystemEnvironment(), agentStatusChangeListener);

        agentInstance.cancel();

        verify(agentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnRefreshAndMarkedMissing() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig("abc"), new SystemEnvironment(), agentStatusChangeListener);

        agentInstance.idle();
        agentInstance.refresh();

        assertThat(agentInstance.getStatus(), is(AgentStatus.Missing));
        verify(agentStatusChangeListener, times(2)).onAgentStatusChange(agentInstance);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnRefreshAndMarkedLostContact() throws Exception {
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment() {
            public int getAgentConnectionTimeout() {
                return -1;
            }
        }, agentStatusChangeListener);
        assertThat(instance.getStatus(), is(AgentStatus.Missing));

        instance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));
        instance.refresh();

        assertThat(instance.getStatus(), is(AgentStatus.LostContact));
        verify(agentStatusChangeListener, times(2)).onAgentStatusChange(instance);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnEnablingAgent() throws Exception {
        AgentInstance instance = AgentInstanceMother.disabled();

        AgentInstance disabledAgent = new AgentInstance(instance.agentConfig, instance.getType(), systemEnvironment, agentStatusChangeListener);

        disabledAgent.enable();

        verify(agentStatusChangeListener).onAgentStatusChange(disabledAgent);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnDisablingAgent() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig("abc"), new SystemEnvironment(), agentStatusChangeListener);

        agentInstance.deny();

        verify(agentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Test
    public void shouldNotifyAgentChangeListenerOnConfigSync() throws Exception {
        AgentInstance instance = AgentInstanceMother.disabled();

        AgentInstance agentInstance = new AgentInstance(instance.agentConfig, instance.getType(), systemEnvironment, agentStatusChangeListener);

        agentInstance.syncConfig(agentConfig);

        verify(agentStatusChangeListener).onAgentStatusChange(agentInstance);
    }

    @Test
    public void shouldUpdateAgentBackToIdleAfterCancelledTaskFinishes() throws Exception {
        AgentInstance cancelled = AgentInstanceMother.cancelled();

        AgentRuntimeInfo fromAgent = new AgentRuntimeInfo(cancelled.agentConfig().getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        fromAgent.idle();
        cancelled.update(fromAgent);

        assertThat(cancelled.getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldUpdateTheInstallLocation() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        String installPath = "/var/lib/GoServer";
        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        newRuntimeInfo.setLocation(installPath);
        agentInstance.update(newRuntimeInfo);

        assertThat(agentInstance.getLocation(), is(installPath));
    }

    @Test
    public void shouldUpdateTheUsableSpace() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));

        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        newRuntimeInfo.setUsableSpace(1000L);

        assertThat(agentInstance.getUsableSpace(), is(not(newRuntimeInfo.getUsableSpace())));
        agentInstance.update(newRuntimeInfo);
        assertThat(agentInstance.getUsableSpace(), is(newRuntimeInfo.getUsableSpace()));
    }

    @Test
    public void shouldAssignCertificateToApprovedAgent() {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));

        Registration entry = agentInstance.assignCertification();
        assertThat(entry.getChain().length, is(not(0)));
    }

    @Test
    public void shouldNotAssignCertificateToPendingAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux", false);
        AgentInstance agentInstance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, systemEnvironment,
                mock(AgentStatusChangeListener.class));

        Registration entry = agentInstance.assignCertification();
        assertThat(entry.getChain().length, is(0));
    }


    @Test
    public void shouldInitializeTheLastHeardTimeWhenFirstPing() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        Date time = agentInstance.getLastHeardTime();
        assertThat(time, is(nullValue()));
        agentInstance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));
        time = agentInstance.getLastHeardTime();
        assertThat(time, is(not(nullValue())));
    }

    @Test
    public void shouldUpdateTheLastHeardTime() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));
        Date time = agentInstance.getLastHeardTime();
        Thread.sleep(1000);
        agentInstance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));
        Date newtime = agentInstance.getLastHeardTime();
        assertThat(newtime.after(time), is(true));
    }

    @Test
    public void shouldUpdateSupportBuildCommandProtocolFlag() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        assertThat(agentInstance.getSupportsBuildCommandProtocol(), is(false));
        agentInstance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));
        assertThat(agentInstance.getSupportsBuildCommandProtocol(), is(false));

        agentInstance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", true));
        assertThat(agentInstance.getSupportsBuildCommandProtocol(), is(true));
    }


    @Test
    public void shouldUpdateIPForPhysicalMachineWhenUpChanged() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(new AgentRuntimeInfo(new AgentIdentifier("ccedev01", "10.18.7.52", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));

        assertThat(agentInstance.agentConfig().getIpAddress(), is("10.18.7.52"));
    }

    @Test
    public void shouldCleanBuildingInfoWhenAgentIsIdle() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(buildingRuntimeInfo());

        agentInstance.update(idleRuntimeInfo());
        assertThat(agentInstance.getBuildingInfo(), is(AgentBuildingInfo.NOT_BUILDING));
    }

    private AgentRuntimeInfo idleRuntimeInfo() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        agentRuntimeInfo.idle();
        return agentRuntimeInfo;
    }

    @Test
    public void shouldUpdateBuildingInfoWhenAgentIsBuilding() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        AgentBuildingInfo buildingInfo = new AgentBuildingInfo("running pipeline/stage/build", "buildLocator");
        agentRuntimeInfo.busy(buildingInfo);
        agentInstance.update(agentRuntimeInfo);
        assertThat(agentInstance.getBuildingInfo(), is(buildingInfo));
    }

    @Test
    public void shouldUpdateBuildingInfoWhenAgentIsBuildingWhenCancelled() throws Exception {
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        agentInstance.update(buildingRuntimeInfo());

        agentInstance.update(cancelRuntimeInfo());

        assertThat(agentInstance.getBuildingInfo(), is(defaultBuildingInfo));
        assertThat(agentInstance.getStatus(), is(AgentStatus.Cancelled));
    }

    private AgentRuntimeInfo cancelRuntimeInfo() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        agentRuntimeInfo.busy(defaultBuildingInfo);
        agentRuntimeInfo.cancel();
        return agentRuntimeInfo;
    }

    @Test
    public void shouldNotChangePendingAgentIpAddress() throws Exception {
        AgentInstance pending = AgentInstance.createFromLiveAgent(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false),
                systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("ccedev01", "10.18.7.52", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        assertThat(pending.isIpChangeRequired(info.getIpAdress()), is(false));
    }

    @Test
    public void shouldChangeIpWhenSameAgentIpChanged() throws Exception {
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("ccedev01", "10.18.7.52", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        assertThat(instance.isIpChangeRequired(info.getIpAdress()), is(true));
    }

    @Test
    public void shouldNotChangeIpWhenIpNotChanged() throws Exception {
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        assertThat(instance.isIpChangeRequired(DEFAULT_IP_ADDRESS), is(false));
    }

    @Test
    public void shouldDefaultToMissingStatusWhenSyncAnApprovedAgent() throws Exception {
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.syncConfig(agentConfig);
        assertThat(instance.getStatus(), is(AgentStatus.Missing));
    }

    @Test
    public void pendingAgentshouldNotBeRegistered() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        AgentInstance instance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, systemEnvironment,
                mock(AgentStatusChangeListener.class));
        assertThat(instance.isRegistered(), is(false));
    }

    @Test
    public void deniedAgentshouldBeRegistered() throws Exception {
        agentConfig.disable();
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));

        assertThat(instance.isRegistered(), is(true));
    }

    @Test
    public void shouldBeRegisteredForIdleAgent() throws Exception {
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.update(idleRuntimeInfo());
        assertThat(instance.isRegistered(), is(true));
    }

    @Test
    public void shouldBecomeIdleAfterApprove() throws Exception {
        AgentInstance instance = AgentInstance.createFromLiveAgent(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false),
                systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.enable();
        instance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));
        assertThat(instance.getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldBeMissingWhenNeverHeardFromAnyAgent() {
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        assertThat(instance.getStatus(), is(AgentStatus.Missing));

        instance.refresh();
        assertThat(instance.getStatus(), is(AgentStatus.Missing));
    }

    @Test
    public void shouldBeLostContactWhenLastHeardTimeExeedTimeOut() {
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment() {
            public int getAgentConnectionTimeout() {
                return -1;
            }
        }, mock(AgentStatusChangeListener.class));
        assertThat(instance.getStatus(), is(AgentStatus.Missing));

        instance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));
        instance.refresh();
        assertThat(instance.getStatus(), is(AgentStatus.LostContact));
    }

    @Test
    public void shouldRefreshDisabledAgent() throws Exception {
        agentConfig.disable();
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment() {
            public int getAgentConnectionTimeout() {
                return -1;
            }
        }, mock(AgentStatusChangeListener.class));
        instance.update(new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Building, currentWorkingDirectory(), "cookie", false));

        instance.refresh();

        assertThat(instance.getRuntimeStatus(), is(AgentRuntimeStatus.LostContact));
        assertThat(instance.getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldDenyPendingAgent() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        AgentInstance instance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, systemEnvironment,
                mock(AgentStatusChangeListener.class));
        instance.deny();

        assertThat(instance.getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldBeLiveStatus() throws Exception {
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        instance.update(idleRuntimeInfo());
        instance.refresh();
        assertThat(instance.getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldSyncIPWithConfig() {
        AgentInstance original = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));

        original.update(new AgentRuntimeInfo(new AgentIdentifier("CCeDev01", "10.18.5.2", "uuid2"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false));

        assertThat(original.agentConfig(), is(new AgentConfig("uuid2", "CCeDev01", "10.18.5.2")));
    }

    @Test
    public void shouldKeepOriginalStatusWhenAgentIsNotDenied() throws Exception {
        AgentInstance original = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        original.update(buildingRuntimeInfo(agentConfig));

        original.syncConfig(agentConfig);
        assertThat(original.getStatus(), is(AgentStatus.Building));
    }

    @Test
    public void shouldDenyAgentWhenAgentIsDeniedInConfigFile() throws Exception {
        AgentInstance original = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        original.update(buildingRuntimeInfo());

        AgentConfig newAgentConfig = new AgentConfig(agentConfig.getUuid(), agentConfig.getHostname(), agentConfig.getIpAddress());
        newAgentConfig.disable();

        original.syncConfig(newAgentConfig);
        assertThat(original.getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldDenyAgentWhenItIsNotBuilding() throws Exception {
        AgentInstance original = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        original.update(idleRuntimeInfo());

        original.deny();
        assertThat(agentConfig.isDisabled(), is(true));
        assertThat(original.getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldReturnFalseWhenAgentHasEnoughSpace() throws Exception {
        AgentInstance original = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment() {
            @Override
            public long getAgentSizeLimit() {
                return 100 * 1024 * 1024;
            }
        }, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        long is110M = 110 * 1024 * 1024;
        newRuntimeInfo.setUsableSpace(is110M);
        original.update(newRuntimeInfo);

        assertThat(original.isLowDiskSpace(), is(false));
    }

    @Test
    public void shouldReturnTrueWhenFreeDiskOnAgentIsLow() throws Exception {
        AgentInstance original = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment() {
            @Override
            public long getAgentSizeLimit() {
                return 100 * 1024 * 1024;
            }
        }, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        long is90M = 90 * 1024 * 1024;
        newRuntimeInfo.setUsableSpace(is90M);
        original.update(newRuntimeInfo);

        assertThat(original.isLowDiskSpace(), is(true));
    }

    @Test
    public void shouldBeAbleToDenyAgentWhenItIsBuilding() throws Exception {
        AgentInstance original = AgentInstance.createFromConfig(agentConfig, systemEnvironment, mock(AgentStatusChangeListener.class));
        AgentRuntimeInfo runtimeInfo = buildingRuntimeInfo();
        original.update(runtimeInfo);
        assertThat(original.canDisable(), is(true));
        original.deny();
        assertThat(agentConfig.isDisabled(), is(true));
        assertThat(original.getStatus(), is(AgentStatus.Disabled));
        assertThat(original.getBuildingInfo(), is(runtimeInfo.getBuildingInfo()));
    }

    @Test
    public void shouldOrderByHostname() throws Exception {
        AgentInstance agentA = new AgentInstance(new AgentConfig("UUID", "A", "127.0.0.1"), LOCAL, systemEnvironment, null);
        AgentInstance agentB = new AgentInstance(new AgentConfig("UUID", "B", "127.0.0.2"), LOCAL, systemEnvironment, null);

        assertThat(agentA.compareTo(agentA), is(0));
        assertThat(agentA.compareTo(agentB), lessThan(0));
        assertThat(agentB.compareTo(agentA), greaterThan(0));
    }

    @Test
    public void shouldNotBeEqualIfUuidIsNotEqual() throws Exception {
        AgentInstance agentA = new AgentInstance(new AgentConfig("UUID", "A", "127.0.0.1"), LOCAL, systemEnvironment, null);
        AgentInstance copyOfAgentA = new AgentInstance(new AgentConfig("UUID", "A", "127.0.0.1"),
                LOCAL, systemEnvironment, null);
        AgentInstance agentB = new AgentInstance(new AgentConfig("UUID", "B", "127.0.0.2"), LOCAL, systemEnvironment, null);

        assertThat(agentA, is(not(agentB)));
        assertThat(agentB, is(not(agentA)));
        assertThat(agentA, is(copyOfAgentA));
    }

    @Test
    public void shouldBeAbleToDenyAgentThatIsRunningCancelledJob() {
        AgentConfig config = new AgentConfig("UUID", "A", "127.0.0.1");
        AgentInstance agent = new AgentInstance(config, LOCAL, systemEnvironment, mock(AgentStatusChangeListener.class));
        agent.cancel();
        AgentBuildingInfo cancelled = agent.getBuildingInfo();
        assertThat(agent.canDisable(), is(true));
        agent.deny();
        assertThat(config.isDisabled(), is(true));
        assertThat(agent.getStatus(), is(AgentStatus.Disabled));
        assertThat(agent.getBuildingInfo(), is(cancelled));
    }

    @Test
    public void shouldReturnNullWhenNoMatchingJobs() throws Exception {
        AgentInstance agentInstance = new AgentInstance(agentConfig("linux, mercurial"), LOCAL, systemEnvironment, null);

        JobPlan matchingJob = agentInstance.firstMatching(new ArrayList<>());
        assertThat(matchingJob, is(nullValue()));
    }

    @Test
    public void shouldReturnFirstMatchingJobPlan() throws Exception {
        AgentInstance agentInstance = new AgentInstance(agentConfig("linux, mercurial"), LOCAL, systemEnvironment, null);

        List<JobPlan> plans = jobPlans("linux, svn", "linux, mercurial");
        JobPlan matchingJob = agentInstance.firstMatching(plans);
        assertThat(matchingJob, is(plans.get(1)));
    }

    @Test
    public void shouldReturnAJobPlanWithMatchingUuidSet() throws Exception {
        AgentConfig config = agentConfig("linux, mercurial");
        AgentInstance agentInstance = new AgentInstance(config, LOCAL, systemEnvironment, null);

        final JobPlan job = jobPlan("pipeline-name", "job-name", "resource", config.getUuid());
        JobPlan matchingJob = agentInstance.firstMatching(new ArrayList<JobPlan>() {{
            add(job);
        }});
        assertThat(matchingJob, is(job));
    }

    @Test
    public void shouldNotReturnAJobWithMismatchedUuid() throws Exception {
        AgentConfig config = agentConfig("linux, mercurial");
        AgentInstance agentInstance = new AgentInstance(config, LOCAL, systemEnvironment, null);

        final JobPlan job = jobPlan("pipeline-name", "job-name", "linux", config.getUuid() + "-ensure-doesn't-match");
        JobPlan matchingJob = agentInstance.firstMatching(new ArrayList<JobPlan>() {{
            add(job);
        }});
        assertThat(matchingJob, is(nullValue()));
    }

    @Test
    public void shouldSetAgentToIdleWhenItIsApproved() {
        AgentInstance pending = AgentInstanceMother.pending();
        AgentConfig config = new AgentConfig(pending.getUuid(), pending.getHostname(), pending.getIpAddress());
        pending.syncConfig(config);
        AgentStatus status = pending.getStatus();
        assertThat(status, is(AgentStatus.Idle));
    }

    @Test
    public void syncConfigShouldUpdateElasticAgentRuntimeInfo() {
        AgentInstance agent = AgentInstanceMother.idle();

        AgentConfig agentConfig = new AgentConfig(agent.getUuid(), agent.getHostname(), agent.getIpAddress());
        agentConfig.setElasticAgentId("i-123456");
        agentConfig.setElasticPluginId("com.example.aws");

        assertFalse(agent.isElastic());
        agent.syncConfig(agentConfig);
        assertTrue(agent.isElastic());

        assertEquals("i-123456", agent.elasticAgentMetadata().elasticAgentId());
        assertEquals("com.example.aws", agent.elasticAgentMetadata().elasticPluginId());
    }

    @Test
    public void shouldReturnFreeDiskSpace() throws Exception {
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
        AgentConfig agentConfig = new AgentConfig("uuid");
        agentConfig.setElasticAgentId("elastic-agent-id-1");
        String elasticPluginId = "elastic-plugin-id-1";
        agentConfig.setElasticPluginId(elasticPluginId);
        AgentInstance agentInstance = new AgentInstance(agentConfig, REMOTE, mock(SystemEnvironment.class), null);
        DefaultJobPlan jobPlan1 = new DefaultJobPlan();
        jobPlan1.setElasticProfile(new ElasticProfile("foo", elasticPluginId, "prod-cluster"));
        List<JobPlan> jobPlans = asList(jobPlan1, new DefaultJobPlan());

        assertThat(agentInstance.firstMatching(jobPlans), is(nullValue()));
    }

    @Test
    public void shouldNotMatchJobPlanIfTheAgentWasLaunchedByADifferentPluginFromThatConfiguredForTheJob() {
        AgentConfig agentConfig = new AgentConfig("uuid");
        agentConfig.setElasticAgentId("elastic-agent-id-1");
        String elasticPluginId = "elastic-plugin-id-1";
        agentConfig.setElasticPluginId(elasticPluginId);
        AgentInstance agentInstance = new AgentInstance(agentConfig, REMOTE, mock(SystemEnvironment.class), null);
        DefaultJobPlan jobPlan1 = new DefaultJobPlan();
        jobPlan1.setElasticProfile(new ElasticProfile("foo", "elastic-plugin-id-2", "prod-cluster"));
        List<JobPlan> jobPlans = asList(jobPlan1, new DefaultJobPlan());

        assertThat(agentInstance.firstMatching(jobPlans), is(nullValue()));
    }

    @Test
    public void shouldNotMatchJobPlanIfTheAgentIsElasticAndJobHasResourcesDefined() {
        AgentConfig agentConfig = new AgentConfig("uuid", "hostname", "11.1.1.1", new ResourceConfigs(new ResourceConfig("r1")));
        agentConfig.setElasticAgentId("elastic-agent-id-1");
        String elasticPluginId = "elastic-plugin-id-1";
        agentConfig.setElasticPluginId(elasticPluginId);
        AgentInstance agentInstance = new AgentInstance(agentConfig, REMOTE, mock(SystemEnvironment.class), null);
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
        AgentConfig agentConfig = new AgentConfig("1234", "localhost", "192.168.0.1");
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
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
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(resource), new ArrayList<>(), null, 100, jobIdentifier, null, new EnvironmentVariables(), new EnvironmentVariables(), null);
        plan.setAgentUuid(uuid);
        return plan;
    }

    private AgentConfig agentConfig(String resources) {
        return new AgentConfig("UUID", "A", "127.0.0.1", new ResourceConfigs(resources));
    }

    private AgentRuntimeInfo buildingRuntimeInfo() {
        return buildingRuntimeInfo(agentConfig);
    }

    private AgentRuntimeInfo buildingRuntimeInfo(AgentConfig agentConfig) {
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
        runtimeInfo.busy(defaultBuildingInfo);
        return runtimeInfo;
    }
}
