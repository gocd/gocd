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
package com.thoughtworks.go.apiv1.agentjobhistory

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.agentjobhistory.representers.AgentJobHistoryRepresenter
import com.thoughtworks.go.domain.JobInstances
import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.helper.AgentInstanceMother
import com.thoughtworks.go.helper.JobInstanceMother
import com.thoughtworks.go.server.service.AgentService
import com.thoughtworks.go.server.service.JobInstanceService
import com.thoughtworks.go.server.ui.JobInstancesModel
import com.thoughtworks.go.server.ui.SortOrder
import com.thoughtworks.go.server.util.Pagination
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class AgentJobHistoryControllerV1Test implements SecurityServiceTrait, ControllerTrait<AgentJobHistoryControllerV1> {

  @Mock
  JobInstanceService jobInstanceService
  @Mock
  AgentService agentService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  AgentJobHistoryControllerV1 createControllerInstance() {
    new AgentJobHistoryControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), jobInstanceService, agentService)
  }

  @Nested
  class Index {

    @BeforeEach
    void setUp() {
      loginAsAdmin()
    }

    @Test
    void 'should render response'() {
      def uuid = "some-agent"
      def totalCompletedJobs = 100
      def pageSize = 50
      def offset = 0
      def sortOrder = SortOrder.DESC

      def pagination = Pagination.pageStartingAt(offset, totalCompletedJobs, pageSize)
      def jobInstance = JobInstanceMother.completed("blah", JobResult.Passed)

      when(agentService.findAgent(uuid)).thenReturn(AgentInstanceMother.idleWith(uuid))
      when(jobInstanceService.totalCompletedJobsCountOn(uuid)).thenReturn(totalCompletedJobs)
      when(jobInstanceService.completedJobsOnAgent(uuid, JobInstanceService.JobHistoryColumns.completed, sortOrder, pagination)).thenReturn(new JobInstancesModel(new JobInstances(jobInstance), pagination))

      getWithApiHeader(Routes.AgentJobHistory.forAgent(uuid))
      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)

      verify(jobInstanceService).totalCompletedJobsCountOn(uuid)
      verify(jobInstanceService).completedJobsOnAgent(uuid, JobInstanceService.JobHistoryColumns.completed, sortOrder, pagination)

      verifyNoMoreInteractions(jobInstanceService)
    }

    @Nested
    class QueryParamValidation {
      @Nested
      class Offset {
        @Test
        void 'should allow `offset` with value `0`'() {

          def pagination = Pagination.pageStartingAt(0, 42, 10)
          def jobInstance = JobInstanceMother.completed("blah", JobResult.Passed)
          def jobInstancesModel = new JobInstancesModel(new JobInstances(jobInstance), pagination)

          when(agentService.findAgent("some-agent")).thenReturn(AgentInstanceMother.idleWith("some-agent"))
          when(jobInstanceService.totalCompletedJobsCountOn(any())).thenReturn(42)
          when(jobInstanceService.completedJobsOnAgent(any(), any(), any(), any())).thenReturn(jobInstancesModel)

          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?offset=0")
          assertThatResponse()
            .isOk()
            .hasContentType(controller.mimeType)
            .hasBodyWithJsonObject(AgentJobHistoryRepresenter, "some-agent", jobInstancesModel)
        }

        @Test
        void 'should disallow negative offset'() {
          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?offset=-1")
          assertThatResponse()
            .isBadRequest()
            .hasContentType(controller.mimeType)
            .hasJsonMessage(AgentJobHistoryControllerV1.BAD_OFFSET_MSG)
        }

        @Test
        void 'should allow positive offset'() {
          def pagination = Pagination.pageStartingAt(1, 42, 10)
          def jobInstance = JobInstanceMother.completed("blah", JobResult.Passed)
          def jobInstancesModel = new JobInstancesModel(new JobInstances(jobInstance), pagination)

          when(agentService.findAgent("some-agent")).thenReturn(AgentInstanceMother.idleWith("some-agent"))
          when(jobInstanceService.totalCompletedJobsCountOn(any())).thenReturn(42)
          when(jobInstanceService.completedJobsOnAgent(any(), any(), any(), any())).thenReturn(jobInstancesModel)

          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?offset=1")
          assertThatResponse()
            .isOk()
            .hasContentType(controller.mimeType)
            .hasBodyWithJsonObject(AgentJobHistoryRepresenter, "some-agent", jobInstancesModel)
        }
      }

      @Nested
      class PageSize {
        @Test
        void 'should not allow `page_size` less than 10'() {
          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?page_size=9")

          assertThatResponse()
            .isBadRequest()
            .hasContentType(controller.mimeType)
            .hasJsonMessage(AgentJobHistoryControllerV1.BAD_PAGE_SIZE_MSG)
        }

        @Test
        void 'should disallow `page_size` more than 100'() {
          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?page_size=101")

          assertThatResponse()
            .isBadRequest()
            .hasContentType(controller.mimeType)
            .hasJsonMessage(AgentJobHistoryControllerV1.BAD_PAGE_SIZE_MSG)
        }

        @Test
        void 'should allow page_size of 10'() {
          def pagination = Pagination.pageStartingAt(0, 42, 10)
          def jobInstance = JobInstanceMother.completed("blah", JobResult.Passed)
          def jobInstancesModel = new JobInstancesModel(new JobInstances(jobInstance), pagination)

          when(agentService.findAgent("some-agent")).thenReturn(AgentInstanceMother.idleWith("some-agent"))
          when(jobInstanceService.totalCompletedJobsCountOn(any())).thenReturn(42)
          when(jobInstanceService.completedJobsOnAgent(any(), any(), any(), any())).thenReturn(jobInstancesModel)

          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?page_size=10")

          assertThatResponse()
            .isOk()
            .hasContentType(controller.mimeType)
            .hasBodyWithJsonObject(AgentJobHistoryRepresenter, "some-agent", jobInstancesModel)
        }

        @Test
        void 'should allow page_size of 100'() {
          def pagination = Pagination.pageStartingAt(0, 42, 100)
          def jobInstance = JobInstanceMother.completed("blah", JobResult.Passed)
          def jobInstancesModel = new JobInstancesModel(new JobInstances(jobInstance), pagination)

          when(agentService.findAgent("some-agent")).thenReturn(AgentInstanceMother.idleWith("some-agent"))
          when(jobInstanceService.totalCompletedJobsCountOn(any())).thenReturn(42)
          when(jobInstanceService.completedJobsOnAgent(any(), any(), any(), any())).thenReturn(jobInstancesModel)

          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?page_size=100")

          assertThatResponse()
            .isOk()
            .hasContentType(controller.mimeType)
            .hasBodyWithJsonObject(AgentJobHistoryRepresenter, "some-agent", jobInstancesModel)
        }
      }

      @Nested
      class SortColumn {
        @Test
        void 'should not allow bad `sort_column` column name'() {
          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?sort_column=blah")

          assertThatResponse()
            .isBadRequest()
            .hasContentType(controller.mimeType)
            .hasJsonMessage(AgentJobHistoryControllerV1.BAD_SORT_COLUMN_MSG)
        }

        @Test
        void 'should allow good `sort_column` name'() {
          def pagination = Pagination.pageStartingAt(0, 42, 10)
          def jobInstance = JobInstanceMother.completed("blah", JobResult.Passed)
          def jobInstancesModel = new JobInstancesModel(new JobInstances(jobInstance), pagination)

          when(agentService.findAgent("some-agent")).thenReturn(AgentInstanceMother.idleWith("some-agent"))
          when(jobInstanceService.totalCompletedJobsCountOn(any())).thenReturn(42)
          when(jobInstanceService.completedJobsOnAgent(any(), any(), any(), any())).thenReturn(jobInstancesModel)

          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?sort_column=duration")

          assertThatResponse()
            .isOk()
            .hasContentType(controller.mimeType)
            .hasBodyWithJsonObject(AgentJobHistoryRepresenter, "some-agent", jobInstancesModel)
        }
      }

      @Nested
      class SortOrder {
        @Test
        void 'should allow ASC `sort_order`'() {
          def pagination = Pagination.pageStartingAt(0, 42, 10)
          def jobInstance = JobInstanceMother.completed("blah", JobResult.Passed)
          def jobInstancesModel = new JobInstancesModel(new JobInstances(jobInstance), pagination)

          when(agentService.findAgent("some-agent")).thenReturn(AgentInstanceMother.idleWith("some-agent"))
          when(jobInstanceService.totalCompletedJobsCountOn(any())).thenReturn(42)
          when(jobInstanceService.completedJobsOnAgent(any(), any(), any(), any())).thenReturn(jobInstancesModel)

          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?sort_order=ASC")

          assertThatResponse()
            .isOk()
            .hasContentType(controller.mimeType)
            .hasBodyWithJsonObject(AgentJobHistoryRepresenter, "some-agent", jobInstancesModel)
        }

        @Test
        void 'should allow DESC `sort_order`'() {
          def pagination = Pagination.pageStartingAt(0, 42, 10)
          def jobInstance = JobInstanceMother.completed("blah", JobResult.Passed)
          def jobInstancesModel = new JobInstancesModel(new JobInstances(jobInstance), pagination)

          when(agentService.findAgent("some-agent")).thenReturn(AgentInstanceMother.idleWith("some-agent"))
          when(jobInstanceService.totalCompletedJobsCountOn(any())).thenReturn(42)
          when(jobInstanceService.completedJobsOnAgent(any(), any(), any(), any())).thenReturn(jobInstancesModel)

          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?sort_order=DESC")

          assertThatResponse()
            .isOk()
            .hasContentType(controller.mimeType)
            .hasBodyWithJsonObject(AgentJobHistoryRepresenter, "some-agent", jobInstancesModel)
        }

        @Test
        void 'should not allow bad `sort_order`'() {
          getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent") + "?sort_order=blah")

          assertThatResponse()
            .isBadRequest()
            .hasContentType(controller.mimeType)
            .hasJsonMessage(AgentJobHistoryControllerV1.BAD_SORT_ORDER_MSG)
        }
      }
    }

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(Routes.AgentJobHistory.forAgent("some-agent"))
      }
    }
  }
}
