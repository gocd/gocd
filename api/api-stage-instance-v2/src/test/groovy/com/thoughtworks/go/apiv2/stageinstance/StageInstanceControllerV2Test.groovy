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
package com.thoughtworks.go.apiv2.stageinstance

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.stageinstance.representers.StageInstancesRepresenter
import com.thoughtworks.go.apiv2.stageinstance.representers.StageRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.*
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory
import com.thoughtworks.go.presentation.pipelinehistory.JobHistoryItem
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import com.thoughtworks.go.server.service.ScheduleService
import com.thoughtworks.go.server.service.StageService
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.server.service.result.LocalizedOperationResult
import com.thoughtworks.go.server.util.Pagination
import com.thoughtworks.go.serverhealth.HealthStateScope
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.PipelineGroupOperateUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv2.stageinstance.StageInstanceControllerV2.BAD_OFFSET_MSG
import static com.thoughtworks.go.apiv2.stageinstance.StageInstanceControllerV2.BAD_PAGE_SIZE_MSG
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class StageInstanceControllerV2Test implements SecurityServiceTrait, ControllerTrait<StageInstanceControllerV2> {

  @Mock
  private StageService stageService

  @Mock
  private ScheduleService scheduleService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  StageInstanceControllerV2 createControllerInstance() {
    new StageInstanceControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), stageService, scheduleService)
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
  class CancelStageByIdentifier {
    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "cancelStage"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'cancel'), [:])
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
      void 'cancels running stage'() {
        Stage stage = new Stage()
        stage.setId(42)

        String expectedResponseBody = "Stage is cancelled!"

        when(stageService.findStageWithIdentifier(eq("up42"), eq(3), eq("stage1"), eq("1"), anyString(), any() as HttpOperationResult)).thenReturn(stage)
        when(scheduleService.cancelAndTriggerRelevantStages(eq(stage.getId()), eq(currentUsername()), any() as LocalizedOperationResult)).then({ InvocationOnMock invocation ->
          LocalizedOperationResult operationResult = invocation.getArgument(2)
          operationResult.accepted(expectedResponseBody)
          return stage
        })

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'cancel'), [:])

        assertThatResponse()
          .isAccepted()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(expectedResponseBody)

        verify(scheduleService).cancelAndTriggerRelevantStages(eq(stage.getId()), eq(currentUsername()), any() as LocalizedOperationResult)
      }

      @Test
      void 'should not call schedule service if stage is instance of NullStage'() {
        when(stageService.findStageWithIdentifier(anyString(), anyInt(), anyString(), anyString(), anyString(), any() as HttpOperationResult)).thenReturn(new NullStage("foo"))

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'cancel'), [:])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Not Found { Stage 'stage1' with counter '1' not found. Please make sure specified stage or stage run with specified counter exists. }")

        verifyZeroInteractions(scheduleService)
      }

      @Test
      void 'should not call schedule service if stage does not exist'() {
        when(stageService.findStageWithIdentifier(anyString(), anyInt(), anyString(), anyString(), anyString(), any() as HttpOperationResult)).thenReturn(null)

        postWithApiHeader(controller.controllerPath("up42", "3", "stage1", "1", 'cancel'), [:])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Not Found { Stage 'stage1' with counter '1' not found. Please make sure specified stage or stage run with specified counter exists. }")

        verifyZeroInteractions(scheduleService)
      }
    }
  }

  @Nested
  class InstanceByCounter {
    String pipelineName = "up42"
    String pipelineCounter = "1"
    String stageName = "run-tests"
    String stageCounter = "1"

    @BeforeEach
    void setUp() {
      when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true)
    }

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "instanceByCounter"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, stageCounter), [:])
      }

      @Override
      String getPipelineName() {
        return InstanceByCounter.this.pipelineName
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }

      @Test
      void 'should get specified stage instance'() {
        when(stageService.findStageWithIdentifier(eq(pipelineName), eq(pipelineCounter.toInteger()), eq(stageName), eq(stageCounter), eq(currentUserLoginName().toString()), any() as HttpOperationResult)).thenReturn(getStageModel())

        getWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, stageCounter), [:])

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(getStageModel(), StageRepresenter)
      }

      def getStageModel() {
        def stageModel = new Stage()
        stageModel.setId(456)
        stageModel.setName('stage name')
        stageModel.setCounter(1)
        stageModel.setApprovalType('manual')
        stageModel.setApprovedBy('me')
        stageModel.setRerunOfCounter(1)
        stageModel.setIdentifier(new StageIdentifier('pipeline name', 213, 'stage name', '4'))
        stageModel.setJobInstances(new JobInstances(getJobInstance()))

        return stageModel
      }

      def getJobInstance() {
        def jobInstance = new JobInstance("job")
        jobInstance.setId(1);
        jobInstance.setState(JobState.Assigned)
        jobInstance.setResult(JobResult.Unknown)
        jobInstance.setAgentUuid("uuid")
        jobInstance.setScheduledDate(new Date(2018, 12, 21, 12, 30))
        jobInstance.setOriginalJobId(1)
        jobInstance.setTransitions(new JobStateTransitions(new JobStateTransition(JobState.Scheduled, new Date(2018, 12, 21, 12, 45)),
          new JobStateTransition(JobState.Assigned, null)))

        return jobInstance
      }

      @Test
      void 'should render 404 if stage cannot be found'() {

        HttpOperationResult result
        doAnswer({ InvocationOnMock invocation ->
          result = invocation.getArgument(5)
          result.notFound("not found", "", HealthStateType.general(HealthStateScope.forStage(pipelineName, stageName)))
          return mock(Stage)
        }).when(stageService).findStageWithIdentifier(eq(pipelineName), eq(pipelineCounter.toInteger()), eq(stageName), eq(stageCounter), any() as String, any() as HttpOperationResult)

        getWithApiHeader(controller.controllerPath(pipelineName, pipelineCounter, stageName, stageCounter), [:])

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("not found")
      }

    }
  }

  @Nested
  class History {
    String pipelineName = "up42"
    String stageName = "run-tests"
    String offset = "1"

    @BeforeEach
    void setUp() {
      when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true)
    }

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "history"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, 'history'), [:])
      }

      @Override
      String getPipelineName() {
        return History.this.pipelineName
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }

      @Test
      void 'should get stage history'() {
        when(stageService.getCount(eq(pipelineName), eq(stageName))).thenReturn(20)
        when(stageService.findDetailedStageHistoryByOffset(eq(pipelineName), eq(stageName), any() as Pagination, eq(currentUserLoginName().toString()), any() as HttpOperationResult)).thenReturn(getStageModels())

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, 'history'), [:])

        def expectedJson = toObjectString({
          StageInstancesRepresenter.toJSON(it, getStageModels(), Pagination.pageStartingAt(0, 20, 10))
        })

        assertThatResponse()
          .isOk()
          .hasBody(expectedJson)

      }

      @Test
      void 'should get stage history with offset'() {
        when(stageService.getCount(eq(pipelineName), eq(stageName))).thenReturn(20)
        when(stageService.findDetailedStageHistoryByOffset(eq(pipelineName), eq(stageName), any() as Pagination, eq(currentUserLoginName().toString()), any() as HttpOperationResult)).thenReturn(getStageModels())

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, 'history') + "?offset=" + offset, [:])

        def expectedJson = toObjectString({
          StageInstancesRepresenter.toJSON(it, getStageModels(), Pagination.pageStartingAt(1, 20, 10))
        })

        assertThatResponse()
          .isOk()
          .hasBody(expectedJson)
      }

      @Test
      void 'should get stage history with page size and offset'() {
        when(stageService.getCount(eq(pipelineName), eq(stageName))).thenReturn(20)
        when(stageService.findDetailedStageHistoryByOffset(eq(pipelineName), eq(stageName), any() as Pagination, eq(currentUserLoginName().toString()), any() as HttpOperationResult)).thenReturn(getStageModels())

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, 'history') + "?page_size=15&offset=" + offset, [:])

        def expectedJson = toObjectString({
          StageInstancesRepresenter.toJSON(it, getStageModels(), Pagination.pageStartingAt(1, 20, 15))
        })

        assertThatResponse()
          .isOk()
          .hasBody(expectedJson)
      }

      @Test
      void 'should throw error if page size is specified as less than 10'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, 'history') + "?page_size=5", [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage(BAD_PAGE_SIZE_MSG)
      }

      @Test
      void 'should throw error if page size is specified as greater than 100'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, 'history') + "?page_size=115", [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage(BAD_PAGE_SIZE_MSG)
      }

      @Test
      void 'should throw error if page size is an invalid integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, 'history') + "?page_size=abc", [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage(BAD_PAGE_SIZE_MSG)
      }

      @Test
      void 'should throw error if offset is specified as less than 0'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, 'history') + "?offset=-5", [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage(BAD_OFFSET_MSG)
      }

      @Test
      void 'should throw error if offset is an invalid integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, 'history') + "?offset=abc", [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage(BAD_OFFSET_MSG)
      }

      def getStageModels() {
        def jobHistoryItem = new JobHistoryItem("job", JobState.Completed, JobResult.Passed, new Date(2018, 12, 22, 11, 10))
        jobHistoryItem.setId(34)
        def jobHistory = new JobHistory()
        jobHistory.add(jobHistoryItem)
        def stageInstanceModel = new StageInstanceModel("stage", "3", jobHistory)
        stageInstanceModel.setId(21)
        def stageInstances = new StageInstanceModels()
        stageInstances.add(stageInstanceModel)

        return stageInstances
      }
    }
  }
}
