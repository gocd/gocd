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

package com.thoughtworks.go.apiv1.templateauthorization

import com.thoughtworks.go.apiv1.internalagent.InternalAgentControllerV1
import com.thoughtworks.go.apiv1.internalagent.representers.*
import com.thoughtworks.go.config.Agent
import com.thoughtworks.go.domain.AgentRuntimeStatus
import com.thoughtworks.go.domain.JobIdentifier
import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.remote.AgentInstruction
import com.thoughtworks.go.remote.request.*
import com.thoughtworks.go.remote.work.NoWork
import com.thoughtworks.go.server.messaging.BuildRepositoryMessageProducer
import com.thoughtworks.go.server.service.AgentRuntimeInfo
import com.thoughtworks.go.spark.ControllerTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalAgentControllerV1Test implements ControllerTrait<InternalAgentControllerV1> {
  @Mock
  BuildRepositoryMessageProducer buildRepositoryMessageProducer;

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalAgentControllerV1 createControllerInstance() {
    new InternalAgentControllerV1(buildRepositoryMessageProducer)
  }

  @Nested
  class ping {
    @Test
    void 'should return an agent instruction'() {
      def agent = new Agent("uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")

      when(buildRepositoryMessageProducer.ping(runtimeInfo)).thenReturn(AgentInstruction.NONE)
      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/ping"), headers, PingRequestRepresenter.toJSON(new PingRequest(runtimeInfo)))

      assertThatResponse()
              .isOk()
              .hasBodyWithJson(AgentInstructionRepresenter.toJSON(AgentInstruction.NONE))
    }

    @Test
    void 'ensure agent is making a request for itself'() {
      def agent = new Agent("different_agent_uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/ping"), headers, PingRequestRepresenter.toJSON(new PingRequest(runtimeInfo)))

      assertThatResponse()
              .isForbidden()
    }
  }

  @Nested
  class reportCurrentStatus {
    @Test
    void 'should report current status'() {
      def agent = new Agent("uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/report_current_status"), headers, ReportCurrentStatusRequestRepresenter.toJSON(
              new ReportCurrentStatusRequest(runtimeInfo, jobIdentifier, JobState.Building)))

      verify(buildRepositoryMessageProducer).reportCurrentStatus(runtimeInfo, jobIdentifier, JobState.Building)
      assertThatResponse()
              .isOk()
              .hasBodyContaining("")
    }

    @Test
    void 'ensure agent is making a request for itself'() {
      def agent = new Agent("different_agent_uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/report_current_status"), headers, ReportCurrentStatusRequestRepresenter.toJSON(
              new ReportCurrentStatusRequest(runtimeInfo, jobIdentifier, JobState.Building)))

      assertThatResponse()
              .isForbidden()
    }
  }

  @Nested
  class reportCompleting {
    @Test
    void 'should report completing'() {
      def agent = new Agent("uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/report_completing"), headers, ReportCompleteStatusRequestRepresenter.toJSON(
              new ReportCompleteStatusRequest(runtimeInfo, jobIdentifier, JobResult.Passed)))

      verify(buildRepositoryMessageProducer).reportCompleting(runtimeInfo, jobIdentifier, JobResult.Passed)
      assertThatResponse()
              .isOk()
              .hasBodyContaining("")
    }

    @Test
    void 'ensure agent is making a request for itself'() {
      def agent = new Agent("different_agent_uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/report_completing"), headers, ReportCompleteStatusRequestRepresenter.toJSON(
              new ReportCompleteStatusRequest(runtimeInfo, jobIdentifier, JobResult.Passed)))

      assertThatResponse()
              .isForbidden()
    }
  }

  @Nested
  class reportCompleted {
    @Test
    void 'should report completed'() {
      def agent = new Agent("uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/report_completed"), headers, ReportCompleteStatusRequestRepresenter.toJSON(
              new ReportCompleteStatusRequest(runtimeInfo, jobIdentifier, JobResult.Passed)))

      verify(buildRepositoryMessageProducer).reportCompleted(runtimeInfo, jobIdentifier, JobResult.Passed)
      assertThatResponse()
              .isOk()
              .hasBodyContaining("")
    }

    @Test
    void 'ensure agent is making a request for itself'() {
      def agent = new Agent("different_agent_uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/report_completed"), headers, ReportCompleteStatusRequestRepresenter.toJSON(
              new ReportCompleteStatusRequest(runtimeInfo, jobIdentifier, JobResult.Passed)))

      assertThatResponse()
              .isForbidden()
    }
  }

  @Nested
  class isIgnored {
    @Test
    void 'should check if a job is ignored'() {
      def agent = new Agent("uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      when(buildRepositoryMessageProducer.isIgnored(runtimeInfo, jobIdentifier)).thenReturn(false)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/is_ignored"), headers, IsIgnoredRequestRepresenter.toJSON(
              new IsIgnoredRequest(runtimeInfo, jobIdentifier)))

      assertThatResponse()
              .isOk()
              .hasBodyContaining("false")
    }

    @Test
    void 'ensure agent is making a request for itself'() {
      def agent = new Agent("different_agent_uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/is_ignored"), headers, IsIgnoredRequestRepresenter.toJSON(
              new IsIgnoredRequest(runtimeInfo, jobIdentifier)))


      assertThatResponse()
              .isForbidden()
    }
  }

  @Nested
  class getCookie {
    @Test
    void 'should fetch a cookie'() {
      def agent = new Agent("uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")

      when(buildRepositoryMessageProducer.getCookie(runtimeInfo)).thenReturn("cookie")

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/get_cookie"), headers, GetCookieRequestRepresenter.toJSON(
              new GetCookieRequest(runtimeInfo)))

      assertThatResponse()
              .isOk()
              .hasBodyContaining("cookie")
    }

    @Test
    void 'ensure agent is making a request for itself'() {
      def agent = new Agent("different_agent_uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/get_cookie"), headers, GetCookieRequestRepresenter.toJSON(
              new GetCookieRequest(runtimeInfo)))

      assertThatResponse()
              .isForbidden()
    }
  }

  @Nested
  class getWork {
    @Test
    void 'should get work for an agent'() {
      def agent = new Agent("uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")

      when(buildRepositoryMessageProducer.getWork(runtimeInfo)).thenReturn(new NoWork())

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/get_work"), headers, GetWorkRequestRepresenter.toJSON(
              new GetWorkRequest(runtimeInfo)))

      assertThatResponse()
              .isOk()
              .hasBodyContaining(WorkRepresenter.toJSON(new NoWork()))
    }

    @Test
    void 'ensure agent is making a request for itself'() {
      def agent = new Agent("different_agent_uuid", "localhost", "176.19.4.1")
      def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
              "20.1.0", "20.9.0")
      def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
              "1", "some_job", 1111L)

      def headers = [
              'accept'      : controller.mimeType,
              'content-type': 'application/json',
              'X-Agent-GUID': 'uuid'
      ]
      postWithApiHeader(controller.controllerPath("/get_work"), headers, GetWorkRequestRepresenter.toJSON(
              new GetWorkRequest(runtimeInfo)))

      assertThatResponse()
              .isForbidden()
    }
  }
}