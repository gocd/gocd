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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.server.service.AgentService;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class JobResultListenerTest {
    private AgentService agentService;
    private JobResultListener listener;
    private JobIdentifier jobIdentifier;
    private static final String AGENT_UUID = "uuid";
    private AgentInstance agentInstance;

    @Before
    public void setup() {
        agentService = mock(AgentService.class);
        listener = new JobResultListener(new JobResultTopic(null), agentService);
        jobIdentifier = new JobIdentifier("cruise", 1, "1", "dev", "1", "linux-firefox");
    }

    @Test
    public void shouldUpdateAgentStatusWhenAJobIsCancelled() throws Exception {
        agentInstance = AgentInstanceMother.building("cruise/1/dev/1/linux-firefox");
        when(agentService.findAgent(AGENT_UUID)).thenReturn(agentInstance);
        listener.onMessage(new JobResultMessage(jobIdentifier, JobResult.Cancelled, AGENT_UUID));
        verify(agentService).notifyJobCancelledEvent(AGENT_UUID);
    }

    @Test
    public void shouldNotUpdateAgentStatusWhenAJobIsCancelledInCaseOfAgentBuildingAnother() throws Exception {
        agentInstance = AgentInstanceMother.building("cruise/1/dev/1/linux-firefox-2");
        when(agentService.findAgent(AGENT_UUID)).thenReturn(agentInstance);
        listener.onMessage(new JobResultMessage(jobIdentifier, JobResult.Cancelled, AGENT_UUID));
        verify(agentService, never()).notifyJobCancelledEvent(AGENT_UUID);
    }
}
