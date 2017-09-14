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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.exception.UnregisteredAgentException;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;

public class AgentStub {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentStub.class);

    public static void main(String[] args) {
        LOGGER.info("Starting agent......");
        new ClassPathXmlApplicationContext("/com/thoughtworks/go/agent/agent.xml");
    }

    public AgentStub(BuildRepositoryRemote server) {
        LOGGER.info("Agent started.");
        Work work = server.getWork(new AgentRuntimeInfo(new AgentIdentifier("", "", "1234"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, new TimeProvider()));
        try {
            LOGGER.info(work.description());
        } catch (UnregisteredAgentException e) {
            LOGGER.warn("Unregistered Agent: {}", e.getMessage());
        }
    }
}
