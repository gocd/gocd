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

package com.thoughtworks.go.apiv1.export

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.ConfigRepoPlugin
import com.thoughtworks.go.config.GoConfigPluginService
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.plugin.access.configrepo.ExportedConfig.from
import static com.thoughtworks.go.spark.Routes.Export
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ExportControllerV1Test implements SecurityServiceTrait, ControllerTrait<ExportControllerV1> {

  @Mock
  GoConfigPluginService goConfigPluginService

  @Mock
  private ConfigRepoPlugin configRepoPlugin

  @Override
  ExportControllerV1 createControllerInstance() {
    new ExportControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), goConfigPluginService, goConfigService)
  }

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Nested
  class ExportPipeline {
    String pipelinePath(String name) {
      return Export.PIPELINES_PATH.replaceAll(":pipeline_name", name)
    }

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "exportPipeline"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(pipelinePath("foo")))
      }
    }

    @Nested
    class AsAdmin {

      private final String exportEtag = 'big_etag_for_export'

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      String pluginId = 'test.config.plugin'
      String groupName = 'group1'

      @Test
      void 'should be able to export pipeline config if user is admin and etag is stale'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig('pipeline1')
        pipeline.setOrigin(new FileConfigOrigin())

        when(goConfigService.pipelineConfigNamed('pipeline1')).thenReturn(pipeline)
        when(goConfigPluginService.isConfigRepoPlugin(pluginId)).thenReturn(true)
        when(goConfigPluginService.supportsPipelineExport(pluginId)).thenReturn(true)
        when(goConfigPluginService.partialConfigProviderFor(pluginId)).thenReturn(configRepoPlugin)
        when(configRepoPlugin.etagForExport(pipeline, groupName)).thenReturn(exportEtag)
        Map<String, String> headers = new HashMap<String, String>() {
          {
            put("Content-Type", "text/plain")
            put("X-Export-Filename", "foo.txt")
          }
        }
        when(configRepoPlugin.pipelineExport(pipeline, groupName)).thenReturn(from("message from plugin", headers))

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?pluginId=${pluginId}&groupName=${groupName}"), ['if-none-match': '"junk"'])

        assertThatResponse()
          .isOk()
          .hasHeader("Etag", "\"$exportEtag\"")
          .hasBody("message from plugin")
      }

      @Test
      void 'returns a 400 when pluginId is blank or missing'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(goConfigService.pipelineConfigNamed("pipeline1")).thenReturn(pipeline)

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?groupName=${groupName}"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("Request is missing parameter `pluginId`")

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?pluginId=%20"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("Request is missing parameter `pluginId`")
      }

      @Test
      void 'returns a 400 when groupName is blank or missing'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(goConfigService.pipelineConfigNamed("pipeline1")).thenReturn(pipeline)

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?pluginId=${pluginId}"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("Request is missing parameter `groupName`")

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?pluginId=${pluginId}&groupName=%20"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("Request is missing parameter `groupName`")
      }

      @Test
      void 'returns a 422 when pipeline is defined by a template'() {
        PipelineConfig pipeline = mock(PipelineConfig)
        when(pipeline.hasTemplate()).thenReturn(true)
        when(pipeline.name()).thenReturn(new CaseInsensitiveString("pipeline1"))

        when(goConfigService.pipelineConfigNamed("pipeline1")).thenReturn(pipeline)
        when(goConfigPluginService.isConfigRepoPlugin(pluginId)).thenReturn(false)

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?pluginId=${pluginId}&groupName=${groupName}"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Pipeline `pipeline1` cannot be exported because pipelines defined by templates are not yet supported by config-repo plugins.")
      }

      @Test
      void 'returns a 422 when plugin is not a configrepo plugin'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(goConfigService.pipelineConfigNamed("pipeline1")).thenReturn(pipeline)
        when(goConfigPluginService.isConfigRepoPlugin(pluginId)).thenReturn(false)

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?pluginId=${pluginId}&groupName=${groupName}"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Plugin `$pluginId` is not a config-repo plugin.")
      }

      @Test
      void 'returns a 422 when plugin does not support export'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(goConfigService.pipelineConfigNamed("pipeline1")).thenReturn(pipeline)
        when(goConfigPluginService.isConfigRepoPlugin(pluginId)).thenReturn(true)
        when(goConfigPluginService.supportsPipelineExport(pluginId)).thenReturn(false)

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?pluginId=${pluginId}&groupName=${groupName}"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Plugin `$pluginId` does not support pipeline config export.")
      }

      @Test
      void "should return 304 for export pipeline config if etag matches"() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(goConfigService.pipelineConfigNamed("pipeline1")).thenReturn(pipeline)
        when(goConfigPluginService.isConfigRepoPlugin(pluginId)).thenReturn(true)
        when(goConfigPluginService.supportsPipelineExport(pluginId)).thenReturn(true)
        when(goConfigPluginService.partialConfigProviderFor(pluginId)).thenReturn(configRepoPlugin)
        when(configRepoPlugin.etagForExport(pipeline, groupName)).thenReturn(exportEtag)

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?pluginId=${pluginId}&groupName=${groupName}"), ['if-none-match': "\"$exportEtag\""])

        assertThatResponse()
          .hasBody("")
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void "should return 404 for export pipeline config if pipeline is not found"() {
        when(goConfigService.pipelineConfigNamed("pipeline1")).thenReturn(null)

        getWithApiHeader(controller.controllerPath("${pipelinePath("pipeline1")}?pluginId=${pluginId}&groupName=${groupName}"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(HaltApiMessages.notFoundMessage())
          .hasContentType(controller.mimeType)
      }
    }
  }

}
