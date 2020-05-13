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

package com.thoughtworks.go.apiv1.internaldependencypipelines

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.JobConfigs
import com.thoughtworks.go.config.PipelineTemplateConfig
import com.thoughtworks.go.config.StageConfig
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.service.GoConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.SystemEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalDependencyPipelinesControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalDependencyPipelinesControllerV1> {
  @Mock
  GoConfigService goConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalDependencyPipelinesControllerV1 createControllerInstance() {
    new InternalDependencyPipelinesControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), new SystemEnvironment(), goConfigService)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(path('pipeline', 'stage'))
      }
    }
  }

  @Test
  void 'should return pipeline auto suggestions'() {
    def config = new BasicCruiseConfig()
    config.addPipeline("first", PipelineConfigMother.pipelineConfig("pipeline1"))
    when(goConfigService.getMergedConfigForEditing()).thenReturn(config)

    getWithApiHeader(path('pipeline1', 'stage'))

    assertThatResponse()
      .isOk()
      .hasJsonBody('{' +
      '  "pipeline1": {' +
      '    "mingle": {}' +
      '  },' +
      '  "": {' +
      '    "mingle": {}' +
      '  }' +
      '}')
  }

  @Test
  void 'should return template auto suggestions'() {
    def config = new BasicCruiseConfig()
    config.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("template1"), new StageConfig(new CaseInsensitiveString("stage1"), new JobConfigs())))
    config.addPipeline("first", PipelineConfigMother.pipelineConfig("pipeline1"))
    when(goConfigService.getMergedConfigForEditing()).thenReturn(config)

    getWithApiHeader(path('template1', 'stage1') + '?template=true')

    assertThatResponse()
      .isOk()
      .hasJsonBody('{' +
      '  "pipeline1": {' +
      '    "mingle": {}' +
      '  }' +
      '}')
  }

  String path(String name, String stageName) {
    return controller.controllerBasePath().replaceAll(":pipeline_name", name).replaceAll(':stage_name', stageName)
  }
}
