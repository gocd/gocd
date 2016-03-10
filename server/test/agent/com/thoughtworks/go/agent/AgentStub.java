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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.exception.UnregisteredAgentException;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;

public class AgentStub {
    private static final Log LOGGER = LogFactory.getLog(AgentStub.class);

    public static void main(String[] args) {
        LOGGER.info("Starting agent......");
        new ClassPathXmlApplicationContext("/com/thoughtworks/go/agent/agent.xml");
    }

    public AgentStub(BuildRepositoryRemote server) {
        LOGGER.info("Agent started.");
        Work work = server.getWork(new AgentRuntimeInfo(new AgentIdentifier("", "", "1234"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false));
        try {
            LOGGER.info(work.description());
        } catch (UnregisteredAgentException e) {
            LOGGER.warn("Unregistered Agent: " + e.getMessage());
        }
    }
}
