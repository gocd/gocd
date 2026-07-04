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

package com.thoughtworks.go.apiv1.internaldependencypipelines

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.TemplateViewUserSecurity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.config.CaseInsensitiveString.cis
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class InternalDependencyPipelinesControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalDependencyPipelinesControllerV1> {

  @Override
  InternalDependencyPipelinesControllerV1 createControllerInstance() {
    new InternalDependencyPipelinesControllerV1(new ApiAuthorizationHelper(securityService, goConfigService), goConfigService)
  }

  @Nested
  class Index {
    @Nested
    class PipelineSecurity implements SecurityTestTrait, PipelineAccessSecurity {
      @Delegate SecurityServiceTrait s = InternalDependencyPipelinesControllerV1Test.this
      @Delegate ControllerTrait<InternalDependencyPipelinesControllerV1> c = InternalDependencyPipelinesControllerV1Test.this

      @BeforeEach
      void setUp() {
        when(goConfigService.hasPipelineNamed(any(CaseInsensitiveString.class))).thenReturn(true)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(path('pipeline1', 'stage1'))
      }

      @Override
      PipelineSpecifier getPipelineSpecifier() {
        new PipelineSpecifier(pipelineName: 'pipeline1')
      }
    }

    @Nested
    class TemplateSecurity implements SecurityTestTrait, TemplateViewUserSecurity {
      @Delegate SecurityServiceTrait s = InternalDependencyPipelinesControllerV1Test.this
      @Delegate ControllerTrait<InternalDependencyPipelinesControllerV1> c = InternalDependencyPipelinesControllerV1Test.this

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(path('template1', 'stage1') + '?template=true')
      }
    }
  }

  @Nested
  class AsNormalUser {
    @BeforeEach
    void setUp() {
      loginAsPipelineViewUser(groupName: "first", pipelineName: "pipeline1")
      when(goConfigService.hasPipelineNamed(any(CaseInsensitiveString.class))).thenReturn(true)
    }

    @Test
    void 'should return pipeline auto suggestions'() {
      def config = new BasicCruiseConfig()
      config.addPipeline("first", PipelineConfigMother.pipelineConfig("pipeline1"))
      when(goConfigService.getMergedConfigForEditing()).thenReturn(config)

      getWithApiHeader(path('pipeline1', 'stage'))

      assertThatResponse()
        .isOk()
        .hasJsonBody([
          "pipeline1": ["mingle": [:]],
          ""         : ["mingle": [:]]
        ])
    }

    @Test
    void 'should filter out upstream pipelines the user cannot view'() {
      def upstream = PipelineConfigMother.pipelineConfig("upstream")
      def downstream = PipelineConfigMother.pipelineConfig("downstream")
      downstream.addMaterialConfig(new DependencyMaterialConfig(cis("upstream"), cis("mingle")))

      def config = new BasicCruiseConfig()
      config.addPipeline("first", upstream)
      config.addPipeline("first", downstream)
      when(goConfigService.getMergedConfigForEditing()).thenReturn(config)
      when(securityService.hasViewPermissionForPipeline(any(Username.class), anyString())).thenReturn(false)
      when(securityService.hasViewPermissionForPipeline(any(Username.class), eq("downstream"))).thenReturn(true)

      getWithApiHeader(path('downstream', 'stage'))

      assertThatResponse()
        .isOk()
        .hasJsonBody([
          "downstream": ["mingle": [:]],
          ""          : ["mingle": [:]]
        ])
    }
  }

  @Nested
  class AsTemplateAdmin {
    @BeforeEach
    void setUp() {
      loginAsTemplateAdmin()
      when(securityService.hasViewPermissionForPipeline(any(Username.class), anyString())).thenReturn(true)
    }

    @Test
    void 'should return template auto suggestions'() {
      def config = new BasicCruiseConfig()
      config.addTemplate(new PipelineTemplateConfig(cis("template1"), new StageConfig(cis("stage1"), new JobConfigs())))
      config.addPipeline("first", PipelineConfigMother.pipelineConfig("pipeline1"))
      when(goConfigService.getMergedConfigForEditing()).thenReturn(config)

      getWithApiHeader(path('template1', 'stage1') + '?template=true')

      assertThatResponse()
        .isOk()
        .hasJsonBody([
          "pipeline1": ["mingle": [:]]
        ])
    }

    @Test
    void 'should filter out non-viewable pipelines from template auto suggestions'() {
      def config = new BasicCruiseConfig()
      config.addTemplate(new PipelineTemplateConfig(cis("template1"), new StageConfig(cis("stage1"), new JobConfigs())))
      config.addPipeline("visible", PipelineConfigMother.pipelineConfig("visible"))
      config.addPipeline("hidden", PipelineConfigMother.pipelineConfig("hidden"))
      when(goConfigService.getMergedConfigForEditing()).thenReturn(config)
      when(securityService.hasViewPermissionForPipeline(any(Username.class), anyString())).thenReturn(false)
      when(securityService.hasViewPermissionForPipeline(any(Username.class), eq("visible"))).thenReturn(true)

      getWithApiHeader(path('template1', 'stage1') + '?template=true')

      assertThatResponse()
        .isOk()
        .hasJsonBody([
          "visible": ["mingle": [:]]
        ])
    }
  }

  String path(String name, String stageName) {
    return controller.controllerBasePath().replace(":pipeline_name", name).replace(':stage_name', stageName)
  }
}
