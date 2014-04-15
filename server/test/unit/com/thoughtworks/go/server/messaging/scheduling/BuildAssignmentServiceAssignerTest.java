/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.messaging.scheduling;

import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.BuildAssignmentService;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.work.FakeWork;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class BuildAssignmentServiceAssignerTest {
    private Mockery context;
    private BuildAssignmentService buildAssignmentService;
    private AgentService agentService;
    private static final NullAgentInstance AN_AGENT = new NullAgentInstance("uuid");
    private static final FakeWork SOME_WORK = new FakeWork();

    @Test
    public void shouldFindWorkForSuppliedAgent() {
        context = new ClassMockery();
        buildAssignmentService = context.mock(BuildAssignmentService.class);
        agentService = context.mock(AgentService.class);
        BuildAssignmentServiceAssigner assigner =
                new BuildAssignmentServiceAssigner(agentService, buildAssignmentService);
        context.checking(new Expectations() {{
            one(agentService).findAgentAndRefreshStatus("uuid");
            will(returnValue(AN_AGENT));
            one(buildAssignmentService).assignWorkToAgent(AN_AGENT);
            will(returnValue(SOME_WORK));
        }});
        assigner.assignWorkToAgent(new AgentIdentifier("localhost", "127.0.0.1", "uuid"));
    }
}
