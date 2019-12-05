/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.helper.JobInstanceMother
import com.thoughtworks.go.server.service.JobInstanceService
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.server.util.Pagination
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class JobInstanceControllerV1Test implements SecurityServiceTrait, ControllerTrait<JobInstanceControllerV1> {

  @Mock
  private JobInstanceService jobInstanceService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  JobInstanceControllerV1 createControllerInstance() {
    new JobInstanceControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), jobInstanceService)
  }

  @Nested
  class History {

    String pipelineName = "up42"
    String stageName = "run-tests"
    String jobName = "java"
    String offset = "1"

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
        when(jobInstanceService.getJobHistoryCount(eq(pipelineName), eq(stageName), eq(jobName))).thenReturn(20)
        when(jobInstanceService.findJobHistoryPage(eq(pipelineName), eq(stageName), anyString(), any(Pagination.class), anyString(), any() as HttpOperationResult)).thenReturn(getJobInstances())

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history'), [:])

        def expectedJson = toObjectString({
          JobInstancesRepresenter.toJSON(it, getJobInstances(), Pagination.pageStartingAt(0, 20, 10))
        })

        assertThatResponse()
          .isOk()
          .hasBody(expectedJson)

      }

      @Test
      void 'should get job history with offset'() {
        when(jobInstanceService.getJobHistoryCount(eq(pipelineName), eq(stageName), eq(jobName))).thenReturn(20)
        when(jobInstanceService.findJobHistoryPage(eq(pipelineName), eq(stageName), anyString(), any(Pagination.class), anyString(), any() as HttpOperationResult)).thenReturn(getJobInstances())

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?offset=2", [:])

        def expectedJson = toObjectString({
          JobInstancesRepresenter.toJSON(it, getJobInstances(), Pagination.pageStartingAt(2, 20, 10))
        })

        assertThatResponse()
          .isOk()
          .hasBody(expectedJson)
      }

      @Test
      void 'should get job history with offset and page size'() {
        when(jobInstanceService.getJobHistoryCount(eq(pipelineName), eq(stageName), eq(jobName))).thenReturn(20)
        when(jobInstanceService.findJobHistoryPage(eq(pipelineName), eq(stageName), anyString(), any(Pagination.class), anyString(), any() as HttpOperationResult)).thenReturn(getJobInstances())

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?page_size=25&offset=15", [:])

        def expectedJson = toObjectString({
          JobInstancesRepresenter.toJSON(it, getJobInstances(), Pagination.pageStartingAt(15, 20, 25))
        })

        assertThatResponse()
          .isOk()
          .hasBody(expectedJson)
      }

      @Test
      void 'should throw error if offset is below 0'() {

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?offset=-1")

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter `offset`, if specified must be a number greater or equal to 0.")
      }

      @Test
      void 'should throw error if offset is not an integer'() {

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?offset=abc")

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter `offset`, if specified must be a number greater or equal to 0.")
      }

      @Test
      void 'should throw error if page_size is below 10'() {

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?page_size=7")

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter `page_size`, if specified must be a number between 10 and 100.")
      }

      @Test
      void 'should throw error if page_size is a negative integer'() {

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?page_size=-7")

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter `page_size`, if specified must be a number between 10 and 100.")
      }

      @Test
      void 'should throw error if page_size is not an integer'() {

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'history') + "?page_size=abc")

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter `page_size`, if specified must be a number between 10 and 100.")
      }

      def getJobInstances() {
        def jobInstance = JobInstanceMother.completed(jobName)

        return new JobInstances(jobInstance)
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
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'instance', 2, 2), [:])
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
        when(jobInstanceService.findJobInstance(eq(pipelineName), eq(stageName), eq(jobName), eq(1), eq(1), anyString())).thenReturn(getJobInstance())

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'instance', 1, 1), [:])

        def expectedJson = toObjectString({
          JobInstanceRepresenter.toJSON(it, getJobInstance())
        })

        assertThatResponse()
          .isOk()
          .hasBody(expectedJson)
      }

      @Test
      void 'should return null job instance if the job has not been ran for the given counters'() {
        def nullJobInstance = new NullJobInstance(jobName)
        when(jobInstanceService.findJobInstance(eq(pipelineName), eq(stageName), eq(jobName), eq(10), eq(10), anyString())).thenReturn(nullJobInstance)

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'instance', 10, 10), [:])

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("No job instance was found for 'up42/10/run-tests/10/java'.")
      }

      @Test
      void 'should throw error if pipeline counter is specified below 0'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'instance', -1, 1), [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The params 'pipeline_counter' must be a number greater than 0.")
      }

      @Test
      void 'should throw error if pipeline counter is not an integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'instance', 'abc', 1), [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The params 'pipeline_counter' must be a number greater than 0.")
      }

      @Test
      void 'should throw error if stage counter is specified below 0'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'instance', 1, -1), [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The params 'stage_counter' must be a number greater than 0.")
      }

      @Test
      void 'should throw error if stage counter is not an integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'instance', 1, 'abc'), [:])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The params 'stage_counter' must be a number greater than 0.")
      }

      @Test
      void 'should render error if pipeline does not exist'() {
        doThrow(new RecordNotFoundException(EntityType.Pipeline, pipelineName))
          .when(jobInstanceService).findJobInstance(eq(pipelineName), eq(stageName), eq(jobName), eq(10), eq(10), anyString())

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'instance', 10, 10), [:])

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Pipeline with name 'up42' was not found!")
      }

      @Test
      void 'should render error if the user does not have permission to view the pipeline'() {
        doThrow(new NotAuthorizedException("some message"))
          .when(jobInstanceService).findJobInstance(eq(pipelineName), eq(stageName), eq(jobName), eq(10), eq(10), anyString())

        getWithApiHeader(controller.controllerPath(pipelineName, stageName, jobName, 'instance', 10, 10), [:])

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
