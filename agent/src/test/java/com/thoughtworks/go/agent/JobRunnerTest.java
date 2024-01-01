/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.remote.AgentInstruction.*;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class JobRunnerTest {
    private static final String SERVER_URL = "somewhere-does-not-matter";
    private JobRunner runner;
    private Work work;
    private AgentIdentifier agentIdentifier;
    private UpstreamPipelineResolver resolver;

    @BeforeEach
    void setUp() {
        runner = new JobRunner();
        work = mock(Work.class);
        agentIdentifier = new AgentIdentifier("localhost", "127.0.0.1", "uuid");

        new SystemEnvironment().setProperty(SystemEnvironment.SERVICE_URL, SERVER_URL);
        resolver = mock(UpstreamPipelineResolver.class);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(resolver);
    }

    @Nested
    class handleInstruction {
        @Test
        void shouldDoNothingWhenJobIsNotCancelled() {
            runner.setWork(work);
            runner.handleInstruction(NONE, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));

            verifyNoInteractions(work);
        }

        @Test
        void shouldCancelOncePerJob() {
            runner.setWork(work);
            runner.handleInstruction(CANCEL, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
            verify(work, times(1)).cancel(any(), any());

            runner.handleInstruction(CANCEL, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
            verify(work, times(1)).cancel(any(), any());
        }

        @Test
        void shouldKillRunningTasksOncePerJob() {
            runner.setWork(work);
            runner.handleInstruction(KILL_RUNNING_TASKS, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
            verify(work, times(1)).cancel(any(), any());

            runner.handleInstruction(KILL_RUNNING_TASKS, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));
            verify(work, times(1)).cancel(any(), any());
        }

        @Test
        void shouldReturnTrueOnGetJobIsCancelledWhenJobIsCancelled() {
            assertThat(runner.isJobCancelled()).isFalse();

            runner.handleInstruction(CANCEL, new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie"));

            assertThat(runner.isJobCancelled()).isTrue();
        }
    }
}
