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

package com.thoughtworks.go.apiv7.admin.pipelineconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.apiv7.admin.pipelineconfig.representers.PipelineConfigRepresenter
import com.thoughtworks.go.config.ConfigRepoPlugin
import com.thoughtworks.go.config.GoConfigPluginService
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.materials.PackageMaterialConfig
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig
import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother
import com.thoughtworks.go.domain.scm.SCMMother
import com.thoughtworks.go.helper.GoConfigMother
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class PipelineConfigControllerV7Test implements SecurityServiceTrait, ControllerTrait<PipelineConfigControllerV7> {

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  private PipelineConfigService pipelineConfigService

  @Mock
  private EntityHashingService entityHashingService

  @Mock
  private PasswordDeserializer passwordDeserializer

  @Mock
  private GoConfigPluginService goConfigPluginService

  @Mock
  private ConfigRepoPlugin configRepoPlugin

  private final String pipelineEtag = 'md5'

  @Override
  PipelineConfigControllerV7 createControllerInstance() {
    return new PipelineConfigControllerV7(pipelineConfigService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, passwordDeserializer, goConfigService, goConfigPluginService)
  }

  @Nested
  class Export {
    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {


      @Override
      String getControllerMethodUnderTest() {
        return "export"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('/foo/export'))
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

      String pluginId = 'config.repo.json'
      String groupName = 'group1'

      @Test
      void 'should be able to export pipeline config if user is admin and etag is stale'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig('pipeline1')
        pipeline.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig('pipeline1')).thenReturn(pipeline)
        when(goConfigPluginService.supportsPipelineExport(pluginId)).thenReturn(true)
        when(goConfigPluginService.partialConfigProviderFor(pluginId)).thenReturn(configRepoPlugin)
        when(configRepoPlugin.etagForExport(pipeline, groupName)).thenReturn(exportEtag)

        when(configRepoPlugin.pipelineExport(pipeline, groupName)).thenReturn("message from plugin")

        getWithApiHeader(controller.controllerPath("/pipeline1/export?pluginId=${pluginId}&groupName=${groupName}"), ['if-none-match': '"junk"'])

        assertThatResponse()
          .isOk()
          .hasHeader("Etag", "\"$exportEtag\"")
          .hasBody("message from plugin")
      }

      @Test
      void 'returns a 400 when pluginId is blank or missing'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)

        getWithApiHeader(controller.controllerPath("/pipeline1/export?groupName=${groupName}"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("Request is missing parameter `pluginId`")

        getWithApiHeader(controller.controllerPath("/pipeline1/export?pluginId=%20"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("Request is missing parameter `pluginId`")
      }

      @Test
      void 'returns a 400 when groupName is blank or missing'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)

        getWithApiHeader(controller.controllerPath("/pipeline1/export?pluginId=${pluginId}"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("Request is missing parameter `groupName`")

        getWithApiHeader(controller.controllerPath("/pipeline1/export?pluginId=${pluginId}&groupName=%20"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("Request is missing parameter `groupName`")
      }

      @Test
      void 'returns a 422 when plugin does not support export'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
        when(goConfigPluginService.supportsPipelineExport(pluginId)).thenReturn(false)

        getWithApiHeader(controller.controllerPath("/pipeline1/export?pluginId=${pluginId}&groupName=${groupName}"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Plugin `$pluginId` does not support pipeline export or is not a config repo plugin.")
      }

      @Test
      void "should return 304 for export pipeline config if etag matches"() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
        when(goConfigPluginService.supportsPipelineExport(pluginId)).thenReturn(true)
        when(goConfigPluginService.partialConfigProviderFor(pluginId)).thenReturn(configRepoPlugin)
        when(configRepoPlugin.etagForExport(pipeline, groupName)).thenReturn(exportEtag)

        getWithApiHeader(controller.controllerPath("/pipeline1/export?pluginId=${pluginId}&groupName=${groupName}"), ['if-none-match': "\"$exportEtag\""])

        assertThatResponse()
          .hasBody("")
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void "should return 404 for export pipeline config if pipeline is not found"() {
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null)

        getWithApiHeader(controller.controllerPath("/pipeline1/export?pluginId=${pluginId}&groupName=${groupName}"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(HaltApiMessages.notFoundMessage())
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Nested
  class Show {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {


      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('/foo'))
      }
    }

    @Nested
    class AsAdmin {

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        when(securityService.hasViewPermissionForPipeline(any(), any())).thenReturn(true)
      }

      @Test
      void 'should show pipeline config for an admin'() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig('pipeline1')
        pipeline.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig('pipeline1')).thenReturn(pipeline)
        when(entityHashingService.md5ForEntity(pipeline)).thenReturn(pipelineEtag)

        getWithApiHeader(controller.controllerPath("/pipeline1"))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(pipeline, PipelineConfigRepresenter)
          .hasHeader("Etag", "\"$pipelineEtag\"")
      }

      @Test
      void "should return 304 for show pipeline config if etag sent in request is fresh"() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
        when(entityHashingService.md5ForEntity(pipeline)).thenReturn(pipelineEtag)

        getWithApiHeader(controller.controllerPath('/pipeline1'), ['if-none-match': "\"$pipelineEtag\""])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void "should return 404 for show pipeline config if pipeline is not found"() {
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null)

        getWithApiHeader(controller.controllerPath("/pipeline1"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(HaltApiMessages.notFoundMessage())
          .hasContentType(controller.mimeType)
      }

      @Test
      void "should show pipeline config if etag sent in request is stale"() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
        when(entityHashingService.md5ForEntity(pipeline)).thenReturn(pipelineEtag)

        getWithApiHeader(controller.controllerPath('/pipeline1'), ['if-none-match': '"junk"'])

        assertThatResponse()
          .isOk()
          .hasEtag("\"$pipelineEtag\"")
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(pipeline, PipelineConfigRepresenter)
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "create"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), [group: "new_grp"])
      }

    }

    @Nested
    class AsAdmin {

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        when(securityService.hasViewPermissionForPipeline(any(), any())).thenReturn(true)
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null)
      }

      @Test
      void "should allow admin users create a new pipeline config in any group"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null, pipelineConfig)

        postWithApiHeader(controller.controllerPath(), [group: "new_grp", pipeline: toObject({
          PipelineConfigRepresenter.toJSON(it, pipelineConfig)
        })])

        verify(pipelineConfigService).createPipelineConfig(any(Username.class) as Username, any(PipelineConfig.class) as PipelineConfig, any(HttpLocalizedOperationResult.class) as HttpLocalizedOperationResult, eq("new_grp"))
        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(pipelineConfig, PipelineConfigRepresenter)
      }

      @Test
      void "should handle server validation errors"() {
        HttpLocalizedOperationResult result
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.createPipelineConfig(any(), any(), any(), eq("group"))).then({ InvocationOnMock invocation ->
          pipelineConfig.addError("labelTemplate", String.format(PipelineConfig.LABEL_TEMPLATE_ERROR_MESSAGE, "foo bar"))
          result = invocation.getArguments()[2]
          result.unprocessableEntity("message from server")
        })

        postWithApiHeader(controller.controllerPath(), [group: 'group', pipeline: invalidPipeline()])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("message from server")

      }

      @Test
      void "should fail if a pipeline by same name already exists"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        postWithApiHeader(controller.controllerPath(), [group: "new_grp", pipeline: pipeline()])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Failed to add pipeline 'pipeline1'. Another pipeline with the same name already exists.")
      }

      @Test
      void "should fail if group is blank"() {
        postWithApiHeader(controller.controllerPath(), [group: '', pipeline: pipeline()])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Pipeline group must be specified for creating a pipeline.")
      }

      @Test
      void "should set package definition on to package material before save"() {
        def packageRepository = PackageRepositoryMother.create("repoid")
        def cruiseConfig = GoConfigMother.defaultCruiseConfig()
        cruiseConfig.getPackageRepositories().add(packageRepository)
        PipelineConfig pipelineBeingSaved = null
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig)
        when(pipelineConfigService.createPipelineConfig(any(), any(), any(), eq("group"))).then({ InvocationOnMock invocation ->
          pipelineBeingSaved = invocation.getArguments()[1]
        })

        postWithApiHeader(controller.controllerPath(), [group: "group", pipeline: pipelineWithPluggableMaterial("pipeline1", "package", "package-name")])

        assertThatResponse().isOk()

        assertEquals(packageRepository.findPackage("package-name"), ((PackageMaterialConfig) pipelineBeingSaved.materialConfigs().first()).getPackageDefinition())
      }

      @Test
      void "should set scm config on to pluggable scm material before save"() {
        def scm = SCMMother.create("scm-id")
        def cruiseConfig = GoConfigMother.defaultCruiseConfig()
        cruiseConfig.getSCMs().add(scm)

        PipelineConfig pipelineBeingSaved = null
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig)
        when(pipelineConfigService.createPipelineConfig(any(), any(), any(), eq("group"))).then({ InvocationOnMock invocation ->
          pipelineBeingSaved = invocation.getArguments()[1]
        })

        postWithApiHeader(controller.controllerPath(), [group: "group", pipeline: pipelineWithPluggableMaterial("pipeline1", "plugin", "scm-id")])

        assertThatResponse().isOk()
        assertEquals(scm, (((PluggableSCMMaterialConfig) pipelineBeingSaved.materialConfigs().first()).getSCMConfig()))
      }
    }
  }

  @Nested
  class Update {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("foo")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        putWithApiHeader(controller.controllerPath('/foo'), [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-md5',
          'content-type': 'application/json'
        ], toObjectString({ PipelineConfigRepresenter.toJSON(it, pipelineConfig) }))
      }
    }

    @Nested
    class AsAdmin {

      @BeforeEach
      void setup() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void "should update pipeline config for an admin"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(entityHashingService.md5ForEntity(pipelineConfig)).thenReturn(pipelineEtag)
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : pipelineEtag,
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, toObject({
          PipelineConfigRepresenter.toJSON(it, pipelineConfig)
        }))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(pipelineConfig, PipelineConfigRepresenter)
      }

      @Test
      void "should not update pipeline config if etag passed does not match the one on server"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'old-etag',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, pipeline())

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for pipeline 'pipeline1'. Please update your copy of the config with the changes and try again.")
      }

      @Test
      void "should not update pipeline config if no etag is passed"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]
        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, pipeline())

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for pipeline 'pipeline1'. Please update your copy of the config with the changes and try again.")

      }

      @Test
      void "should not update pipeline config when the pipeline is defined remotely"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        def gitMaterial = new GitMaterialConfig("https://github.com/config-repos/repo", "master")
        def origin = new RepoConfigOrigin(new ConfigRepoConfig(gitMaterial, "json-plugib"), "revision1")
        pipelineConfig.setOrigin(origin)

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : pipelineEtag,
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, pipeline())

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Can not operate on pipeline 'pipeline1' as it is defined remotely in 'https://github.com/config-repos/repo at revision1'.")
      }

      @Test
      void "should handle server validation errors"() {
        HttpLocalizedOperationResult result
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(entityHashingService.md5ForEntity(pipelineConfig)).thenReturn(pipelineEtag)
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        pipelineConfig.addError("labelTemplate", String.format(PipelineConfig.LABEL_TEMPLATE_ERROR_MESSAGE, 'foo bar'))

        when(pipelineConfigService.updatePipelineConfig(any(), any(), eq(pipelineEtag), any())).then({ InvocationOnMock invocation ->
          result = invocation.getArguments()[3]
          result.unprocessableEntity("message from server")
        })

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : pipelineEtag,
          'content-type': 'application/json'
        ]


        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, invalidPipeline())

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("message from server")

      }

      @Test
      void "should not allow renaming a pipeline"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : pipelineEtag,
          'content-type': 'application/json'
        ]


        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, pipeline("renamed_pipeline"))

        assertThatResponse()
          .hasStatus(422)
          .hasJsonMessage("Renaming of pipelines is not supported by this API.")
      }

      @Test
      void "should set package definition on to package material before save"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())

        def packageRepository = PackageRepositoryMother.create("repoid")
        def cruiseConfig = GoConfigMother.defaultCruiseConfig()
        cruiseConfig.getPackageRepositories().add(packageRepository)
        PipelineConfig pipelineBeingSaved = null

        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig)
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)
        when(entityHashingService.md5ForEntity(pipelineConfig)).thenReturn(pipelineEtag)
        when(pipelineConfigService.updatePipelineConfig(any(), any(), any(), any())).then({ InvocationOnMock invocation ->
          pipelineBeingSaved = invocation.getArguments()[1]
        })

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : pipelineEtag,
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, pipelineWithPluggableMaterial("pipeline1", "package", "package-name"))

        assertThatResponse().isOk()
        assertEquals(packageRepository.findPackage("package-name"), ((PackageMaterialConfig) pipelineBeingSaved.materialConfigs().first()).getPackageDefinition())
      }

      @Test
      void "should set scm config on to pluggable scm material before save"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())

        def scm = SCMMother.create("scm-id")
        def cruiseConfig = GoConfigMother.defaultCruiseConfig()
        cruiseConfig.getSCMs().add(scm)

        PipelineConfig pipelineBeingSaved = null

        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig)
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)
        when(entityHashingService.md5ForEntity(pipelineConfig)).thenReturn(pipelineEtag)

        when(pipelineConfigService.updatePipelineConfig(any(), any(), any(), any())).then({ InvocationOnMock invocation ->
          pipelineBeingSaved = invocation.getArguments()[1]
        })

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : pipelineEtag,
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, pipelineWithPluggableMaterial("pipeline1", "plugin", "scm-id"))

        assertThatResponse().isOk()
        assertEquals(scm, (((PluggableSCMMaterialConfig) pipelineBeingSaved.materialConfigs().first()).getSCMConfig()))
      }
    }
  }

  @Nested
  class Destroy {
    private PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1");

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "destroy"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath('/foo'))
      }
    }

    @Nested
    class AsAdmin {

      @BeforeEach
      void setup() {
        enableSecurity()
        loginAsAdmin()
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
      }

      @Test
      void "should delete pipeline config for an admin"() {
        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage("The pipeline 'pipeline1' was deleted successfully.")
        }).when(pipelineConfigService).deletePipelineConfig(any(), eq(this.pipeline), any())


        deleteWithApiHeader(controller.controllerPath("/pipeline1"))

        assertThatResponse()
          .isOk()
          .hasJsonMessage("The pipeline 'pipeline1' was deleted successfully.")
      }

      @Test
      void "should render not found if the specified pipeline is absent"() {
        when(pipelineConfigService.getPipelineConfig("non-existent-pipeline")).thenReturn(null)

        deleteWithApiHeader(controller.controllerPath("/non-existent-pipeline"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Either the resource you requested was not found, or you are not authorized to perform this action.")
      }

      @Test
      void "should not delete pipeline config when the pipeline is defined remotely"() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1")

        def gitMaterial = new GitMaterialConfig("https://github.com/config-repos/repo", "master")
        def origin = new RepoConfigOrigin(new ConfigRepoConfig(gitMaterial, "json-plugin"), "revision1")
        pipeline.setOrigin(origin)

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)

        deleteWithApiHeader(controller.controllerPath("/pipeline1"))
        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Can not operate on pipeline 'pipeline1' as it is defined remotely in 'https://github.com/config-repos/repo at revision1'.")
      }
    }
  }

  static def invalidPipeline() {
    return [
      label_template: "\${COUNT}",
      lock_behavior : "none",
      name          : "pipeline1",
      materials     : [
        [
          type      : "svn",
          attributes: [
            name           : "http___some_svn_url",
            auto_update    : true,
            url            : "http://some/svn/url",
            destination    : "svnDir",
            filter         : null,
            check_externals: false,
            username       : null,
            password       : null
          ]
        ]
      ],
      stages        : [[name: "mingle", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: [type: "success", authorization: [:]], jobs: []]],
      errors        : [label_template: ["Invalid label. Label should be composed of alphanumeric text, it should contain the builder number as \${COUNT}, can contain a material revision as \${<material-name>} of \${<material-name>[:<number>]}, or use params as #{<param-name>}."]]
    ]
  }

  static def pipelineWithPluggableMaterial(pipeline_name, material_type, ref) {
    return [
      label_template: "\${COUNT}",
      name          : pipeline_name,
      materials     :
        [[
           type      : material_type,
           attributes:
             [
               ref: ref
             ]
         ]],
      stages        :
        [[
           name: "up42_stage",
           jobs:
             [[
                name : "up42_job",
                tasks:
                  [[
                     type      : "exec",
                     attributes:
                       [
                         command: "ls"
                       ]
                   ]]
              ]]
         ]]
    ]
  }

  static def pipeline(pipeline_name = "pipeline1", material_type = "hg", task_type = "exec") {
    return [
      label_template       : "Jyoti-\${COUNT}",
      lock_behavior        : "none",
      name                 : pipeline_name,
      template_name        : null,
      parameters           : [],
      environment_variables: [],
      materials            :
        [[
           type       : material_type,
           attributes :
             [
               url        : "../manual-testing/ant_hg/dummy",
               destination: "dest_dir",
               filter     :
                 [
                   ignore: []
                 ]
             ],
           name       : "dummyhg",
           auto_update: true
         ]],
      stages               :
        [[
           name                   : "up42_stage",
           fetch_materials        : true,
           clean_working_directory: false,
           never_cleanup_artifacts: false,
           approval               :
             [
               type         : "success",
               authorization:
                 [
                   roles: [],
                   users: []
                 ]
             ],
           environment_variables  : [],
           jobs                   :
             [[
                name                 : "up42_job",
                run_on_all_agents    : false,
                environment_variables:
                  [],
                resources            :
                  [],
                tasks                :
                  [[
                     type      : task_type,
                     attributes:
                       [
                         command    : "ls",
                         working_dir: null
                       ],
                     run_if    : []
                   ]],
                tabs                 : [],
                artifacts            : [],
                properties           : []
              ]]
         ]],
      mingle               :
        [
          base_url               : null,
          project_identifier     : null,
          mql_grouping_conditions: null
        ]
    ]
  }
}
