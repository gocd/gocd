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

package com.thoughtworks.go.apiv1.pipelineinstance

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.pipelineinstance.representers.PipelineInstanceModelRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.helper.StageMother
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.PipelineHistoryService
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class PipelineInstanceControllerV1Test implements SecurityServiceTrait, ControllerTrait<PipelineInstanceControllerV1> {
  @Mock
  PipelineHistoryService pipelineHistoryService

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
        getWithApiHeader(controller.controllerPath("${pipelineName}/instance/4"))
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

        getWithApiHeader(controller.controllerPath(pipelineName, "instance", "4"))

        def expectedJson = toObjectString({ PipelineInstanceModelRepresenter.toJSON(it, pipelineInstanceModel) })

        assertThatResponse()
          .isOk()
          .hasJsonBody(expectedJson)
      }

      @Test
      void 'should throw 404 if the pipeline does not exist'() {
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(false)

        getWithApiHeader(controller.controllerPath(pipelineName, "instance", "4"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Pipeline with name '${pipelineName}' was not found!")
      }

      @Test
      void 'should throw 404 if the counter specified is less than 1'() {
        getWithApiHeader(controller.controllerPath(pipelineName, "instance", "0"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. The pipeline counter cannot be less than 1.")
      }

      @Test
      void 'should throw 404 if the counter specified is not a valid integer'() {
        getWithApiHeader(controller.controllerPath(pipelineName, "instance", "abc"))

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
}
