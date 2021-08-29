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

package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.server.service.AuthorizationExtensionCacheService
import com.thoughtworks.go.server.service.SecurityAuthConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import spark.ModelAndView
import spark.Request
import spark.Response

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class ClickyPipelineConfigControllerTest implements ControllerTrait<ClickyPipelineConfigController>, SecurityServiceTrait {
  @Mock
  private AuthorizationExtensionCacheService authorizationExtensionCacheService
  @Mock
  private SecurityAuthConfigService securityAuthConfigService
  @Mock
  private Response response


  @Override
  ClickyPipelineConfigController createControllerInstance() {
    return new ClickyPipelineConfigController(new SPAAuthenticationHelper(securityService, goConfigService), goConfigService, templateEngine)
  }

  @Nested
  class Security implements SecurityTestTrait, GroupAdminUserSecurity {
    @Override
    String getControllerMethodUnderTest() {
      return "index"
    }

    @Override
    void makeHttpCall() {
      get(controller.controllerPath("foo", "edit"))
    }
  }

  @Test
  void "should add pipeline name in page meta"() {
    def pipelineName = "up42"
    def request = mock(Request)
    when(request.params("pipeline_name")).thenReturn(pipelineName)

    ModelAndView modalAndView = controller.index(request, response)
    Map<Object, Object> model = modalAndView.getModel() as Map<Object, Object>

    assertThat(model.get("meta") as Map<String, Object>)
      .containsEntry("pipelineName", pipelineName)
  }

  @Test
  void "should add pipeline group name in page meta"() {
    def pipelineName = "up42"
    def groupName = "first"
    def request = mock(Request)
    when(request.params("pipeline_name")).thenReturn(pipelineName)
    when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName))).thenReturn(groupName)

    ModelAndView modalAndView = controller.index(request, response)
    Map<Object, Object> model = modalAndView.getModel() as Map<Object, Object>

    assertThat(model.get("meta") as Map<String, Object>).containsEntry("pipelineName", pipelineName)
    assertThat(model.get("meta") as Map<String, Object>).containsEntry("pipelineGroupName", groupName)
  }
}
