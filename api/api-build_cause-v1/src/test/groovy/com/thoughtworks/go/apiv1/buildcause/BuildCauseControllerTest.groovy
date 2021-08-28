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
package com.thoughtworks.go.apiv1.buildcause

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.buildcause.representers.BuildCauseRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.helpers.PipelineModelMother
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.PipelineHistoryService
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.server.service.result.OperationResult
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
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class BuildCauseControllerTest implements ControllerTrait<BuildCauseController>, SecurityServiceTrait {

  @Mock
  private PipelineHistoryService pipelineHistoryService

  @Override
  BuildCauseController createControllerInstance() {
    new BuildCauseController(pipelineHistoryService, new ApiAuthenticationHelper(securityService, goConfigService))
  }

  @Nested
  class BuildCause {

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {

      @BeforeEach
      void setUp() {
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(getPipelineName()))).thenReturn(true)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("/${getPipelineName()}/2"))
      }

      @Override
      String getPipelineName() {
        return "foo"
      }
    }

    @Test
    void 'should render build cause'() {
      def pipelineInstanceModel = PipelineModelMother.pipeline_instance_model([name  : "p1", label: "g1", counter: 5,
                                                                               stages: [[name: "cruise", counter: "10", approved_by: "Anonymous"]]])
      when(pipelineHistoryService.findPipelineInstance(eq("foo") as String, eq(2) as Integer, eq(currentUsername()) as Username,
        any(OperationResult.class) as OperationResult)).thenReturn(pipelineInstanceModel)

      getWithApiHeader(controller.controllerPath('/foo/2'))

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(pipelineInstanceModel.buildCause, BuildCauseRepresenter)
    }

    @Test
    void 'should render http 404 if pipeline instance is not found'() {
      doAnswer({ InvocationOnMock invocation ->
        HttpOperationResult result = invocation.arguments.last()
        result.notFound("Pipeline foo/2 not found", "description", HealthStateType.general(HealthStateScope.GLOBAL))
      }).when(pipelineHistoryService).findPipelineInstance(eq("foo") as String, eq(2) as Integer, eq(currentUsername()) as Username,
        any(OperationResult.class) as OperationResult)

      getWithApiHeader(controller.controllerPath("/foo/2"))

      assertThatResponse()
        .isNotFound()
        .hasJsonMessage("Pipeline foo/2 not found { description }")
    }

    @Nested
    class BadRequests {
      @Test
      void 'should render http 404 if pipeline_count is not an int'() {
        getWithApiHeader(controller.controllerPath('/foo/a'))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("Parameter `pipeline_counter` must be an integer.")
      }

    }
  }
}
