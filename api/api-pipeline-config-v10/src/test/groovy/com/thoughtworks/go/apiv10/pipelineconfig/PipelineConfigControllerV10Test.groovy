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
package com.thoughtworks.go.apiv10.pipelineconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv10.admin.pipelineconfig.PipelineConfigControllerV10
import com.thoughtworks.go.apiv10.admin.shared.representers.PipelineConfigRepresenter
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.config.materials.PackageMaterialConfig
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig
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
import com.thoughtworks.go.server.service.PipelinePauseChecker
import com.thoughtworks.go.server.service.PipelinePauseService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService
import com.thoughtworks.go.server.service.support.toggle.Toggles
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
import static com.thoughtworks.go.helper.MaterialConfigsMother.git
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class PipelineConfigControllerV10Test implements SecurityServiceTrait, ControllerTrait<PipelineConfigControllerV10> {
  @Mock
  private FeatureToggleService featureToggleService;

  @BeforeEach
  void setUp() {
    initMocks(this)
    Toggles.initializeWith(featureToggleService);
    when(featureToggleService.isToggleOn(Toggles.TEST_DRIVE)).thenReturn(false)
  }

  @Mock
  private PipelineConfigService pipelineConfigService

  @Mock
  private EntityHashingService entityHashingService

  @Mock
  private PasswordDeserializer passwordDeserializer

  @Mock
  PipelinePauseService pipelinePauseService

  @Mock
  PipelinePauseChecker pipelinePauseChecker

  @Override
  PipelineConfigControllerV10 createControllerInstance() {
    return new PipelineConfigControllerV10(pipelineConfigService, pipelinePauseService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, passwordDeserializer, goConfigService, goCache)
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
      def groupName = 'default'

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        when(securityService.hasViewPermissionForPipeline(any(), any())).thenReturn(true)
        when(goConfigService.findGroupNameByPipeline(any(CaseInsensitiveString.class))).thenReturn(groupName)
      }

      @Test
      void 'should show pipeline config for an admin'() {
        def pipeline = PipelineConfigMother.pipelineConfig('pipeline1')
        pipeline.setOrigin(new FileConfigOrigin())
        def pipelineDigest = 'digest_for_pipeline_config'

        when(pipelineConfigService.getPipelineConfig('pipeline1')).thenReturn(pipeline)
        when(entityHashingService.hashForEntity(pipeline, groupName)).thenReturn(pipelineDigest)

        getWithApiHeader(controller.controllerPath("/pipeline1"))

        def expectedJSON = toObjectString({ PipelineConfigRepresenter.toJSON(it, pipeline, groupName) })

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(expectedJSON)
          .hasHeader("Etag", '"digest_for_pipeline_config"')
      }

      @Test
      void "should return 304 for show pipeline config if etag sent in request is fresh"() {
        def pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())
        def pipeline_digest = 'digest_for_pipeline_config'

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
        when(entityHashingService.hashForEntity(pipeline, groupName)).thenReturn(pipeline_digest)

        getWithApiHeader(controller.controllerPath('/pipeline1'), ['if-none-match': '"digest_for_pipeline_config"'])

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
          .hasJsonMessage(controller.entityType.notFoundMessage("pipeline1"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void "should show pipeline config if etag sent in request is stale"() {
        def pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())
        def pipeline_digest = 'digest_for_pipeline_config'

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
        when(entityHashingService.hashForEntity(pipeline, groupName)).thenReturn(pipeline_digest)

        getWithApiHeader(controller.controllerPath('/pipeline1'), ['if-none-match': '"junk"'])

        def expectedJSON = toObjectString({ PipelineConfigRepresenter.toJSON(it, pipeline, groupName) })

        assertThatResponse()
          .isOk()
          .hasEtag('"digest_for_pipeline_config"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJson(expectedJSON)
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

      def groupName = "default"

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        when(securityService.hasViewPermissionForPipeline(any(), any())).thenReturn(true)
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null)
        when(goConfigService.findGroupNameByPipeline(any(CaseInsensitiveString.class))).thenReturn(groupName)
      }

      @Test
      void "should allow admin users create a new pipeline config in any group"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null, pipelineConfig)

        postWithApiHeader(controller.controllerPath(), [group: "new_grp", pipeline: toObject({
          PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName)
        })])

        verify(pipelineConfigService).createPipelineConfig(any(Username.class) as Username, any(PipelineConfig.class) as PipelineConfig, any(HttpLocalizedOperationResult.class) as HttpLocalizedOperationResult, eq("new_grp"))

        def expectedJSON = toObjectString({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName) })

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(expectedJSON)
      }

      @Test
      void "should create a pipeline in a paused state with a specified pause cause when X-pause-pipeline header is set to true and X-pause-cause header is specified"() {
        def pipelineName = "pipeline1"
        def pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName)
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig(pipelineName)).thenReturn(null, pipelineConfig)
        when(pipelinePauseService.isPaused('pipeline1')).thenReturn(true)

        def pauseCause = "test-pause-cause"
        postWithApiHeader(controller.controllerPath(), ["X-pause-pipeline": "true", "X-pause-cause": pauseCause], [group: "new_grp", pipeline: toObject({
          PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName)
        })])

        verify(pipelineConfigService).createPipelineConfig(any(Username.class) as Username, any(PipelineConfig.class) as PipelineConfig, any(HttpLocalizedOperationResult.class) as HttpLocalizedOperationResult, eq("new_grp"))

        def expectedJSON = toObjectString({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName) })

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(expectedJSON)

        verify(pipelinePauseService).pause(eq(pipelineName), eq(pauseCause), any(Username.class))
      }


      @Test
      void "should create a pipeline in a paused state with default pause cause when X-pause-pipeline header is set to true and X-pause-cause header is not specified"() {
        def pipelineName = "pipeline1"
        def pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName)
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig(pipelineName)).thenReturn(null, pipelineConfig)
        when(pipelinePauseService.isPaused('pipeline1')).thenReturn(true)

        postWithApiHeader(controller.controllerPath(), ["X-pause-pipeline": "true"], [group: "new_grp", pipeline: toObject({
          PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName)
        })])

        verify(pipelineConfigService).createPipelineConfig(any(Username.class) as Username, any(PipelineConfig.class) as PipelineConfig, any(HttpLocalizedOperationResult.class) as HttpLocalizedOperationResult, eq("new_grp"))

        def expectedJSON = toObjectString({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName) })

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(expectedJSON)

        def pauseCause = "No pause cause was specified when pipeline was created via API"
        verify(pipelinePauseService).pause(eq(pipelineName), eq(pauseCause), any(Username.class))
      }

      @Test
      void "should create a pipeline config in running state when X-pause-pipeline header is set to false"() {
        def pipelineName = "pipeline1"
        def pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName)
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig(pipelineName)).thenReturn(null, pipelineConfig)
        when(pipelinePauseService.isPaused('pipeline1')).thenReturn(false)

        postWithApiHeader(controller.controllerPath(), ["X-pause-pipeline": "false"], [group: "new_grp", pipeline: toObject({
          PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName)
        })])

        verify(pipelineConfigService).createPipelineConfig(any(Username.class) as Username, any(PipelineConfig.class) as PipelineConfig, any(HttpLocalizedOperationResult.class) as HttpLocalizedOperationResult, eq("new_grp"))

        def expectedJSON = toObjectString({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName) })

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(expectedJSON)

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult()
        PipelinePauseChecker checker = new PipelinePauseChecker(pipelineName, pipelinePauseService)
        checker.check(result)

        assertTrue(result.getServerHealthState().isSuccess())
      }

      @Test
      void "should create a pipeline config in running state when X-pause-pipeline header is not specified"() {
        def pipelineName = "pipeline1"
        def pipelineConfig = PipelineConfigMother.pipelineConfig(pipelineName)
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig(pipelineName)).thenReturn(null, pipelineConfig)
        when(pipelinePauseService.isPaused('pipeline1')).thenReturn(false)

        postWithApiHeader(controller.controllerPath(), [group: "new_grp", pipeline: toObject({
          PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName)
        })])

        verify(pipelineConfigService).createPipelineConfig(any(Username.class) as Username, any(PipelineConfig.class) as PipelineConfig, any(HttpLocalizedOperationResult.class) as HttpLocalizedOperationResult, eq("new_grp"))

        def expectedJSON = toObjectString({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName) })

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(expectedJSON)

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult()
        PipelinePauseChecker checker = new PipelinePauseChecker(pipelineName, pipelinePauseService)
        checker.check(result)

        assertTrue(result.getServerHealthState().isSuccess())
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
          'If-Match'    : 'cached-digest',
          'content-type': 'application/json'
        ], toObjectString({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName) }))
      }
    }

    @Nested
    class AsAdmin {

      def groupName = "default"

      @BeforeEach
      void setup() {
        enableSecurity()
        loginAsAdmin()
        when(goConfigService.findGroupNameByPipeline(any(CaseInsensitiveString.class))).thenReturn(groupName)
      }

      @Test
      void "should update pipeline config for an admin"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(entityHashingService.hashForEntity(pipelineConfig, groupName)).thenReturn('digest')
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, toObject({
          PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName)
        }))

        def expectedJSON = toObjectString({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName) })

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(expectedJSON)
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
        def gitMaterial = git("https://github.com/config-repos/repo", "master")
        def origin = new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(gitMaterial, "json-plugib", "id"), "revision1")
        pipelineConfig.setOrigin(origin)

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, pipeline())

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Can not operate on pipeline 'pipeline1' as it is defined remotely in 'https://github.com/config-repos/repo at revision1'.")
      }

      @Test
      void "should not update pipeline config when the group is not specified"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)
        when(entityHashingService.hashForEntity(pipelineConfig, groupName)).thenReturn('digest')

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
          'content-type': 'application/json'
        ]

        def json = toObject({
          PipelineConfigRepresenter.toJSON(it, pipelineConfig, groupName)
        })
        json.remove("group")
        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, json)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Pipeline group must be specified for creating a pipeline.")
      }

      @Test
      void "should handle server validation errors"() {
        HttpLocalizedOperationResult result
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(entityHashingService.hashForEntity(pipelineConfig, groupName)).thenReturn('digest')
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        pipelineConfig.addError("labelTemplate", String.format(PipelineConfig.LABEL_TEMPLATE_ERROR_MESSAGE, 'foo bar'))

        when(pipelineConfigService.updatePipelineConfig(any(), any(), anyString(), eq("digest"), any())).then({ InvocationOnMock invocation ->
          result = invocation.getArguments()[4]
          result.unprocessableEntity("message from server")
        })

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
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
          'If-Match'    : 'digest',
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
        when(entityHashingService.hashForEntity(pipelineConfig, groupName)).thenReturn('digest')
        when(pipelineConfigService.updatePipelineConfig(any(), any(), anyString(), any(), any())).then({ InvocationOnMock invocation ->
          pipelineBeingSaved = invocation.getArguments()[1]
        })

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
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
        when(entityHashingService.hashForEntity(pipelineConfig, groupName)).thenReturn('digest')

        when(pipelineConfigService.updatePipelineConfig(any(), any(), anyString(), any(), any())).then({ InvocationOnMock invocation ->
          pipelineBeingSaved = invocation.getArguments()[1]
        })

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'digest',
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
          .hasJsonMessage(EntityType.Pipeline.notFoundMessage("non-existent-pipeline"))
      }

      @Test
      void "should not delete pipeline config when the pipeline is defined remotely"() {
        def pipeline = PipelineConfigMother.pipelineConfig("pipeline1")

        def gitMaterial = git("https://github.com/config-repos/repo", "master")
        def origin = new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(gitMaterial, "json-plugin", "id"), "revision1")
        pipeline.setOrigin(origin)

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)

        deleteWithApiHeader(controller.controllerPath("/pipeline1"))
        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Can not operate on pipeline 'pipeline1' as it is defined remotely in 'https://github.com/config-repos/repo at revision1'.")
      }
    }
  }

  @Nested
  class ExtractToTemplate {
    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "extractToTemplate"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath('/foo/extract_to_template'), [:])
      }
    }

    @Nested
    class AsAdmin {
      private PipelineConfig pipeline = PipelineConfigMother.pipelineConfigWithTemplate("pipeline1", "new-template");

      @BeforeEach
      void setup() {
        enableSecurity()
        loginAsAdmin()
        pipeline.setOrigin(new FileConfigOrigin());
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline1"))).thenReturn("some-group")
      }

      @Test
      void "should create template pipeline config for an admin"() {
        putWithApiHeader(controller.controllerPath("/pipeline1/extract_to_template"), ["template_name": "new-template"])
        verify(pipelineConfigService).extractTemplateFromPipeline("pipeline1", "new-template", currentUsername())

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ PipelineConfigRepresenter.toJSON(it, pipeline, "some-group") }))
      }
    }
  }

  static def invalidPipeline() {
    return [
      label_template: "\${COUNT}",
      lock_behavior : "none",
      name          : "pipeline1",
      group         : "group",
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
      group         : "group",
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
