/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.triggerwithoptionsview

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.triggerwithoptionsview.representers.TriggerOptions
import com.thoughtworks.go.apiv1.triggerwithoptionsview.representers.TriggerWithOptionsViewRepresenter
import com.thoughtworks.go.config.EnvironmentVariablesConfig
import com.thoughtworks.go.config.PipelineNotFoundException
import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.domain.MaterialRevisions
import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.EnvironmentVariablesConfigMother
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.helper.MaterialsMother
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import com.thoughtworks.go.server.service.PipelineHistoryService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.util.HaltApiMessages.notFoundMessage
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class TriggerWithOptionsViewDelegateTest implements ControllerTrait<TriggerWithOptionsViewDelegate>, SecurityServiceTrait {
  PipelineHistoryService pipelineHistoryService = mock(PipelineHistoryService.class)

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait {

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void "should disallow anonymous users, with security enabled"() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Test
      void 'should allow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()

        makeHttpCall()
        assertRequestAuthorized()
      }


      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("build-linux"))
      }
    }

    @Nested
    class AsAuthorizedUser {
      @Test
      void 'should render trigger options'() {
        MaterialRevisions revisions = ModificationsMother.modifyOneFile(MaterialsMother.defaultSvnMaterialsWithUrl("http://example.com/svn/project"), "revision")

        StageInstanceModels stages = new StageInstanceModels()
        stages.addStage("unit1", JobHistory.withJob("test", JobState.Completed, JobResult.Passed, new Date()))
        stages.addFutureStage("unit2", false)

        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, "bob"), stages)
        model.setMaterialConfigs(MaterialConfigsMother.defaultSvnMaterialConfigsWithUrl("http://example.com/svn/project"))

        EnvironmentVariablesConfig variables = EnvironmentVariablesConfigMother.environmentVariables()

        when(goConfigService.variablesFor("build-linux")).thenReturn(variables)
        when(pipelineHistoryService.latest("build-linux", currentUsername())).thenReturn(model)

        getWithApiHeader(controller.controllerPath("build-linux"))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(new TriggerOptions(variables, model), TriggerWithOptionsViewRepresenter.class)
      }

      @Test
      void 'should render 404 if bad pipeline is provided'() {
        when(goConfigService.variablesFor("pipeline-that-is-not-present")).thenThrow(new PipelineNotFoundException())

        getWithApiHeader(controller.controllerPath("pipeline-that-is-not-present"))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(notFoundMessage())
      }
    }
  }

  @Override
  TriggerWithOptionsViewDelegate createControllerInstance() {
    return new TriggerWithOptionsViewDelegate(new ApiAuthenticationHelper(securityService, goConfigService), goConfigService, pipelineHistoryService)
  }
}
