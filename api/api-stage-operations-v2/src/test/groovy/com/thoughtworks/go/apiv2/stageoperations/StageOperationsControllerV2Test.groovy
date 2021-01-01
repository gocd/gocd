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
package com.thoughtworks.go.apiv2.stageoperations

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.domain.Stage
import com.thoughtworks.go.server.service.PipelineService
import com.thoughtworks.go.server.service.ScheduleService
import com.thoughtworks.go.server.service.SchedulingCheckerService
import com.thoughtworks.go.server.service.StageService
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.serverhealth.HealthStateScope
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineGroupOperateUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class StageOperationsControllerV2Test implements SecurityServiceTrait, ControllerTrait<StageOperationsControllerV2> {
  @Mock
  ScheduleService scheduleService

  @Mock
  StageService stageService

  @Mock
  SchedulingCheckerService schedulingChecker

  @Mock
  PipelineService pipelineService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  StageOperationsControllerV2 createControllerInstance() {
    return new StageOperationsControllerV2(scheduleService, new ApiAuthenticationHelper(securityService, goConfigService), pipelineService)
  }

  @Nested
  class Run {
    String pipelineName = "up42"
    String pipelineCounter = "3"
    String stageName = "run-tests"

    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "triggerStage"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run'), [:])
      }

      @Override
      String getPipelineName() {
        return Run.this.pipelineName
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupOperateUser(pipelineName)

      }

      @Test
      void 'runs a stage'() {
        String acceptanceMessage = "Request to run stage ${[pipelineName, pipelineCounter, stageName].join("/")} accepted"
        HttpOperationResult result
        doAnswer({ InvocationOnMock invocation ->
          result = invocation.getArgument(3)
          result.accepted(acceptanceMessage, "", HealthStateType.general(HealthStateScope.forStage(pipelineName, stageName)))
          return mock(Stage)
        }).when(scheduleService).rerunStage(eq(pipelineName), eq(pipelineCounter.toInteger()), eq(stageName), any() as HttpOperationResult)
        when(pipelineService.resolvePipelineCounter(pipelineName, pipelineCounter)).thenReturn(Optional.of(pipelineCounter.toInteger()))
        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run'), [:])

        assertThatResponse()
          .isAccepted()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(acceptanceMessage)

        verify(scheduleService).rerunStage(pipelineName, pipelineCounter.toInteger(), stageName, result)
      }

      @Test
      void 'reports errors'() {
        when(pipelineService.resolvePipelineCounter(eq(pipelineName), eq(pipelineCounter))).thenReturn(Optional.of(pipelineCounter.toInteger()))
        when(scheduleService.rerunStage(eq(pipelineName), eq(pipelineCounter.toInteger()), eq(stageName), any() as ScheduleService.ErrorConditionHandler)).thenThrow(new RuntimeException("bewm."))
        doAnswer({ InvocationOnMock invocation -> invocation.callRealMethod() }).
          when(scheduleService).rerunStage(eq(pipelineName), eq(pipelineCounter.toInteger()), eq(stageName), any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run'), [:])

        assertThatResponse()
          .isInternalServerError()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Stage rerun request for stage [${[pipelineName, pipelineCounter, stageName].join("/")}] " +
          "could not be completed because of an unexpected failure. Cause: bewm.")
      }
    }
  }
}
