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

package com.thoughtworks.go.apiv1.pipelineselection

import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionResponse
import com.thoughtworks.go.apiv1.pipelineselection.representers.PipelineSelectionsRepresenter
import com.thoughtworks.go.config.BasicPipelineConfigs
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigs
import com.thoughtworks.go.server.domain.user.PipelineSelections
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.server.service.PipelineSelectionsService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import javax.servlet.http.Cookie

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PipelineSelectionControllerDelegateTest implements SecurityServiceTrait, ControllerTrait<PipelineSelectionControllerDelegate> {
  PipelineSelectionsService pipelineSelectionsService = mock(PipelineSelectionsService.class)
  PipelineConfigService pipelineConfigService = mock(PipelineConfigService.class)

  @Nested
  class Show {

    @Nested
    class AsAuthorizedUser {
      @Test
      void 'returns the pipeline selection'() {
        enableSecurity()
        loginAsUser()


        def selections = new PipelineSelections(["build-linux", "build-windows"], new Date(), currentUserLoginId(), true)

        def group1 = new BasicPipelineConfigs(group: "grp1")
        def group2 = new BasicPipelineConfigs(group: "grp2")
        group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline1")))
        group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline2")))

        List<PipelineConfigs> pipelineConfigs = [group1, group2]

        when(pipelineSelectionsService.getSelectedPipelines(null, currentUserLoginId())).thenReturn(selections)
        when(pipelineConfigService.viewableGroupsFor(currentUsername())).thenReturn(pipelineConfigs)

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(new PipelineSelectionResponse(selections, pipelineConfigs), PipelineSelectionsRepresenter.class)
      }
    }

    @Nested
    class AsAnonymousUser {
      @Test
      void 'returns the pipeline selection from cookie'() {
        disableSecurity()
        loginAsAnonymous()

        def selections = new PipelineSelections(["build-linux", "build-windows"], new Date(), currentUserLoginId(), true)

        def group1 = new BasicPipelineConfigs(group: "grp1")
        def group2 = new BasicPipelineConfigs(group: "grp2")
        group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline1")))
        group2.add(new PipelineConfig(name: new CaseInsensitiveString("pipeline2")))

        List<PipelineConfigs> pipelineConfigs = [group1, group2]

        String cookieId = SecureRandom.hex()

        when(pipelineSelectionsService.getSelectedPipelines(cookieId, currentUserLoginId())).thenReturn(selections)
        when(pipelineConfigService.viewableGroupsFor(currentUsername())).thenReturn(pipelineConfigs)

        httpRequestBuilder.withCookies(new Cookie("selected_pipelines", cookieId))
        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(new PipelineSelectionResponse(selections, pipelineConfigs), PipelineSelectionsRepresenter.class)
      }
    }
  }


  @Nested
  class Update {

    @Nested
    class AsAuthorizedUser {
      @Test
      void 'updates the pipeline selection and returns a message'() {
        enableSecurity()
        loginAsUser()

        def payload = [
          selections: ['build-linux', 'build-windows'],
          blacklist : true
        ]

        when(pipelineSelectionsService.persistSelectedPipelines(null, currentUserLoginId(), payload.selections, payload.blacklist)).thenReturn(1l)

        putWithApiHeader(controller.controllerBasePath(), payload)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasNoBody()
      }
    }

    @Nested
    class AsAnonymousUser {
      @Test
      void 'updates the pipeline selection and returns a message'() {
        disableSecurity()
        loginAsAnonymous()

        def payload = [
          selections: ['build-linux', 'build-windows'],
          blacklist : true
        ]

        long recordId = SecureRandom.longNumber()
        when(pipelineSelectionsService.persistSelectedPipelines(String.valueOf(recordId), currentUserLoginId(), payload.selections, payload.blacklist)).thenReturn(recordId)

        httpRequestBuilder.withCookies(new Cookie("selected_pipelines", String.valueOf(recordId)))
        putWithApiHeader(controller.controllerBasePath(), payload)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasCookie("/go", "selected_pipelines", String.valueOf(recordId), 31536000, true, true)
          .hasNoBody()
      }
    }
  }

  @Override
  PipelineSelectionControllerDelegate createControllerInstance() {
    return new PipelineSelectionControllerDelegate(new ApiAuthenticationHelper(securityService, goConfigService), pipelineSelectionsService, pipelineConfigService)
  }
}
