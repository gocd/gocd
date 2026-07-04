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
package com.thoughtworks.go.apiv2.compare

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper
import com.thoughtworks.go.apiv2.compare.representers.PipelineInstanceModelsRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.helper.StageInstanceModelMother
import com.thoughtworks.go.helper.StageMother
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.PipelineHistoryService
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

import java.time.Instant
import java.util.stream.Stream

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class InternalCompareControllerV2Test implements SecurityServiceTrait, ControllerTrait<InternalCompareControllerV2> {
  @Mock
  private PipelineHistoryService pipelineHistoryService

  @Override
  InternalCompareControllerV2 createControllerInstance() {
    return new InternalCompareControllerV2(new ApiAuthorizationHelper(securityService, goConfigService), pipelineHistoryService)
  }

  @Nested
  class List {
    @BeforeEach
    void setUp() {
      when(goConfigService.hasPipelineNamed(any(CaseInsensitiveString.class))).thenReturn(true)
    }

    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {
      @Delegate SecurityServiceTrait s = InternalCompareControllerV2Test.this
      @Delegate ControllerTrait<InternalCompareControllerV2> c = InternalCompareControllerV2Test.this

      @Override
      String getControllerMethodUnderTest() {
        return 'list'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(pipelineSpecifier.pipelineName(), getControllerMethodUnderTest()))
      }

      @Override
      PipelineSpecifier getPipelineSpecifier() {
        new PipelineSpecifier(pipelineName: 'up42')
      }
    }

    @Nested
    class AsNormalUser {
      private pipelineName = "up42"

      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }

      @Test
      void 'should return pipeline instance models'() {
        def date = Instant.now()
        def stage = StageMother.passedStageInstance(pipelineName, "stageName", 2, "buildName", date)
        def stageInstanceModel = StageInstanceModelMother.fromStage(stage)
        def stageInstanceModels = new StageInstanceModels()
        stageInstanceModels.add(stageInstanceModel)

        def materialRevisions = ModificationsMother.multipleModifications()
        def buildCause = BuildCause.createWithModifications(materialRevisions, "approver")

        def pipelineInstanceModel1 = PipelineInstanceModel.createPipeline(pipelineName, 2, "label", buildCause, stageInstanceModels)
        def pipelineInstanceModel2 = PipelineInstanceModel.createPipeline(pipelineName, 3, "label", buildCause, stageInstanceModels)
        def pipelineInstanceModels = PipelineInstanceModels.createPipelineInstanceModels(pipelineInstanceModel1, pipelineInstanceModel2)

        when(pipelineHistoryService.findMatchingPipelineInstances(anyString(), anyString(), anyInt(), any(Username.class), any())).thenReturn(pipelineInstanceModels)

        def expected = toObjectString({ PipelineInstanceModelsRepresenter.toJSON(it, pipelineInstanceModels) })

        getWithApiHeader(controller.controllerPath(pipelineName, 'list'))

        verify(pipelineHistoryService).findMatchingPipelineInstances(eq(pipelineName), eq(""), eq(10), any(Username.class), any())

        assertThatResponse()
          .isOk()
          .hasJsonBody(expected)
      }

      @ParameterizedTest
      @MethodSource("pageSizes")
      void 'should throw error if page_size is not between 10 and 100'(String input) {
        getWithApiHeader(controller.controllerPath(pipelineName, 'list') + "?page_size=" + input)

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
}
