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
package com.thoughtworks.go.apiv1.pipelinesascodeinternal

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.ConfigRepoPlugin
import com.thoughtworks.go.config.CruiseConfig
import com.thoughtworks.go.config.GoConfigPluginService
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.ScmMaterialConfig
import com.thoughtworks.go.config.materials.SubprocessExecutionContext
import com.thoughtworks.go.config.update.CreatePipelineConfigCommand
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.plugin.access.configrepo.ConfigFileList
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.MaterialConfigConverter
import com.thoughtworks.go.server.service.MaterialService
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.SystemEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static com.thoughtworks.go.plugin.access.configrepo.ExportedConfig.from
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class PipelinesAsCodeInternalControllerV1Test implements SecurityServiceTrait, ControllerTrait<PipelinesAsCodeInternalControllerV1> {

  private static final String PLUGIN_ID = "test.config.plugin"
  private static final String GROUP_NAME = "test-group"
  private static final String PIPELINE_NAME = "test-pipeline"
  private static final String MATERIAL_URL = "git@someplace.com"
  private static final String MATERIAL_BRANCH = "master"
  private static final String MATERIAL_TYPE = "git"


  @Mock
  GoConfigPluginService pluginService

  @Mock
  PasswordDeserializer passwordDeserializer

  @Mock
  ConfigRepoPlugin configRepoPlugin

  @Mock
  PipelineConfigService pipelineService

  @Mock
  DefaultPluginInfoFinder defaultPluginInfoFinder

  @Mock
  MaterialService materialService

  @Mock
  MaterialConfigConverter materialConfigConverter

  @Mock
  SubprocessExecutionContext subprocessExecutionContext

  @Mock
  ConfigRepoService configRepoService

  @Mock
  SystemEnvironment systemEnvironment

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PipelinesAsCodeInternalControllerV1 createControllerInstance() {
    new PipelinesAsCodeInternalControllerV1(
      new ApiAuthenticationHelper(securityService, goConfigService),
      passwordDeserializer,
      goConfigService,
      pluginService,
      pipelineService,
      materialService,
      materialConfigConverter,
      subprocessExecutionContext,
      systemEnvironment,
      configRepoService
    )
  }

  @Nested
  class ConfigFiles {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "configFiles"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath("config_files", PLUGIN_ID), [:], [
          type      : MATERIAL_TYPE,
          attributes: [
            url   : MATERIAL_URL,
            branch: MATERIAL_BRANCH
          ]
        ])
      }
    }

    @Nested
    class AsAdmin {

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'fetches config files only for a given plugin when specified'() {
        when(configRepoPlugin.id()).thenReturn((PLUGIN_ID))
        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(true)
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(configRepoPlugin)

        when(configRepoService.hasConfigRepoByFingerprint(any(String))).thenReturn(false)
        when(configRepoPlugin.getConfigFiles(any(File), any(List))).thenReturn(ConfigFileList.from(["file1.yml", "file2.yml"]))

        doNothing().when(controller).checkoutFromMaterialConfig(any(MaterialConfig), any(File))

        postWithApiHeader(controller.controllerPath("config_files", PLUGIN_ID), [:], [
          type      : MATERIAL_TYPE,
          attributes: [
            url   : MATERIAL_URL,
            branch: MATERIAL_BRANCH
          ]
        ])

        assertThatResponse()
          .isOk()
          .hasJsonBody([
          plugins: [
            [
              plugin_id: PLUGIN_ID,
              files    : ["file1.yml", "file2.yml"],
              errors   : ""
            ]
          ]
        ])
      }

      void 'returns a 422 when specified plugin is not a configrepo plugin'() {
        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(false)

        postWithApiHeader(controller.controllerPath([pluginId: PLUGIN_ID], "config_files"), [:], [
          type      : MATERIAL_TYPE,
          attributes: [
            url   : MATERIAL_URL,
            branch: MATERIAL_BRANCH
          ]
        ])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Plugin `$PLUGIN_ID` is not a Pipelines-as-Code plugin.")
      }

      @Test
      void 'should return message saying that the repo is already being used with pac'() {
        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(true)
        when(configRepoService.hasConfigRepoByFingerprint(any(String))).thenReturn(true)

        postWithApiHeader(controller.controllerPath("config_files", PLUGIN_ID), [:], [
          type      : MATERIAL_TYPE,
          attributes: [
            url   : MATERIAL_URL,
            branch: MATERIAL_BRANCH
          ]
        ])

        assertThatResponse()
          .isConflict()
          .hasJsonMessage("Material is already being used as a config repository")
      }

      @Test
      void 'rejects non-SCM materials'() {
        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(true)
        when(configRepoService.hasConfigRepoByFingerprint(any(String))).thenReturn(false)
        Map<String, String> plugins = new HashMap<>()
        plugins.put("test", PLUGIN_ID)
        when(defaultPluginInfoFinder.pluginDisplayNameToPluginId(any(String))).thenReturn(plugins)

        postWithApiHeader(controller.controllerPath("config_files", PLUGIN_ID), [:], [
          type      : "dependency",
          attributes: [
            pipeline: "whatever",
            stage   : "meh"
          ]
        ])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("This material check requires an SCM repository; instead, supplied material was of type: DependencyMaterial")
      }

      @Test
      void 'validates material configuration and bubbles up any validation failures'() {
        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(true)
        when(configRepoService.hasConfigRepoByFingerprint(any(String))).thenReturn(false)
        Map<String, String> plugins = new HashMap<>()
        plugins.put("test", PLUGIN_ID)
        when(defaultPluginInfoFinder.pluginDisplayNameToPluginId(any(String))).thenReturn(plugins)

        doAnswer({ InvocationOnMock invocation ->
          ScmMaterialConfig material = (ScmMaterialConfig) invocation.getArgument(0)
          material.errors().add("url", "that's a no-go.")
        }).when(controller).validateMaterial(any(MaterialConfig))

        postWithApiHeader(controller.controllerPath("config_files", PLUGIN_ID), [:], [
          type      : MATERIAL_TYPE,
          attributes: [
            url   : MATERIAL_URL,
            branch: MATERIAL_BRANCH
          ]
        ])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Please fix the following SCM configuration errors: that's a no-go.")
      }

      @Test
      void 'should provide error message when there is an exception'() {
        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(true)
        when(configRepoService.hasConfigRepoByFingerprint(any(String))).thenReturn(false)
        Map<String, String> plugins = new HashMap<>()
        plugins.put("test", PLUGIN_ID)
        when(defaultPluginInfoFinder.pluginDisplayNameToPluginId(any(String))).thenReturn(plugins)

        doThrow(new RuntimeException("An error")).when(controller).checkoutFromMaterialConfig(any(MaterialConfig), any(File))

        postWithApiHeader(controller.controllerPath("config_files", PLUGIN_ID), [:], [
          type      : MATERIAL_TYPE,
          attributes: [
            url   : MATERIAL_URL,
            branch: MATERIAL_BRANCH
          ]
        ])

        assertThatResponse()
          .isInternalServerError()
          .hasJsonMessage("An error")
      }
    }
  }

  @Nested
  class Preview {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "preview"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath([group: GROUP_NAME], "preview", PLUGIN_ID), [:], [
          name: PIPELINE_NAME
        ])
      }
    }

    @Nested
    class AsAdmin {

      private static final String ETAG = 'big_etag_for_export'

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        when(configRepoPlugin.id()).thenReturn(PLUGIN_ID)
      }

      @Test
      void 'should be able to export pipeline config if user is admin and etag is stale'() {
        when(configRepoPlugin.etagForExport(any(PipelineConfig), eq(GROUP_NAME))).thenReturn(ETAG)
        when(configRepoPlugin.pipelineExport(any(PipelineConfig), eq(GROUP_NAME))).thenReturn(from("message from plugin", [
          "Content-Type"     : "text/plain",
          "X-Export-Filename": "foo.txt"
        ]))

        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(true)
        when(pluginService.supportsPipelineExport(PLUGIN_ID)).thenReturn(true)
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(configRepoPlugin)

        postWithApiHeader(controller.controllerPath([group: GROUP_NAME], "preview", PLUGIN_ID), [:], [
          name: PIPELINE_NAME
        ])

        assertThatResponse()
          .isOk()
          .hasHeader("Etag", "\"$ETAG\"")
          .hasBody("message from plugin")
      }

      @Test
      void 'returns a 422 when plugin is not a configrepo plugin'() {
        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(false)

        postWithApiHeader(controller.controllerPath([group: GROUP_NAME], "preview", PLUGIN_ID), [:], [
          name: PIPELINE_NAME
        ])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Plugin `$PLUGIN_ID` is not a Pipelines-as-Code plugin.")
      }

      @Test
      void 'returns a 422 when plugin does not support export'() {
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(configRepoPlugin)
        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(true)
        when(pluginService.supportsPipelineExport(PLUGIN_ID)).thenReturn(false)

        postWithApiHeader(controller.controllerPath([group: GROUP_NAME], "preview", PLUGIN_ID), [:], [
          name: PIPELINE_NAME
        ])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Plugin `$PLUGIN_ID` does not support pipeline config export.")
      }

      @Test
      void "should return 304 for export pipeline config if etag matches"() {
        when(configRepoPlugin.etagForExport(any(PipelineConfig), eq(GROUP_NAME))).thenReturn(ETAG)
        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(true)
        when(pluginService.supportsPipelineExport(PLUGIN_ID)).thenReturn(true)
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(configRepoPlugin)

        postWithApiHeader(controller.controllerPath([group: GROUP_NAME], "preview", PLUGIN_ID), ["if-none-match": "\"$ETAG\""], [
          name: PIPELINE_NAME
        ])

        assertThatResponse()
          .hasBody("")
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void "validates ad-hoc pipeline config when ?validate=true"() {
        CreatePipelineConfigCommand cmd = mock(CreatePipelineConfigCommand)
        CruiseConfig config = mock(CruiseConfig)
        PipelineConfig pipeline = null

        when(pluginService.isConfigRepoPlugin(PLUGIN_ID)).thenReturn(true)
        when(pluginService.supportsPipelineExport(PLUGIN_ID)).thenReturn(true)
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(configRepoPlugin)
        when(pipelineService.createPipelineConfigCommand(eq(currentUsername()), any(PipelineConfig), isNull(), eq(GROUP_NAME))).thenAnswer(new Answer<Object>() {
          @Override
          Object answer(InvocationOnMock invocation) throws Throwable {
            pipeline = invocation.getArgument(1, PipelineConfig)
            return cmd
          }
        })

        when(goConfigService.preprocessedCruiseConfigForPipelineUpdate(cmd)).thenReturn(config)
        when(cmd.isValid(config)).thenAnswer(new Answer<Object>() {
          @Override
          Object answer(InvocationOnMock invocation) throws Throwable {
            if (null == pipeline) {
              throw new IllegalStateException("Expected pipeline config to be set in this test")
            }

            pipeline.addError("name", "That's an uncreative name")
            return false
          }
        })

        postWithApiHeader(controller.controllerPath([group: GROUP_NAME, validate: "true"], "preview", PLUGIN_ID), [:], [
          name: PIPELINE_NAME
        ])

        verify(goConfigService, times(1)).preprocessedCruiseConfigForPipelineUpdate(cmd)
        verify(cmd, times(1)).isValid(config)

        String errorMessage = "Please fix the validation errors for pipeline $PIPELINE_NAME."

        assertThatResponse().
          isUnprocessableEntity().
          hasContentType(controller.mimeType).
          hasJsonBody([
            message: errorMessage,
            data   : [
              errors               : [
                name: ["That's an uncreative name"]
              ],
              label_template       : '${COUNT}',
              lock_behavior        : "none",
              name                 : PIPELINE_NAME,
              template             : null,
              group                : null,
              origin               : [
                type: "gocd"
              ],
              parameters           : [],
              environment_variables: [],
              materials            : [],
              stages               : null,
              tracking_tool        : null,
              timer                : null
            ]
          ])
      }
    }

  }
}
