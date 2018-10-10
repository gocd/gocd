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
import com.thoughtworks.go.domain.JobInstance
import com.thoughtworks.go.domain.JobInstances
import com.thoughtworks.go.domain.Pipeline
import com.thoughtworks.go.domain.Stage
import com.thoughtworks.go.server.service.PipelineService
import com.thoughtworks.go.domain.StageIdentifier
import com.thoughtworks.go.server.service.PipelineService
import com.thoughtworks.go.server.service.ScheduleService
import com.thoughtworks.go.server.service.SchedulingCheckerService
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.server.service.result.OperationResult
import com.thoughtworks.go.serverhealth.HealthStateScope
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineGroupOperateUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.internal.util.reflection.FieldSetter
import org.mockito.invocation.InvocationOnMock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class StageOperationsControllerV1Test implements SecurityServiceTrait, ControllerTrait<StageOperationsControllerV1> {
  @Mock
  ScheduleService scheduleService

  @Mock
  SchedulingCheckerService schedulingChecker

  @Mock
  PipelineService pipelineService

  @Mock
  PipelineService pipelineService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  StageOperationsControllerV1 createControllerInstance() {
    new StageOperationsControllerV1(scheduleService, new ApiAuthenticationHelper(securityService, goConfigService), pipelineService)
  }

  @Nested
  class RerunJobs {
    String pipelineName = "up42"
    String pipelineCounter = "3"
    String stageName = "stage1"

    @Nested
    class RerunFailedJobsSecurity implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "rerunFailedJobs"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run-failed-jobs'), [:])
      }

      @Override
      String getPipelineName() {
        return RerunJobs.this.pipelineName
      }
    }

    @Nested
    class RerunSelectedJobsSecurity implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "rerunSelectedJobs"
      }

      @Override
      void makeHttpCall() {
        String requestBody = "{\"jobs\": [\"job1\"]}"
        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run-selected-jobs'), requestBody)
      }

      @Override
      String getPipelineName() {
        return RerunJobs.this.pipelineName
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
      void 'reruns failed-jobs for a stage'() {
        String acceptanceMessage = "Request to rerun job(s) is accepted"
        HttpOperationResult result

        doAnswer({ InvocationOnMock invocation ->
          result = invocation.getArgument(3)
          result.accepted(acceptanceMessage, "", HealthStateType.general(HealthStateScope.forStage(pipelineName, stageName)))
          return mock(Stage)
        }).when(scheduleService).rerunFailedJobs(eq(pipelineName), eq(pipelineCounter), eq(stageName), any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run-failed-jobs'), [:])

        assertThatResponse()
          .isAccepted()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(acceptanceMessage)

        verify(scheduleService).rerunFailedJobs(pipelineName, pipelineCounter, stageName, result)
      }

      @Test
      void 'rerunFailedJobs reports errors'() {
        def pipeline = mock(Pipeline)
        def stage = mock(Stage)

        FieldSetter.setField(scheduleService, ScheduleService.getDeclaredField("pipelineService"), pipelineService)
        FieldSetter.setField(scheduleService, ScheduleService.getDeclaredField("schedulingChecker"), schedulingChecker)

        when(stage.jobsWithResult(any())).thenReturn(new JobInstances(Arrays.asList(new JobInstance("job1"))))
        when(stage.getIdentifier()).thenReturn(new StageIdentifier("up42/3/run-failedJob/23"))
        when(pipeline.findStage(any())).thenReturn(stage)
        when(pipelineService.fullPipelineByCounterOrLabel(any(), any())).thenReturn(pipeline)
        when(schedulingChecker.canSchedule(any())).thenThrow(new RuntimeException("boom"))

        doAnswer({ InvocationOnMock invocation -> invocation.callRealMethod() }).
          when(scheduleService).rerunFailedJobs(eq(pipelineName), eq(pipelineCounter), eq(stageName), any() as HttpOperationResult)

        doAnswer({ InvocationOnMock invocation -> invocation.callRealMethod() }).
          when(scheduleService).rerunFailedJobs(eq(stage), any() as HttpOperationResult)

        doAnswer({ InvocationOnMock invocation -> invocation.callRealMethod() }).
          when(scheduleService).rerunJobs(eq(stage), any(), any() as HttpOperationResult)


        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run-failed-jobs'), [:])

        assertThatResponse()
          .isInternalServerError()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Job rerun request for job(s) [job1] could not be completed because of unexpected failure. Cause: boom\"")
      }

      @Test
      void 'rerunSelectedJobs reports errors'() {
        List<String> jobs = ["download", "build", "upload"]
        String requestBody = "{\n" +
          "  \"jobs\": [\"download\"]\n" +
          "}"

        def pipeline = mock(Pipeline)
        def stage = mock(Stage)

        FieldSetter.setField(scheduleService, ScheduleService.getDeclaredField("pipelineService"), pipelineService)
        FieldSetter.setField(scheduleService, ScheduleService.getDeclaredField("schedulingChecker"), schedulingChecker)

        when(stage.jobsWithResult(any())).thenReturn(new JobInstances(Arrays.asList(new JobInstance("download"))))
        when(stage.getJobInstances()).thenReturn(new JobInstances(Arrays.asList(new JobInstance("download"))))
        when(stage.getIdentifier()).thenReturn(new StageIdentifier("${[pipelineName, pipelineCounter, stageName].join("/")}/23"))
        when(pipeline.findStage(any())).thenReturn(stage)
        when(pipelineService.fullPipelineByCounterOrLabel(any(), any())).thenReturn(pipeline)
        when(schedulingChecker.canSchedule(any())).thenThrow(new RuntimeException("boom"))

        doAnswer({ InvocationOnMock invocation -> invocation.callRealMethod() }).
          when(scheduleService).rerunJobs(eq(stage), anyList(), any() as HttpOperationResult)

        doAnswer({ InvocationOnMock invocation -> invocation.callRealMethod() }).
          when(scheduleService).rerunSelectedJobs(eq(pipelineName), eq(pipelineCounter), eq(stageName), anyList(), any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run-selected-jobs'), requestBody)

        assertThatResponse()
          .isInternalServerError()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Job rerun request for job(s) [download] could not be completed because of unexpected failure. Cause: boom\"")
      }


      @Test
      void 'reruns selected-jobs for a stage'() {
        String acceptanceMessage = "Request to rerun job(s) is accepted"
        HttpOperationResult result

        List<String> jobs = ["download", "build", "upload"]
        String requestBody = "{\n" +
          "  \"jobs\": [\"download\", \"build\", \"upload\"]\n" +
          "}"

        doAnswer({ InvocationOnMock invocation ->
          result = invocation.getArgument(4)
          result.accepted(acceptanceMessage, "", HealthStateType.general(HealthStateScope.forStage(pipelineName, stageName)))
          return mock(Stage)
        }).when(scheduleService).rerunSelectedJobs(eq(pipelineName), eq(pipelineCounter), eq(stageName), eq(jobs), any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run-selected-jobs'), requestBody)

        assertThatResponse()
          .isAccepted()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(acceptanceMessage)

        verify(scheduleService).rerunSelectedJobs(pipelineName, pipelineCounter, stageName, jobs, result)
      }

      @Test
      void 'rerunSelectedJobs reports error on invalid request body'() {
        String acceptanceMessage = "Request to rerun job(s) is accepted"
        HttpOperationResult result

        List<String> jobs = ["download", "build", "upload"]
        String requestBody = "{\n" +
          "  \"jobss\": [\"download\", \"build\", \"upload\"]\n" +
          "}"

        postWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, 'run-selected-jobs'), requestBody)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Could not read property 'jobs' in request body")

        verify(scheduleService, times(0)).rerunSelectedJobs(pipelineName, pipelineCounter, stageName, jobs, result)
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
