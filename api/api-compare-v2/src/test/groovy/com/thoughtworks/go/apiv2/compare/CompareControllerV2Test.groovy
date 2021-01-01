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
package com.thoughtworks.go.apiv2.compare

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.compare.representers.ComparisonRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.domain.MaterialRevision
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.ChangesetService
import com.thoughtworks.go.server.service.PipelineService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.serverhealth.HealthStateScope
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv2.compare.representers.MaterialRevisionsRepresenterTest.getRevisions
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class CompareControllerV2Test implements SecurityServiceTrait, ControllerTrait<CompareControllerV2> {

  @Mock
  private ChangesetService changesetService

  @Mock
  PipelineService pipelineService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  CompareControllerV2 createControllerInstance() {
    new CompareControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), changesetService, pipelineService)
  }

  @Nested
  class Index {

    @BeforeEach
    void setUp() {
      when(goConfigService.hasPipelineNamed(any(CaseInsensitiveString.class))).thenReturn(true)
    }

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(getApi('up42', 1, 1))
      }

      @Override
      String getPipelineName() {
        return 'up42'
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should return list of changes for the pipeline and between the counters specified'() {
        def pipelineName = "up42"
        def fromCounter = 1
        def toCounter = 4
        List<MaterialRevision> materialRevisions = getRevisions(new Date())

        when(pipelineService.isPipelineBisect(pipelineName, fromCounter, toCounter)).thenReturn(false)
        when(changesetService.revisionsBetween(eq(pipelineName), eq(fromCounter), eq(toCounter), eq(currentUsername()), any(HttpLocalizedOperationResult.class), eq(true))).thenReturn(materialRevisions)

        getWithApiHeader(getApi('up42', fromCounter, toCounter))

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({
          ComparisonRepresenter.toJSON(it, pipelineName, fromCounter, toCounter, false, materialRevisions)
        }))
      }

      @Test
      void 'should return 404 if pipeline was not found'() {
        when(changesetService.revisionsBetween(anyString(), anyInt(), anyInt(), any(), any(), anyBoolean())).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments()[4]
          result.notFound("not found message", HealthStateType.general(HealthStateScope.forPipeline("undefined")))
        })

        getWithApiHeader(getApi('undefined', 1, 1))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("not found message")
      }

      @Test
      void 'should error out if the any of the counter specified does not exist'() {
        when(pipelineService.isPipelineBisect('any-pipeline', 1, 1)).thenThrow(new RecordNotFoundException("not found message"))

        getWithApiHeader(getApi('any-pipeline', 1, 1))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("not found message")
      }

      @Test
      void 'should return forbidden if the user does not have access to view the pipeline'() {
        when(changesetService.revisionsBetween(anyString(), anyInt(), anyInt(), any(Username.class), any(HttpLocalizedOperationResult.class), anyBoolean())).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments()[4]
          result.forbidden("forbidden message", HealthStateType.general(HealthStateScope.forPipeline("undefined")))
        })

        getWithApiHeader(getApi('any-pipeline', 1, 1))

        assertThatResponse()
          .isForbidden()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("forbidden message")
      }

      @Test
      void 'should return as unprocessable entity if fromCounter or toCounter is given as a negative number'() {
        getWithApiHeader(getApi('any-pipeline', -1, 1))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Your request could not be processed. The instance counter `from_counter` cannot be less than 1.")
      }

      @Test
      void 'should return as unprocessable entity if fromCounter or toCounter is given as zero'() {
        getWithApiHeader(getApi('any-pipeline', 1, 0))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Your request could not be processed. The instance counter `to_counter` cannot be less than 1.")
      }

      @Test
      void 'should return as unprocessable entity if fromCounter or toCounter is given as an invalid integer'() {
        getWithApiHeader("/api/pipelines/any-pipeline/compare/a/1")

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Your request could not be processed. The instance counter `from_counter` should be an integer.")
      }
    }
  }

  static String getApi(String pipelineName, Integer fromCounter, Integer toCounter) {
    return "/api/pipelines/$pipelineName/compare/$fromCounter/$toCounter".toString()
  }
}
