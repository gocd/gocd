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
package com.thoughtworks.go.apiv1.jobinstance

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.jobinstance.representers.JobInstanceRepresenter
import com.thoughtworks.go.apiv1.jobinstance.representers.JobInstancesRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.config.exceptions.NotAuthorizedException
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.domain.JobInstances
import com.thoughtworks.go.domain.NullJobInstance
import com.thoughtworks.go.domain.PipelineRunIdInfo
import com.thoughtworks.go.helper.JobInstanceMother
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.JobInstanceService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import java.util.stream.Stream

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

@MockitoSettings(strictness = Strictness.LENIENT)
class JobInstanceControllerV1Test implements SecurityServiceTrait, ControllerTrait<JobInstanceControllerV1> {

  @Mock
  private JobInstanceService jobInstanceService


  @Override
  JobInstanceControllerV1 createControllerInstance() {
    new JobInstanceControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), jobInstanceService)
  }

  @Nested
  class History {

    String pipelineName = "up42"
    String stageName = "run-tests"
    String jobName = "java"

    @BeforeEach
    void setUp() {
      when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true)
    }

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "getHistoryInfo"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history'), [:])
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
      void 'should get job history'() {
        def jobInstances = new JobInstances()
        def jobInstance = JobInstanceMother.completed(jobName)
        jobInstance.setId(2)
        jobInstances.add(jobInstance)
        def runIdInfo = new PipelineRunIdInfo(5, 2)

        when(jobInstanceService.getOldestAndLatestJobInstanceId(eq(currentUsername()), eq(pipelineName), eq(stageName), eq(jobName))).thenReturn(runIdInfo)
        when(jobInstanceService.getJobHistoryViaCursor(eq(currentUsername()), eq(pipelineName), eq(stageName), eq(jobName), anyLong(), anyLong(), anyInt())).thenReturn(jobInstances)

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history'), [:])

        def expectedJson = toObjectString({ JobInstancesRepresenter.toJSON(it, jobInstances, runIdInfo) })

        assertThatResponse()
          .isOk()
          .hasBody(expectedJson)

      }

      @Test
      void 'should render job history after the specified cursor'() {
        def jobInstances = new JobInstances()
        for (int i = 1; i < 2; i++) {
          def jobInstance = JobInstanceMother.completed(jobName)
          jobInstance.setId(i + 2)
          jobInstances.add(jobInstance)
        }
        def runIdInfo = new PipelineRunIdInfo(5, 2)

        when(jobInstanceService.getOldestAndLatestJobInstanceId(eq(currentUsername()), eq(pipelineName), eq(stageName), eq(jobName))).thenReturn(runIdInfo)
        when(jobInstanceService.getJobHistoryViaCursor(eq(currentUsername()), eq(pipelineName), eq(stageName), eq(jobName), anyLong(), anyLong(), anyInt())).thenReturn(jobInstances)

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?after=3")

        verify(jobInstanceService).getJobHistoryViaCursor(any(Username.class), eq(pipelineName), eq(stageName), eq(jobName), eq(3L), eq(0L), eq(10))

        def expectedJson = toObjectString({
          JobInstancesRepresenter.toJSON(it, jobInstances, runIdInfo)
        })

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
      }

      @Test
      void 'should render job history before the specified cursor'() {
        def jobInstances = new JobInstances()
        for (int i = 3; i < 5; i++) {
          def jobInstance = JobInstanceMother.completed(jobName)
          jobInstance.setId(i + 2)
          jobInstances.add(jobInstance)
        }
        def runIdInfo = new PipelineRunIdInfo(7, 2)

        when(jobInstanceService.getOldestAndLatestJobInstanceId(eq(currentUsername()), eq(pipelineName), eq(stageName), eq(jobName))).thenReturn(runIdInfo)
        when(jobInstanceService.getJobHistoryViaCursor(eq(currentUsername()), eq(pipelineName), eq(stageName), eq(jobName), anyLong(), anyLong(), anyInt())).thenReturn(jobInstances)

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?before=3")

        verify(jobInstanceService).getJobHistoryViaCursor(any(Username.class), eq(pipelineName), eq(stageName), eq(jobName), eq(0L), eq(3L), eq(10))

        def expectedJson = toObjectString({
          JobInstancesRepresenter.toJSON(it, jobInstances, runIdInfo)
        })

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
      }

      @Test
      void 'should throw if the after cursor is specified as a invalid integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?after=abc")

        verifyZeroInteractions(jobInstanceService)

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter 'after', if specified, must be a positive integer.")
      }

      @Test
      void 'should throw if the before cursor is specified as a invalid integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?before=abc")

        verifyZeroInteractions(jobInstanceService)

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter 'before', if specified, must be a positive integer.")
      }

      @ParameterizedTest
      @MethodSource("pageSizes")
      void 'should throw error if page_size is not between 10 and 100'(String input) {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?page_size=" + input)

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter 'page_size', if specified must be a number between 10 and 100.")
      }

      static Stream<Arguments> pageSizes() {
        return Stream.of(
          Arguments.of("7"),
          Arguments.of("107"),
          Arguments.of("-10"),
          Arguments.of("abc")
        )
      }
    }

  }

  @Nested
  class Instance {
    String pipelineName = "up42"
    String stageName = "run-tests"
    String jobName = "java"

    @BeforeEach
    void setUp() {
      when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true)
    }

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "getInstanceInfo"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(pipelineName, 2, stageName, 2, jobName), [:])
      }

      @Override
      String getPipelineName() {
        return Instance.this.pipelineName
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }

      @Test
      void 'should return job instance for given counters'() {
        def jobInstance = getJobInstance()
        when(jobInstanceService.findJobInstance(eq(pipelineName), eq(stageName), eq(jobName), eq(1), eq(1), eq(currentUsername()))).thenReturn(jobInstance)

        getWithApiHeader(controller.controllerPath(pipelineName, 1, stageName, 1, jobName), [:])

        def expectedJson = toObjectString({
          JobInstanceRepresenter.toJSON(it, jobInstance)
        })

        assertThatResponse()
          .isOk()
          .hasBody(expectedJson)
      }

      @Test
      void 'should return null job instance if the job has not been ran for the given counters'() {
        def nullJobInstance = new NullJobInstance(jobName)
        when(jobInstanceService.findJobInstance(eq(pipelineName), eq(stageName), eq(jobName), eq(10), eq(10), eq(currentUsername()))).thenReturn(nullJobInstance)

        getWithApiHeader(controller.controllerPath(pipelineName, 10, stageName, 10, jobName), [:])

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("No job instance was found for 'up42/10/run-tests/10/java'.")
      }

      @Test
      void 'should throw error if pipeline counter is specified below 0'() {
        getWithApiHeader(controller.controllerPath(pipelineName, -1, stageName, 1, jobName), [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The params 'pipeline_counter' must be a number greater than 0.")
      }

      @Test
      void 'should throw error if pipeline counter is not an integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, 'abc', stageName, 1, jobName), [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The params 'pipeline_counter' must be a number greater than 0.")
      }

      @Test
      void 'should throw error if stage counter is specified below 0'() {
        getWithApiHeader(controller.controllerPath(pipelineName, 1, stageName, -1, jobName), [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The params 'stage_counter' must be a number greater than 0.")
      }

      @Test
      void 'should throw error if stage counter is not an integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, 1, stageName, 'abc', jobName), [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The params 'stage_counter' must be a number greater than 0.")
      }

      @Test
      void 'should render error if pipeline does not exist'() {
        doThrow(new RecordNotFoundException(EntityType.Pipeline, pipelineName))
          .when(jobInstanceService).findJobInstance(eq(pipelineName), eq(stageName), eq(jobName), eq(10), eq(10), eq(currentUsername()))

        getWithApiHeader(controller.controllerPath(pipelineName, 10, stageName, 10, jobName), [:])

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Pipeline with name 'up42' was not found!")
      }

      @Test
      void 'should render error if the user does not have permission to view the pipeline'() {
        doThrow(new NotAuthorizedException("some message"))
          .when(jobInstanceService).findJobInstance(eq(pipelineName), eq(stageName), eq(jobName), eq(10), eq(10), eq(currentUsername()))

        getWithApiHeader(controller.controllerPath(pipelineName, 10, stageName, 10, jobName), [:])

        assertThatResponse()
          .isUnauthorized()
          .hasJsonMessage("some message")
      }

      def getJobInstance() {
        def jobInstance = JobInstanceMother.completed(jobName)

        return jobInstance
      }
    }
  }
}
