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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.mockito.Mockito.*;

public class ScheduleServiceRescheduleHungJobsTest {
    private JobInstanceService jobInstanceService;
    private AgentService agentService;
    private ScheduleService scheduleService;
    private ConsoleActivityMonitor consoleActivityMonitor;

    @BeforeEach
    public void setUp() {
        agentService = mock(AgentService.class);
        jobInstanceService = mock(JobInstanceService.class);
        consoleActivityMonitor = mock(ConsoleActivityMonitor.class);
        SchedulingPerformanceLogger schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);
        scheduleService = new ScheduleService(null, null, null, null, null, null, null, null, null, jobInstanceService,
                null, null, null, null, null, null, agentService, null, null, consoleActivityMonitor, null, null, schedulingPerformanceLogger,
                null, null
        );
    }

    @Test
    public void shouldNotQueryForBuildWhenThereAreNoLiveAgents() {
        when(agentService.findRegisteredAgents()).thenReturn(new AgentInstances(mock(AgentStatusChangeListener.class)));
        scheduleService.rescheduleHungJobs();
        verify(agentService).findRegisteredAgents();
        verify(jobInstanceService, times(0)).findHungJobs(any());
    }

    @Test
    public void shouldRescheduleHungBuildForDeadAgent() {
        final JobInstance jobInstance = JobInstanceMother.assigned("dev");
        when(agentService.findRegisteredAgents()).thenReturn(activities());
        when(jobInstanceService.findHungJobs(Arrays.asList("uuid1", "uuid2"))).thenReturn(new JobInstances(jobInstance));
        scheduleService.rescheduleHungJobs();
        verify(agentService).findRegisteredAgents();
        verify(jobInstanceService).findHungJobs(Arrays.asList("uuid1", "uuid2"));
    }

    @Test
    public void shouldNotRescheduleHungBuildsWhenNone() {
        when(agentService.findRegisteredAgents()).thenReturn(activities());
        when(jobInstanceService.findHungJobs(Arrays.asList("uuid1", "uuid2"))).thenReturn(new JobInstances());
        scheduleService.rescheduleHungJobs();
        verify(agentService).findRegisteredAgents();
        verify(jobInstanceService).findHungJobs(Arrays.asList("uuid1", "uuid2"));
    }

    @Test
    public void shouldNotifyConsoleActivityMonitorToCancelUnresponsiveJobs() {
        when(agentService.findRegisteredAgents()).thenReturn(activities());
        when(jobInstanceService.findHungJobs(Arrays.asList("uuid1", "uuid2"))).thenReturn(new JobInstances());
        scheduleService.rescheduleHungJobs();
    }

    private AgentInstances activities() {
        AgentStatusChangeListener agentStatusChangeListener = mock(AgentStatusChangeListener.class);
        final AgentInstances activities = new AgentInstances(agentStatusChangeListener);
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        activities.add(AgentInstance.createFromAgent(new Agent("uuid1"), systemEnvironment, agentStatusChangeListener));
        activities.add(AgentInstance.createFromAgent(new Agent("uuid2"), systemEnvironment, agentStatusChangeListener));
        return activities;
    }

}
