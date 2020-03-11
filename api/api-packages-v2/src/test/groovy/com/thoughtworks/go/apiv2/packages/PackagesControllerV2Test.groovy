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
package com.thoughtworks.go.apiv2.packages

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.packages.representers.PackageDefinitionRepresenter
import com.thoughtworks.go.apiv2.packages.representers.PackageDefinitionsRepresenter
import com.thoughtworks.go.apiv2.packages.representers.PackageUsageRepresenter
import com.thoughtworks.go.config.Authorization
import com.thoughtworks.go.config.BasicPipelineConfigs
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigs
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.*
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.materials.PackageDefinitionService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.Pair
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Collections.emptyList
import static java.util.Collections.emptyMap
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class PackagesControllerV2Test implements SecurityServiceTrait, ControllerTrait<PackagesControllerV2> {
  @Mock
  private EntityHashingService entityHashingService

  @Mock
  private PackageDefinitionService packageDefinitionService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PackagesControllerV2 createControllerInstance() {
    new PackagesControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, packageDefinitionService, goConfigService)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
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
      void 'should return packages as part of index call'() {
        List<PackageDefinition> packages = new ArrayList<>()
        def packageDefinition = new PackageDefinition('id', 'name', new Configuration(ConfigurationPropertyMother.create('key', 'value')))
        packageDefinition.setRepository(new PackageRepository('repoid', 'name', null, null))
        packages.push(packageDefinition)
        when(packageDefinitionService.getPackages()).thenReturn(packages)

        getWithApiHeader(controller.controllerBasePath())

        def expectedJSON = toObjectString({ PackageDefinitionsRepresenter.toJSON(it, packages) })

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(expectedJSON)
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
        getWithApiHeader(controller.controllerPath('foo'))
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
      void 'should return the requested package definition as part of show call'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageDefinition = PackageDefinitionMother.create('id', 'name', configuration, PackageRepositoryMother.create('repo-id'))

        when(packageDefinitionService.find('id')).thenReturn(packageDefinition)
        when(entityHashingService.md5ForEntity(packageDefinition)).thenReturn('etag')

        getWithApiHeader(controller.controllerPath('id'))

        def expectedJson = toObjectString({ PackageDefinitionRepresenter.toJSON(it, packageDefinition) })

        assertThatResponse()
          .isOk()
          .hasEtag('"etag"')
          .hasBodyWithJson(expectedJson)
      }

      @Test
      void 'should return 304 if the entity is not modified'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageDefinition = PackageDefinitionMother.create('id', 'name', configuration, PackageRepositoryMother.create('repo-id'))

        when(packageDefinitionService.find('id')).thenReturn(packageDefinition)
        when(entityHashingService.md5ForEntity(packageDefinition)).thenReturn('etag')

        getWithApiHeader(controller.controllerPath('id'), ['if-none-match': 'etag'])

        assertThatResponse()
          .isNotModified()
      }

      @Test
      void 'should return 404 if entity not found'() {
        getWithApiHeader(controller.controllerPath('id'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("id"))
      }

      @Test
      void 'should return 200 if the etag does not match'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageDefinition = PackageDefinitionMother.create('id', 'name', configuration, PackageRepositoryMother.create('repo-id'))

        when(packageDefinitionService.find('id')).thenReturn(packageDefinition)
        when(entityHashingService.md5ForEntity(packageDefinition)).thenReturn('etag')

        getWithApiHeader(controller.controllerPath('id'), ['if-none-match': 'another-etag'])

        assertThatResponse()
          .isOk()
          .hasEtag('"etag"')
          .hasBodyWithJsonObject(packageDefinition, PackageDefinitionRepresenter)
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
        postWithApiHeader(controller.controllerBasePath(), [])
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
      void 'should create the package definition as part of create call'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageDefinition = PackageDefinitionMother.create('id', 'name', configuration, PackageRepositoryMother.create('repo-id'))

        def json = toObjectString({ PackageDefinitionRepresenter.toJSON(it, packageDefinition) })

        when(entityHashingService.md5ForEntity(packageDefinition)).thenReturn('etag')
        when(packageDefinitionService.createPackage(eq(packageDefinition), eq('repo-id'), eq(currentUsername()), any(HttpLocalizedOperationResult.class))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Package definition was created successfully")
        })

        postWithApiHeader(controller.controllerBasePath(), json)

        assertThatResponse()
          .isOk()
          .hasEtag('"etag"')
          .hasBodyWithJson(json)
      }

      @Test
      void 'should error out if there are errors in parsing package definition config'() {
        def json = [
          id           : "id",
          name         : "name",
          auto_update  : true,
          configuration: [
            [
              "key"  : "PACKAGE_NAME",
              "value": "foo"
            ]
          ],
          package_repo : [
            name: "package-repo-name-1"
          ]
        ]

        def expectedResponse = [
          "message": "Validation error.",
          "data"   : [
            "name"         : "name",
            "id"           : "id",
            "auto_update"  : true,
            "package_repo" : [
              "id"  : null,
              "name": "package-repo-name-1"
            ],
            "configuration": [
              [
                "key"  : "PACKAGE_NAME",
                "value": "foo"
              ]
            ]
          ]
        ]

        when(packageDefinitionService.createPackage(any(), any(), any(), any())).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.unprocessableEntity("Validation error.")
        })

        postWithApiHeader(controller.controllerBasePath(), json)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonBody(expectedResponse)
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
        putWithApiHeader(controller.controllerPath('foo'), [])
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
      void 'should update the package definition as part of update call'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageDefinition = PackageDefinitionMother.create('id', 'name', configuration, PackageRepositoryMother.create('repo-id'))

        def updatedConfiguration = new Configuration(ConfigurationPropertyMother.create('updated-key', 'updated-value'))
        def updatedPackageDefinition = PackageDefinitionMother.create('id', 'updated-name', updatedConfiguration, PackageRepositoryMother.create('updated-repo-id'))

        def json = toObjectString({ PackageDefinitionRepresenter.toJSON(it, updatedPackageDefinition) })

        when(entityHashingService.md5ForEntity(packageDefinition)).thenReturn('etag')
        when(entityHashingService.md5ForEntity(updatedPackageDefinition)).thenReturn('updated-etag')
        when(packageDefinitionService.find('id')).thenReturn(packageDefinition)
        when(packageDefinitionService.updatePackage(eq('id'), eq(updatedPackageDefinition), eq('etag'), eq(currentUsername()), any(HttpLocalizedOperationResult.class))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Package definition was updated successfully")
        })

        putWithApiHeader(controller.controllerPath('id'), ['if-match': 'etag'], json)

        assertThatResponse()
          .isOk()
          .hasEtag('"updated-etag"')
          .hasBodyWithJson(json)
      }

      @Test
      void 'should not update the package definition if etag does not match'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageDefinition = PackageDefinitionMother.create('id', 'name', configuration, PackageRepositoryMother.create('repo-id'))

        def updatedConfiguration = new Configuration(ConfigurationPropertyMother.create('updated-key', 'updated-value'))
        def updatedPackageDefinition = PackageDefinitionMother.create('id', 'updated-name', updatedConfiguration, PackageRepositoryMother.create('updated-repo-id'))

        def json = toObjectString({ PackageDefinitionRepresenter.toJSON(it, updatedPackageDefinition) })

        when(entityHashingService.md5ForEntity(packageDefinition)).thenReturn('etag')
        when(entityHashingService.md5ForEntity(updatedPackageDefinition)).thenReturn('updated-etag')
        when(packageDefinitionService.find('id')).thenReturn(packageDefinition)

        putWithApiHeader(controller.controllerPath('id'), ['if-match': 'unknown-etag'], json)

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for packageDefinition 'id'. Please update your copy of the config with the changes and try again.")
      }


      @Test
      void 'should error out if there are errors in parsing package definition config'() {
        def expectedResponse = [
          "message": "Validation error.",
          "data"   : [
            "name"         : "updated-name",
            "id"           : "id",
            "auto_update"  : true,
            "package_repo" : [
              "id"  : "updated-repo-id",
              "name": "repo-updated-repo-id"
            ],
            "configuration": [
              [
                "key"  : "updated-key",
                "value": "updated-value"
              ]
            ]
          ]
        ]

        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageDefinition = PackageDefinitionMother.create('id', 'name', configuration, PackageRepositoryMother.create('repo-id'))

        def updatedConfiguration = new Configuration(ConfigurationPropertyMother.create('updated-key', 'updated-value'))
        def updatedPackageDefinition = PackageDefinitionMother.create('id', 'updated-name', updatedConfiguration, PackageRepositoryMother.create('updated-repo-id'))

        def json = toObjectString({ PackageDefinitionRepresenter.toJSON(it, updatedPackageDefinition) })

        when(packageDefinitionService.find('id')).thenReturn(packageDefinition)
        when(entityHashingService.md5ForEntity(packageDefinition)).thenReturn('etag')
        when(entityHashingService.md5ForEntity(updatedPackageDefinition)).thenReturn('updated-etag')
        when(packageDefinitionService.updatePackage(anyString(), any(), any(), any(), any())).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.unprocessableEntity("Validation error.")
        })

        putWithApiHeader(controller.controllerPath('id'), ['if-match': 'etag'], json)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonBody(expectedResponse)
      }

      @Test
      void 'should return 404 if entity not found'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageDefinition = PackageDefinitionMother.create('id', 'name', configuration, PackageRepositoryMother.create('repo-id'))

        def json = toObjectString({ PackageDefinitionRepresenter.toJSON(it, packageDefinition) })

        putWithApiHeader(controller.controllerPath('id'), ['if-match': 'etag'], json)

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("id"))
      }
    }
  }

  @Nested
  class Remove {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "remove"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath('foo'))
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
      void 'should remove the package definition as part of delete call'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageDefinition = PackageDefinitionMother.create('id', 'name', configuration, PackageRepositoryMother.create('repo-id'))

        when(packageDefinitionService.find('id')).thenReturn(packageDefinition)

        when(packageDefinitionService.deletePackage(eq(packageDefinition), eq(currentUsername()), any(HttpLocalizedOperationResult.class))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Package definition was deleted successfully")
        })

        deleteWithApiHeader(controller.controllerPath('id'))

        assertThatResponse()
          .isOk()
          .hasJsonMessage('Package definition was deleted successfully')
      }

      @Test
      void 'should return 404 if entity not found'() {
        deleteWithApiHeader(controller.controllerPath('id'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("id"))
      }
    }
  }

  @Nested
  class Usages {

    @BeforeEach
    void setUp() {
      loginAsGroupAdmin()
    }

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "usagesForPackage"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath() + "/pkg_id/usages")
      }
    }

    @Test
    void 'should return a list of pipelines which uses the specified package'() {
      def pipelineConfig = PipelineConfigMother.pipelineConfig("some-pipeline")
      Pair<PipelineConfig, PipelineConfigs> pair = new Pair<>(pipelineConfig, new BasicPipelineConfigs("pipeline-group", new Authorization(), pipelineConfig))
      ArrayList<Pair<PipelineConfig, PipelineConfigs>> pairs = new ArrayList<>()
      pairs.add(pair)

      def allUsages = new HashMap()
      allUsages.put("pkg_id", pairs)

      when(goConfigService.getPackageUsageInPipelines()).thenReturn(allUsages)

      getWithApiHeader(controller.controllerBasePath() + "/pkg_id/usages")

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(PackageUsageRepresenter.class, pairs)
    }

    @Test
    void 'should return a empty list if no usages found'() {
      when(goConfigService.getPackageUsageInPipelines()).thenReturn(emptyMap())

      getWithApiHeader(controller.controllerBasePath() + "/pkg_id/usages")

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(PackageUsageRepresenter.class, emptyList())
    }
  }
}
