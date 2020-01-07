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
package com.thoughtworks.go.apiv1.pipelineinstance

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.pipelineinstance.representers.PipelineInstanceModelRepresenter
import com.thoughtworks.go.apiv1.pipelineinstance.representers.PipelineInstanceModelsRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.PipelineRunIdInfo
import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.helper.StageMother
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.PipelineHistoryService
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.PipelineGroupOperateUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock

import java.util.stream.Stream

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class PipelineInstanceControllerV1Test implements SecurityServiceTrait, ControllerTrait<PipelineInstanceControllerV1> {
  @Mock
  private PipelineHistoryService pipelineHistoryService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PipelineInstanceControllerV1 createControllerInstance() {
    new PipelineInstanceControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), pipelineHistoryService)
  }

  @Nested
  class Instance {
    private String pipelineName = "up42"

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
        getWithApiHeader(controller.controllerPath("${pipelineName}/4/instance"))
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
        enableSecurity()
        loginAsPipelineViewUser(pipelineName)
      }

      @Test
      void 'should return the pipeline instance corresponding to the counter'() {
        PipelineInstanceModel pipelineInstanceModel = getPipelineInstanceModel()

        when(pipelineHistoryService.findPipelineInstance(eq(pipelineName), eq(4), any(Username.class), any(HttpOperationResult.class))).thenReturn(pipelineInstanceModel)

        getWithApiHeader(controller.controllerPath(pipelineName, "4", "instance"))

        def expectedJson = toObjectString({ PipelineInstanceModelRepresenter.toJSON(it, pipelineInstanceModel) })

        assertThatResponse()
          .isOk()
          .hasJsonBody(expectedJson)
      }

      @Test
      void 'should throw 404 if the pipeline does not exist'() {
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(false)

        getWithApiHeader(controller.controllerPath(pipelineName, "4", "instance"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Pipeline with name '${pipelineName}' was not found!")
      }

      @Test
      void 'should throw 404 if the counter specified is less than 1'() {
        getWithApiHeader(controller.controllerPath(pipelineName, "0", "instance"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. The pipeline counter cannot be less than 1.")
      }

      @Test
      void 'should throw 404 if the counter specified is not a valid integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, "abc", "instance"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. The pipeline counter should be an integer.")
      }


      private PipelineInstanceModel getPipelineInstanceModel() {
        def stage = StageMother.passedStageInstance(pipelineName, "stageName", 2, "buildName", new Date())
        def stageInstanceModel = StageMother.toStageInstanceModel(stage)
        def stageInstanceModels = new StageInstanceModels()
        stageInstanceModels.add(stageInstanceModel)

        def materialRevisions = ModificationsMother.multipleModifications()
        def buildCause = BuildCause.createWithModifications(materialRevisions, "approver")

        return PipelineInstanceModel.createPipeline(pipelineName, 2, "label", buildCause, stageInstanceModels)
      }
    }
  }

  @Nested
  class History {
    private String pipelineName = "up42"

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
        getWithApiHeader(controller.controllerPath(pipelineName, 'history'), [:])
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
        enableSecurity()
        loginAsPipelineViewUser(pipelineName)
      }

      @Test
      void 'should render latest pipeline history'() {
        def date = new Date()
        def stage = StageMother.passedStageInstance(pipelineName, "stageName", 2, "buildName", date)
        def stageInstanceModel = StageMother.toStageInstanceModel(stage)
        def stageInstanceModels = new StageInstanceModels()
        stageInstanceModels.add(stageInstanceModel)

        def materialRevisions = ModificationsMother.multipleModifications()
        def buildCause = BuildCause.createWithModifications(materialRevisions, "approver")

        def pipelineInstanceModel1 = PipelineInstanceModel.createPipeline(pipelineName, 2, "label", buildCause, stageInstanceModels)
        def pipelineInstanceModel2 = PipelineInstanceModel.createPipeline(pipelineName, 3, "label", buildCause, stageInstanceModels)
        def pipelineInstanceModels = PipelineInstanceModels.createPipelineInstanceModels(pipelineInstanceModel1, pipelineInstanceModel2)
        def ids = new PipelineRunIdInfo(pipelineInstanceModel2.id, pipelineInstanceModel1.id)

        when(pipelineHistoryService.totalCount(pipelineName)).thenReturn(1)
        when(pipelineHistoryService.loadPipelineHistoryData(any(Username.class), eq(pipelineName), anyLong(), anyLong(), anyInt())).thenReturn(pipelineInstanceModels)
        when(pipelineHistoryService.getOldestAndLatestPipelineId(eq(pipelineName), any(Username.class))).thenReturn(ids)

        getWithApiHeader(controller.controllerPath(pipelineName, 'history'))

        verify(pipelineHistoryService).loadPipelineHistoryData(any(Username.class), eq(pipelineName), eq(0L), eq(0L), eq(10))

        def expectedJson = toObjectString({ PipelineInstanceModelsRepresenter.toJSON(it, pipelineInstanceModels, ids) })

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
      }

      @Test
      void 'should render pipeline history after the specified cursor'() {
        def date = new Date()
        def stage = StageMother.passedStageInstance(pipelineName, "stageName", 2, "buildName", date)
        def stageInstanceModel = StageMother.toStageInstanceModel(stage)
        def stageInstanceModels = new StageInstanceModels()
        stageInstanceModels.add(stageInstanceModel)

        def materialRevisions = ModificationsMother.multipleModifications()
        def buildCause = BuildCause.createWithModifications(materialRevisions, "approver")

        def pipelineInstanceModels = PipelineInstanceModels.createPipelineInstanceModels()
        for (int i = 1; i <= 2; i++) {
          def pipelineInstanceModel = PipelineInstanceModel.createPipeline(pipelineName, i, "label", buildCause, stageInstanceModels)
          pipelineInstanceModel.id = i
          pipelineInstanceModels.add(pipelineInstanceModel)
        }
        def ids = new PipelineRunIdInfo(5L, 1L)

        when(pipelineHistoryService.totalCount(pipelineName)).thenReturn(1)
        when(pipelineHistoryService.loadPipelineHistoryData(any(Username.class), eq(pipelineName), anyLong(), anyLong(), anyInt())).thenReturn(pipelineInstanceModels)
        when(pipelineHistoryService.getOldestAndLatestPipelineId(eq(pipelineName), any(Username.class))).thenReturn(ids)

        getWithApiHeader(controller.controllerPath(pipelineName, 'history') + "?after=3")

        verify(pipelineHistoryService).loadPipelineHistoryData(any(Username.class), eq(pipelineName), eq(3L), eq(0L), eq(10))

        def expectedJson = toObjectString({ PipelineInstanceModelsRepresenter.toJSON(it, pipelineInstanceModels, ids) })

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
      }

      @Test
      void 'should render pipeline history before the specified cursor'() {
        def date = new Date()
        def stage = StageMother.passedStageInstance(pipelineName, "stageName", 2, "buildName", date)
        def stageInstanceModel = StageMother.toStageInstanceModel(stage)
        def stageInstanceModels = new StageInstanceModels()
        stageInstanceModels.add(stageInstanceModel)

        def materialRevisions = ModificationsMother.multipleModifications()
        def buildCause = BuildCause.createWithModifications(materialRevisions, "approver")

        def pipelineInstanceModels = PipelineInstanceModels.createPipelineInstanceModels()
        for (int i = 4; i <= 5; i++) {
          def pipelineInstanceModel = PipelineInstanceModel.createPipeline(pipelineName, i, "label", buildCause, stageInstanceModels)
          pipelineInstanceModel.id = i
          pipelineInstanceModels.add(pipelineInstanceModel)
        }
        def ids = new PipelineRunIdInfo(5L, 1L)

        when(pipelineHistoryService.totalCount(pipelineName)).thenReturn(1)
        when(pipelineHistoryService.loadPipelineHistoryData(any(Username.class), eq(pipelineName), anyLong(), anyLong(), anyInt())).thenReturn(pipelineInstanceModels)
        when(pipelineHistoryService.getOldestAndLatestPipelineId(eq(pipelineName), any(Username.class))).thenReturn(ids)

        getWithApiHeader(controller.controllerPath(pipelineName, 'history') + "?before=3")

        verify(pipelineHistoryService).loadPipelineHistoryData(any(Username.class), eq(pipelineName), eq(0L), eq(3L), eq(10))

        def expectedJson = toObjectString({ PipelineInstanceModelsRepresenter.toJSON(it, pipelineInstanceModels, ids) })

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
      }

      @Test
      void 'should throw if the after cursor is specified as a invalid integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, 'history') + "?after=abc")

        verifyZeroInteractions(pipelineHistoryService)

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter 'after', if specified, must be a positive integer.")
      }

      @Test
      void 'should throw if the before cursor is specified as a invalid integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, 'history') + "?before=abc")

        verifyZeroInteractions(pipelineHistoryService)

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The query parameter 'before', if specified, must be a positive integer.")
      }

      @ParameterizedTest
      @MethodSource("pageSizes")
      void 'should throw error if page_size is not between 10 and 100'(String input) {
        getWithApiHeader(controller.controllerPath(pipelineName, 'history') + "?page_size=" + input)

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
  class Comment {
    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "comment"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(pipelineName, 1, "comment"), [:])
      }

      @Override
      String getPipelineName() {
        return "up42"
      }
    }
  }

  @Nested
  class AsAuthorizedUser {
    public static final String pipelineName = "up42"

    @BeforeEach
    void setUp() {
      enableSecurity()
      loginAsGroupOperateUser(pipelineName)
    }

    @Test
    void "should return 200 when comment is updated"() {
      postWithApiHeader(controller.controllerPath(pipelineName, 1, "comment"), [comment: "some comment"])

      assertThatResponse()
        .isOk()
        .hasJsonMessage("Comment successfully updated.")

      verify(pipelineHistoryService)
        .updateComment(eq(pipelineName), eq(1), eq("some comment"), eq(currentUsername()))
    }

    @Test
    void "should return 422 when 'comment' property is missing in payload"() {
      postWithApiHeader(controller.controllerPath(pipelineName, 1, "comment"), ["bar": "foo"])

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Json `{\\\"bar\\\":\\\"foo\\\"}` does not contain property 'comment'")

      verifyZeroInteractions(pipelineHistoryService)
    }

    @Test
    void "should return 422 when pipeline counter is not an integer"() {
      postWithApiHeader(controller.controllerPath(pipelineName, "one", "comment"), [comment: "foo"])

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Your request could not be processed. The pipeline counter should be an integer.")

      verifyZeroInteractions(pipelineHistoryService)
    }
  }
}
