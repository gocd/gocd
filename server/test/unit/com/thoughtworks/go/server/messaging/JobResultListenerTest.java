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

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.util.ClassMockery;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.domain.JobResult.Cancelled;

public class JobResultListenerTest {
    @Rule
    public final JUnitRuleMockery mockery = new JUnitRuleMockery();
    private AgentService agentService;
    private JobResultListener listener;
    private JobIdentifier jobIdentifier;
    private static final String AGENT_UUID = "uuid";

    @Before
    public void setup() {
        agentService = mockery.mock(AgentService.class);
        listener = new JobResultListener(new JobResultTopic(null), agentService);
        jobIdentifier = new JobIdentifier("cruise", "1", "dev", "1", "linux-firefox");
    }

    @Test
    public void shouldUpdateAgentStatusWhenAJobIsCancelled() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(agentService).notifyJobCancelledEvent(AGENT_UUID);
            }
        });

        listener.onMessage(new JobResultMessage(jobIdentifier, Cancelled, AGENT_UUID));
    }
}
