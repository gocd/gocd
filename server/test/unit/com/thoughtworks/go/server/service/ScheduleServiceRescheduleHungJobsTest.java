/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.util.Arrays;
import java.util.HashSet;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.SystemEnvironment;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.thoughtworks.go.domain.AgentInstance.create;
import static com.thoughtworks.go.helper.JobInstanceMother.assigned;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScheduleServiceRescheduleHungJobsTest {
    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();
    private JobInstanceService jobInstanceService;
    private AgentService agentService;
    private ScheduleService scheduleService;
    private ConsoleActivityMonitor consoleActivityMonitor;

    @Before
    public void setUp() {
        agentService = mock(AgentService.class);
        jobInstanceService = mock(JobInstanceService.class);
        consoleActivityMonitor = mock(ConsoleActivityMonitor.class);
        SchedulingPerformanceLogger schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);
        scheduleService = new ScheduleService(null, null, null, null, null, null, null, null, null, null, jobInstanceService,
                null, null, null, null, null, null, agentService, null, null, consoleActivityMonitor, null, null, schedulingPerformanceLogger);
    }

    @Test
    public void shouldRescheduleHungBuildForDeadAgent() {
        final JobInstance jobInstance = assigned("dev");
        when(agentService.findRegisteredAgents()).thenReturn(activities());
        when(jobInstanceService.findHungJobs(asList("uuid1", "uuid2"))).thenReturn(new JobInstances(jobInstance));
        scheduleService.rescheduleHungJobs();
        verify(agentService).findRegisteredAgents();
        verify(jobInstanceService).findHungJobs(asList("uuid1", "uuid2"));
    }

    @Test
    public void shouldNotRescheduleHungBuildsWhenNone() {
        when(agentService.findRegisteredAgents()).thenReturn(activities());
        when(jobInstanceService.findHungJobs(asList("uuid1", "uuid2"))).thenReturn(new JobInstances());
        scheduleService.rescheduleHungJobs();
        verify(agentService).findRegisteredAgents();
        verify(jobInstanceService).findHungJobs(asList("uuid1", "uuid2"));
    }

    @Test
    public void shouldNotifyConsoleActivityMonitorToCancelUnresponsiveJobs() {
        when(agentService.findRegisteredAgents()).thenReturn(activities());
        when(jobInstanceService.findHungJobs(asList("uuid1", "uuid2"))).thenReturn(new JobInstances());
        scheduleService.rescheduleHungJobs();
    }

    private AgentInstances activities() {
        final AgentInstances activities = new AgentInstances(null);
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        HashSet<EnvironmentPipelineMatcher> noEnvironments = new HashSet<EnvironmentPipelineMatcher>();
        activities.add(create(new AgentConfig("uuid1"), false, systemEnvironment));
        activities.add(create(new AgentConfig("uuid2"), false, systemEnvironment));
        return activities;
    }
}
