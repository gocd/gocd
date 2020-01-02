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

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.server.domain.JobDurationStrategy;
import com.thoughtworks.go.server.ui.JobInstanceModel;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.thoughtworks.go.helper.AgentInstanceMother.building;
import static com.thoughtworks.go.helper.JobInstanceMother.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


public class JobPresentationServiceTest {

    private JobDurationStrategy jobDurationStrategy;
    private AgentService agentService;

    @Before
    public void setUp() throws Exception {
        jobDurationStrategy = mock(JobDurationStrategy.class);
        agentService = mock(AgentService.class);
    }

    @Test
    public void shouldReturnJobModel() {
        JobInstance dev = assignedWithAgentId("dev", "agent1");
        JobInstance DEv = assignedWithAgentId("DEv", "agent1");
        JobInstance bev = assignedWithAgentId("bev", "agent2");
        JobInstance tev = scheduled("tev");
        JobInstance lev = assignAgent(passed("lev"), "agent3");
        JobInstance kev = assignAgent(failed("kev"), "agent3");
        AgentInstance agentInstance = building();
        when(agentService.findAgentAndRefreshStatus(any(String.class))).thenReturn(agentInstance);
        List<JobInstanceModel> models = new JobPresentationService(jobDurationStrategy, agentService).jobInstanceModelFor(new JobInstances(dev, bev, tev, lev, kev, DEv));
        assertThat(models.size(), is(6));
        //failed
        assertThat(models.get(0), is(new JobInstanceModel(kev, jobDurationStrategy, agentInstance)));
        //in progress. sort by name (case insensitive)
        assertThat(models.get(1), is(new JobInstanceModel(bev, jobDurationStrategy, agentInstance)));
        assertThat(models.get(2), is(new JobInstanceModel(dev, jobDurationStrategy, agentInstance)));
        assertThat(models.get(3), is(new JobInstanceModel(DEv, jobDurationStrategy, agentInstance)));
        assertThat(models.get(4), is(new JobInstanceModel(tev, jobDurationStrategy)));
        //passed
        assertThat(models.get(5), is(new JobInstanceModel(lev, jobDurationStrategy, agentInstance)));
        //assert agent info
        verify(agentService, times(2)).findAgentAndRefreshStatus("agent1");
        verify(agentService).findAgentAndRefreshStatus("agent2");
        verify(agentService, times(2)).findAgentAndRefreshStatus("agent3");
        verifyNoMoreInteractions(agentService);
    }

    @Test
    public void shouldReturnJobModelForAnAgentThatIsNoMoreAvailableInTheConfig() {
        String deletedAgentUuid = "deleted_agent";
        JobInstance jobWithDeletedAgent = assignedWithAgentId("dev", deletedAgentUuid);
        when(agentService.findAgentAndRefreshStatus(deletedAgentUuid)).thenReturn(null);
        Agent agentFromDb = new Agent(deletedAgentUuid,"hostname", "1.2.3.4", "cookie");
        when(agentService.findAgentByUUID(deletedAgentUuid)).thenReturn(agentFromDb);
        List<JobInstanceModel> models = new JobPresentationService(jobDurationStrategy, agentService).jobInstanceModelFor(new JobInstances(jobWithDeletedAgent));
        assertThat(models.size(), is(1));
        assertThat(models.get(0), is(new JobInstanceModel(jobWithDeletedAgent, jobDurationStrategy, agentFromDb)));
        verify(agentService, times(1)).findAgentAndRefreshStatus(deletedAgentUuid);
        verify(agentService, times(1)).findAgentByUUID(deletedAgentUuid);
        verifyNoMoreInteractions(agentService);
    }
}
