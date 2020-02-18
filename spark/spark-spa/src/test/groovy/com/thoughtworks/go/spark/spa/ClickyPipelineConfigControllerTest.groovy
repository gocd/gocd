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

package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.server.service.AuthorizationExtensionCacheService
import com.thoughtworks.go.server.service.SecurityAuthConfigService
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService
import com.thoughtworks.go.server.service.support.toggle.Toggles
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import spark.HaltException
import spark.ModelAndView
import spark.Request
import spark.Response

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatCode
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ClickyPipelineConfigControllerTest implements ControllerTrait<ClickyPipelineConfigController>, SecurityServiceTrait {
  @Mock
  private AuthorizationExtensionCacheService authorizationExtensionCacheService
  @Mock
  private SecurityAuthConfigService securityAuthConfigService
  @Mock
  private Response response
  @Mock
  private FeatureToggleService featureToggleService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ClickyPipelineConfigController createControllerInstance() {
    return new ClickyPipelineConfigController(new SPAAuthenticationHelper(securityService, goConfigService), featureToggleService, templateEngine)
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
    when(featureToggleService.isToggleOn(Toggles.NEW_PIPELINE_CONFIG_SPA)).thenReturn(true)
    def pipelineName = "up42"
    def request = mock(Request)
    when(request.params("pipeline_name")).thenReturn(pipelineName)

    ModelAndView modalAndView = controller.index(request, response)
    Map<Object, Object> model = modalAndView.getModel() as Map<Object, Object>

    assertThat(model.get("meta") as Map<String, Object>)
      .containsEntry("pipelineName", pipelineName)
  }

  @Test
  void 'should return 404 when toggle is off'() {
    when(featureToggleService.isToggleOn(Toggles.NEW_PIPELINE_CONFIG_SPA)).thenReturn(false)
    def request = mock(Request)

    assertThatCode({ controller.index(request, null) })
      .isInstanceOf(HaltException)
      .satisfies({ HaltException ex -> assertThat(ex.statusCode()).isEqualTo(404) })
  }
}
