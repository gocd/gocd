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
package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.spring.SpaAuthorizationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import spark.ModelAndView
import spark.Request
import spark.Response

import static com.thoughtworks.go.config.CaseInsensitiveString.cis
import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PipelineActivityControllerTest implements ControllerTrait<PipelineActivityController>, SecurityServiceTrait {

  @Override
  PipelineActivityController createControllerInstance() {
    return new PipelineActivityController(new SpaAuthorizationHelper(securityService, goConfigService), templateEngine, goConfigService, securityService)
  }

  @Test
  void "should add pipeline name in meta"() {
    def pipelineName = "up42"
    def request = mock(Request)
    when(request.params("pipeline_name")).thenReturn(pipelineName)
    ModelAndView modelAndView = controller.index(request, mock(Response))

    Map<Object, Object> model = modelAndView.getModel() as Map<Object, Object>

    assertThat(model.get("meta") as Map<String, Object>)
      .containsEntry("pipelineName", pipelineName)
  }

  @Test
  void "should add isEditableFromUI in meta"() {
    def pipelineName = "up42"
    def request = mock(Request)
    when(request.params("pipeline_name")).thenReturn(pipelineName)
    when(goConfigService.isPipelineEditable(pipelineName)).thenReturn(true)
    ModelAndView modelAndView = controller.index(request, mock(Response))

    Map<Object, Object> model = modelAndView.getModel() as Map<Object, Object>

    assertThat(model.get("meta") as Map<String, Object>)
      .containsEntry("isEditableFromUI", true)
  }

  @Test
  void "should add canOperatePipeline in meta"() {
    def pipelineName = "up42"
    def request = mock(Request)
    when(request.params("pipeline_name")).thenReturn(pipelineName)
    when(securityService.hasOperatePermissionForPipeline(currentUserLoginName(), pipelineName)).thenReturn(true)
    ModelAndView modelAndView = controller.index(request, mock(Response))

    Map<Object, Object> model = modelAndView.getModel() as Map<Object, Object>

    assertThat(model.get("meta") as Map<String, Object>)
      .containsEntry("canOperatePipeline", true)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, PipelineAccessSecurity {
      @Delegate ControllerTrait<PipelineActivityController> c = PipelineActivityControllerTest.this
      @Delegate SecurityServiceTrait s = PipelineActivityControllerTest.this

      @BeforeEach
      void setUp() {
        when(goConfigService.hasPipelineNamed(cis(getPipelineName()))).thenReturn(true)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerBasePath().replace(":pipeline_name", getPipelineName()))
      }

      @Override
      PipelineSpecifier getPipelineSpecifier() {
        new PipelineSpecifier(pipelineName: "up42")
      }
    }
  }
}
