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
package com.thoughtworks.go.remote;

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.InvalidAgentInstructionException;
import com.thoughtworks.go.server.messaging.JobStatusMessage;
import com.thoughtworks.go.server.messaging.JobStatusTopic;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.BuildRepositoryService;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.remoting.RemoteAccessException;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

class BuildRepositoryRemoteImplTest {
    private BuildRepositoryService repositoryService;
    private AgentService agentService;
    private JobStatusTopic jobStatusTopic;
    private BuildRepositoryRemoteImpl buildRepository;
    private LogFixture logFixture;
    private AgentRuntimeInfo info;

    @BeforeEach
    void setUp() {
        repositoryService = mock(BuildRepositoryService.class);
        agentService = mock(AgentService.class);
        jobStatusTopic = mock(JobStatusTopic.class);
        buildRepository = new BuildRepositoryRemoteImpl(repositoryService, agentService, jobStatusTopic);
        logFixture = logFixtureFor(BuildRepositoryRemoteImpl.class, Level.TRACE);
        info = new AgentRuntimeInfo(new AgentIdentifier("host", "192.168.1.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
    }

    @AfterEach
    void tearDown() {
        logFixture.close();
    }

    @Nested
    class ping {
        @Test
        void shouldUpdateAgentServiceOnPing() {
            info.setStatus(AgentStatus.Cancelled);
            when(agentService.findAgentAndRefreshStatus(info.getUUId())).thenReturn(AgentInstance.createFromLiveAgent(info, new SystemEnvironment(), null));

            AgentInstruction instruction = buildRepository.ping(info);

            assertThat(instruction.shouldCancel()).isTrue();
            verify(agentService).updateRuntimeInfo(info);
            assertThat(logFixture.getRawMessages()).contains(info + " ping received.");
        }

        @Test
        void shouldReturnInstructionsToAgent() throws InvalidAgentInstructionException {
            AgentInstance agentInstance = AgentInstance.createFromLiveAgent(info, new SystemEnvironment(), null);
            agentInstance.cancel();
            agentInstance.killRunningTasks();

            when(agentService.findAgentAndRefreshStatus(info.getUUId())).thenReturn(agentInstance);

            AgentInstruction instruction = buildRepository.ping(info);

            assertThat(instruction.shouldCancel()).isFalse();
            assertThat(instruction.shouldKillRunningTasks()).isTrue();
        }
    }

    @Test
    void shouldLogFailureToUpdateAgentServiceOnPing() {
        RuntimeException runtimeException = new RuntimeException("holy smoke");
        doThrow(runtimeException).when(agentService).updateRuntimeInfo(info);
        try {
            buildRepository.ping(info);
            fail("should have bombed because agentService could not be updated");
        } catch (Exception e) {
            assertRemoteException(e, runtimeException);
        }
        assertThat(logFixture.getRawMessages()).contains("Error occurred in " + info + " ping.");
    }

    @Test
    void shouldReportCurrentStatus() throws Exception {
        JobIdentifier jobId = new JobIdentifier(new StageIdentifier("pipelineName", 1, "stageName", "1"), "job");
        buildRepository.reportCurrentStatus(info, jobId, JobState.Building);
        verify(agentService).updateRuntimeInfo(info);
        verify(repositoryService).updateStatusFromAgent(jobId, JobState.Building, info.getUUId());
        verify(jobStatusTopic).post(new JobStatusMessage(jobId, JobState.Building, info.getUUId()));
        assertThat(logFixture.getRawMessages()).contains(String.format("[%s] is reporting status [%s] for [%s]", info.agentInfoDebugString(), JobState.Building, jobId.toFullString()));
    }

    @Test
    void shouldLogAgentReportingStatusExceptions() throws Exception {
        JobIdentifier jobId = new JobIdentifier(new StageIdentifier("pipelineName", 1, "stageName", "1"), "job");
        RuntimeException runtimeException = new RuntimeException("holy smoke");
        doThrow(runtimeException).when(repositoryService).updateStatusFromAgent(jobId, JobState.Building, info.getUUId());
        try {
            buildRepository.reportCurrentStatus(info, jobId, JobState.Building);
            fail("should have propagated exception raised by build repository service");
        } catch (Exception e) {
            assertRemoteException(e, runtimeException);
        }
        assertThat(logFixture.getRawMessages()).contains(String.format("Exception occurred when [%s] tries to report status [%s] for [%s]", info.agentInfoDebugString(), JobState.Building, jobId.toFullString()));
    }

    @Test
    void shouldReportResult() {
        JobIdentifier jobId = new JobIdentifier(new StageIdentifier("pipelineName", 1, "stageName", "1"), "job");
        buildRepository.reportCompleting(info, jobId, JobResult.Passed);
        verify(repositoryService).completing(jobId, JobResult.Passed, info.getUUId());
        verify(agentService).updateRuntimeInfo(info);
        assertThat(logFixture.getRawMessages()).contains(String.format("[%s] is reporting result [%s] for [%s]", info.agentInfoDebugString(), JobResult.Passed, jobId.toFullString()));
    }

    @Test
    void shouldLogReportResultException() {
        JobIdentifier jobId = new JobIdentifier(new StageIdentifier("pipelineName", 1, "stageName", "1"), "job");
        RuntimeException runtimeException = new RuntimeException("holy smoke");
        doThrow(runtimeException).when(repositoryService).completing(jobId, JobResult.Passed, info.getUUId());
        try {
            buildRepository.reportCompleting(info, jobId, JobResult.Passed);
            fail("should have propagated exception raised by build repository service");
        } catch (Exception e) {
            assertRemoteException(e, runtimeException);
        }
        assertThat(logFixture.getRawMessages()).contains(String.format("Exception occurred when [%s] tries to report result [%s] for [%s]", info.agentInfoDebugString(), JobResult.Passed, jobId.toFullString()));
    }

    @Test
    void shouldReportCompletedWithResult() throws Exception {
        JobIdentifier jobId = new JobIdentifier(new StageIdentifier("pipelineName", 1, "stageName", "1"), "job");

        buildRepository.reportCompleted(info, jobId, JobResult.Passed);

        verify(repositoryService).completing(jobId, JobResult.Passed, info.getUUId());
        verify(agentService).updateRuntimeInfo(info);
        verify(repositoryService).updateStatusFromAgent(jobId, JobState.Completed, info.getUUId());
        assertThat(logFixture.getRawMessages()).contains(String.format("[%s] is reporting status and result [%s, %s] for [%s]", info.agentInfoDebugString(), JobState.Completed, JobResult.Passed, jobId.toFullString()));
    }

    @Test
    void shouldLogReportResultExceptionDuringReportCompletedFailure() {
        JobIdentifier jobId = new JobIdentifier(new StageIdentifier("pipelineName", 1, "stageName", "1"), "job");
        RuntimeException runtimeException = new RuntimeException("holy smoke");
        doThrow(runtimeException).when(repositoryService).completing(jobId, JobResult.Passed, info.getUUId());
        try {
            buildRepository.reportCompleted(info, jobId, JobResult.Passed);
            fail("should have propagated exception raised by build repository service");
        } catch (Exception e) {
            assertRemoteException(e, runtimeException);
        }
        assertThat(logFixture.getRawMessages()).contains(String.format("Exception occurred when [%s] tries to report status and result [%s, %s] for [%s]", info.agentInfoDebugString(), JobState.Completed, JobResult.Passed,
                jobId.toFullString()));
    }

    @Test
    void shouldUnderstandIfTestIsIgnored() {
        JobIdentifier jobId = new JobIdentifier(new StageIdentifier("pipelineName", 1, "stageName", "1"), "job");
        jobId.setBuildId(12l);
        buildRepository.isIgnored(jobId);
        verify(repositoryService).isCancelledOrRescheduled(jobId.getBuildId());
    }

    @Test
    void shouldThrowRemoteExceptionWhenIgnoreJobThrowsException() {
        JobIdentifier jobId = new JobIdentifier(new StageIdentifier("pipelineName", 1, "stageName", "1"), "job");
        jobId.setBuildId(12l);
        RuntimeException runtimeException = new RuntimeException("holy smoke");
        doThrow(runtimeException).when(repositoryService).isCancelledOrRescheduled(jobId.getBuildId());
        try {
            buildRepository.isIgnored(jobId);
            fail("should throw exception");
        } catch (Exception e) {
            assertRemoteException(e, runtimeException);
        }
    }

    @Test
    void getCookieShouldReturnAssignedCookie() {
        when(agentService.assignCookie(info.getIdentifier())).thenReturn("cookie");
        assertThat(buildRepository.getCookie(info.getIdentifier(), "/foo/bar")).isEqualTo("cookie");
        assertThat(logFixture.getRawMessages()).contains("[Agent Cookie] Agent [Agent [host, 192.168.1.1, uuid]] at location [/foo/bar] asked for a new cookie, assigned [cookie]");
    }

    @Test
    void GetCookieShouldThrowExceptionWhenAssigningCookieThrowsException() {
        RuntimeException runtimeException = new RuntimeException("holy smoke");
        when(agentService.assignCookie(info.getIdentifier())).thenThrow(runtimeException);
        try {
            buildRepository.getCookie(info.getIdentifier(), "/foo/bar");
            fail("should propagate exception raised by agent service assignCookie");
        } catch (Exception e) {
            assertRemoteException(e, runtimeException);
        }
    }

    private void assertRemoteException(Exception exception, RuntimeException innerException) {
        assertThat(exception).isInstanceOf(RemoteAccessException.class);
        assertThat(exception.getCause()).isSameAs(innerException);
    }

}
