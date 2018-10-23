/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.stageoperations

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.domain.*
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

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class StageOperationsControllerV1Test implements SecurityServiceTrait, ControllerTrait<StageOperationsControllerV1> {
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
  StageOperationsControllerV1 createControllerInstance() {
    return new StageOperationsControllerV1(scheduleService, stageService, new ApiAuthenticationHelper(securityService, goConfigService), pipelineService)
  }

  @Nested
  class RerunFailedJobs {
    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "rerunFailedJobs"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-failed-jobs'), [:])
      }

      @Override
      String getPipelineName() {
        return "up42"
      }
    }

    @Nested
    class AsGroupOperateUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupOperateUser("up42")
      }

      @Test
      void 'reruns failed-jobs for a stage'() {
        Stage stage = mock(Stage)
        String expectedResponseBody = "Request to rerun job(s) is accepted"

        when(stageService.findStageWithIdentifier(eq("up42"), eq(3), eq("stage1"), eq("1"), anyString(), any() as HttpOperationResult)).thenReturn(stage)
        when(scheduleService.rerunFailedJobs(any() as Stage, any() as HttpOperationResult)).then({ InvocationOnMock invocation ->
          HttpOperationResult operationResult = invocation.getArguments().last()
          operationResult.accepted(expectedResponseBody, "", HealthStateType.general(HealthStateScope.forStage("up42", "stage1")))
          return stage
        })

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-failed-jobs'), [:])

        assertThatResponse()
          .isAccepted()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(expectedResponseBody)

        verify(scheduleService).rerunFailedJobs(eq(stage), any() as HttpOperationResult)
      }

      @Test
      void 'should not call schedule service if stage is instance of NullStage'() {
        when(stageService.findStageWithIdentifier(anyString(), anyInt(), anyString(), anyString(), anyString(), any() as HttpOperationResult)).thenReturn(new NullStage("foo"))

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-failed-jobs'), [:])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Not Found { Stage 'stage1' with counter '1' not found. Please make sure specified stage or stage run with specified counter exists. }")

        verifyZeroInteractions(scheduleService)
      }

      @Test
      void 'should not call schedule service if stage does not exist'() {
        when(stageService.findStageWithIdentifier(anyString(), anyInt(), anyString(), anyString(), anyString(), any() as HttpOperationResult)).thenReturn(null)

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-failed-jobs'), [:])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Not Found { Stage 'stage1' with counter '1' not found. Please make sure specified stage or stage run with specified counter exists. }")

        verifyZeroInteractions(scheduleService)
      }
    }
  }

  @Nested
  class RerunSelectedJobs {
    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "rerunSelectedJobs"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-selected-jobs'), ["jobs": ["job1"]])
      }

      @Override
      String getPipelineName() {
        return "up42"
      }
    }

    @Nested
    class AsAGroupOperateUser {
      private Stage stage

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupOperateUser("up42")

        stage = mock(Stage)
        when(stage.getIdentifier()).thenReturn(new StageIdentifier("up42/1/stage1/1"))
        when(stage.getJobInstances()).thenReturn(new JobInstances(
          new JobInstance("test"),
          new JobInstance("build"),
          new JobInstance("upload")
        ))
      }

      @Test
      void 'should rerun selected jobs in stage'() {
        String expectedMessage = "Request to rerun job(s) is accepted"
        List<String> jobs = ["test", "build", "upload"]

        when(stageService.findStageWithIdentifier(eq("up42"), eq(3), eq("stage1"), eq("1"), anyString(), any() as HttpOperationResult)).thenReturn(this.stage)
        when(scheduleService.rerunJobs(eq(this.stage), eq(jobs), any() as HttpOperationResult))
          .then({ InvocationOnMock invocation ->
          HttpOperationResult result = invocation.getArguments().last()
          result.accepted(expectedMessage, "", HealthStateType.general(HealthStateScope.forStage("up42", "stage1")))
          return this.stage
        })

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-selected-jobs'), ["jobs": jobs])

        assertThatResponse()
          .isAccepted()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(expectedMessage)

        verify(scheduleService).rerunJobs(eq(this.stage), eq(jobs), any() as HttpOperationResult)
      }

      @Test
      void 'should error out when any of the requested job is not in stage'() {
        List<String> jobs = ["test", "build", "integration", "functional"]

        when(stageService.findStageWithIdentifier(eq("up42"), eq(3), eq("stage1"), eq("1"), anyString(), any() as HttpOperationResult)).thenReturn(this.stage)

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-selected-jobs'), ["jobs": jobs])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Job(s) [integration, functional] does not exist in stage 'up42/1/stage1/1'.")

        verifyZeroInteractions(scheduleService)
      }

      @Test
      void 'should error out if the request body does not contain property jobs'() {
        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-selected-jobs'), ["not-jobs": ["download", "build", "uploads"]])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Could not read property 'jobs' in request body")

        verifyZeroInteractions(scheduleService, stageService)
      }

      @Test
      void 'should error out if the request body has property jobs with non string array value'() {
        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-selected-jobs'), ["jobs": "not-an-array"])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Could not read property 'jobs' as a JsonArray containing string in `{\\\"jobs\\\":\\\"not-an-array\\\"}`")

        verifyZeroInteractions(scheduleService, stageService)
      }

      @Test
      void 'should not call schedule service if stage is instance of NullStage'() {
        when(stageService.findStageWithIdentifier(anyString(), anyInt(), anyString(), anyString(), anyString(), any() as HttpOperationResult)).thenReturn(new NullStage("foo"))

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-selected-jobs'), ["jobs": ["job1"]])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Not Found { Stage 'stage1' with counter '1' not found. Please make sure specified stage or stage run with specified counter exists. }")

        verifyZeroInteractions(scheduleService)
      }

      @Test
      void 'should not call schedule service if stage does not exist'() {
        when(stageService.findStageWithIdentifier(anyString(), anyInt(), anyString(), anyString(), anyString(), any() as HttpOperationResult)).thenReturn(null)

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'run-selected-jobs'), ["jobs": ["job1"]])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Not Found { Stage 'stage1' with counter '1' not found. Please make sure specified stage or stage run with specified counter exists. }")

        verifyZeroInteractions(scheduleService)
      }
    }
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
