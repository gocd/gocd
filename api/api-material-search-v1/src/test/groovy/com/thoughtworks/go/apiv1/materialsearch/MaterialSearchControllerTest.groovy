/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.apiv1.materialsearch

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper
import com.thoughtworks.go.apiv1.materialsearch.representers.MatchedRevisionRepresenter
import com.thoughtworks.go.domain.materials.MatchedRevision
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.MaterialService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.serverhealth.HealthStateScope
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupOperateUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialSearchControllerTest implements ControllerTrait<MaterialSearchController>, SecurityServiceTrait {

  @Mock
  MaterialService materialService


  @Override
  MaterialSearchController createControllerInstance() {
    return new MaterialSearchController(materialService, new ApiAuthorizationHelper(securityService, goConfigService))
  }

  @Nested
  class Security implements SecurityTestTrait, GroupOperateUserSecurity {
    @Delegate ControllerTrait<MaterialSearchController> c = MaterialSearchControllerTest.this
    @Delegate SecurityServiceTrait s = MaterialSearchControllerTest.this

    @Override
    String getControllerMethodUnderTest() {
      return "search"
    }

    @Override
    void makeHttpCall() {
      getWithApiHeader(controller.controllerPath([fingerprint: 'foo', pipeline_name: pipelineSpecifier.pipelineName(), search_text: 'abc']))
    }

    @Override
    PipelineSpecifier getPipelineSpecifier() {
      new PipelineSpecifier(pipelineName: 'some-pipeline')
    }
  }

  @Nested
  class Search {

    @Test
    void 'should show search results'() {
      def matchedRevisions = [new MatchedRevision("abc", "9ea1cf", "9ea1cf0ae04be6088242a5b6275ed36eadfcf205", "username", commitDate, "commit message"),
                              new MatchedRevision("abc", "pipeline/1/stage/1", pipelineDate, "label")]
      when(materialService.searchRevisions(eq("some-pipeline") as String, eq("foo") as String, eq("abc") as String,
        ArgumentMatchers.any() as Username, ArgumentMatchers.any()))
        .thenReturn(matchedRevisions)

      getWithApiHeader(controller.controllerPath([fingerprint: 'foo', pipeline_name: 'some-pipeline', search_text: 'abc']))

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasBodyWithJsonArray(MatchedRevisionRepresenter.class, matchedRevisions)
    }
  }

  @Test
  void 'should render 404 when pipeline or fingerprint is not found'() {
    when(materialService.searchRevisions(any(), any(), any(), any(), any())).then({ InvocationOnMock invocation ->
      HttpLocalizedOperationResult result = invocation.getArgument(4)
      result.notFound("some message",
        HealthStateType.general(HealthStateScope.forPipeline("some-pipeline")))
    })

    getWithApiHeader(controller.controllerPath([fingerprint: 'foo', pipeline_name: 'some-pipeline', search_text: 'abc']))

    assertThatResponse()
      .isNotFound()
      .hasContentType(controller.mimeType)
      .hasJsonMessage("some message")
  }

  @Test
  void 'should render empty list when no search results are found'() {
    when(materialService.searchRevisions(eq("some-pipeline") as String, eq("foo") as String, eq("abc") as String,
      ArgumentMatchers.any() as Username, ArgumentMatchers.any()))
      .thenReturn([])

    getWithApiHeader(controller.controllerPath([fingerprint: 'foo', pipeline_name: 'some-pipeline', search_text: 'abc']))

    assertThatResponse()
      .isOk()
      .hasContentType(controller.mimeType)
      .hasJsonBody([])
  }
}
