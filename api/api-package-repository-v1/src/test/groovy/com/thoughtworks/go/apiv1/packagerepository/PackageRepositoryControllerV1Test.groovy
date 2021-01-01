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
package com.thoughtworks.go.apiv1.packagerepository

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.packagerepository.representers.PackageRepositoriesRepresenter
import com.thoughtworks.go.apiv1.packagerepository.representers.PackageRepositoryRepresenter
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.domain.packagerepository.PackageRepositories
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.materials.PackageRepositoryService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.api.util.HaltApiMessages.etagDoesNotMatch
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class PackageRepositoryControllerV1Test implements SecurityServiceTrait, ControllerTrait<PackageRepositoryControllerV1> {

  @Mock
  private PackageRepositoryService packageRepositoryService

  @Mock
  private EntityHashingService entityHashingService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PackageRepositoryControllerV1 createControllerInstance() {
    new PackageRepositoryControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, packageRepositoryService)
  }

  @Nested
  class Index {

    @BeforeEach
    void setUp() {
      enableSecurity()
      loginAsAdmin()
    }

    @Test
    void 'should return package repositories as part of index call'() {
      def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
      def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)
      def packageRepositories = new PackageRepositories(packageRepository)

      when(packageRepositoryService.getPackageRepositories()).thenReturn(packageRepositories)

      getWithApiHeader(controller.controllerBasePath())

      def expectedJSON = toObjectString({ PackageRepositoriesRepresenter.toJSON(it, packageRepositories) })

      assertThatResponse()
        .isOk()
        .hasBodyWithJson(expectedJSON)
    }

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
  }

  @Nested
  class Show {

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should return requested package repository as part of show call'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

        when(entityHashingService.hashForEntity(packageRepository)).thenReturn('etag')
        when(packageRepositoryService.getPackageRepository('repo-id')).thenReturn(packageRepository)

        getWithApiHeader(controller.controllerPath('repo-id'))

        def expectedJSON = toObjectString({ PackageRepositoryRepresenter.toJSON(it, packageRepository) })

        assertThatResponse()
          .isOk()
          .hasEtag('"etag"')
          .hasBodyWithJson(expectedJSON)
      }

      @Test
      void 'should return 304 if the requested entity is not modified'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

        when(entityHashingService.hashForEntity(packageRepository)).thenReturn('etag')
        when(packageRepositoryService.getPackageRepository('repo-id')).thenReturn(packageRepository)

        getWithApiHeader(controller.controllerPath('repo-id'), ['if-none-match': 'etag'])

        assertThatResponse()
          .isNotModified()
      }

      @Test
      void 'should return 404 if requested entity does not exist'() {
        getWithApiHeader(controller.controllerPath('unknown-repo-id'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Package repository with id 'unknown-repo-id' was not found!")
      }

      @Test
      void 'should return 200 if the etag does not match'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

        when(entityHashingService.hashForEntity(packageRepository)).thenReturn('etag')
        when(packageRepositoryService.getPackageRepository('repo-id')).thenReturn(packageRepository)

        getWithApiHeader(controller.controllerPath('repo-id'), ['if-none-match': 'another-etag'])

        assertThatResponse()
          .isOk()
          .hasEtag('"etag"')
          .hasBodyWithJsonObject(packageRepository, PackageRepositoryRepresenter)
      }
    }

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('repo-id'))
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should create the requested package repository as part of create call'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

        def json = toObjectString({ PackageRepositoryRepresenter.toJSON(it, packageRepository) })

        when(entityHashingService.hashForEntity(packageRepository)).thenReturn('etag')
        when(packageRepositoryService.createPackageRepository(eq(packageRepository), eq(currentUsername()), any(HttpLocalizedOperationResult.class))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Package repository was created successfully")
        })

        postWithApiHeader(controller.controllerBasePath(), json)

        assertThatResponse()
          .isOk()
          .hasEtag('"etag"')
          .hasBodyWithJson(json)
      }

      @Test
      void 'should error out if there are errors in parsing the repository config'() {

        def json = [
          "repo_id"        : "repo-id",
          "name"           : "testing-post",
          "plugin_metadata": [
            "id"     : "artifactory-pkg",
            "version": "1"
          ],
          "configuration"  : [
            [
              "key"  : "base_url",
              "value": ""
            ]
          ]
        ]

        def expectedResponse = [
          "message": "Validation error.",
          "data"   : [
            "repo_id"        : "repo-id",
            "name"           : "testing-post",
            "plugin_metadata": [
              "id"     : "artifactory-pkg",
              "version": "1"
            ],
            "configuration"  : [
              [
                "key": "base_url"
              ]
            ],
            "_embedded"      : [
              "packages": []
            ]
          ]
        ]

        when(packageRepositoryService.createPackageRepository(any(), any(), any())).then({
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
  }

  @Nested
  class Update {

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should update the requested package repository as part of update call'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

        def updatedConfiguration = new Configuration(ConfigurationPropertyMother.create('updated-key', 'updated-value'))
        def updatedPackageRepository = PackageRepositoryMother.create('repo-id', 'updated-repo-name', 'updated-plugin-id', '2.0.0', updatedConfiguration)

        def json = toObjectString({ PackageRepositoryRepresenter.toJSON(it, updatedPackageRepository) })

        when(packageRepositoryService.getPackageRepository('repo-id')).thenReturn(packageRepository)
        when(entityHashingService.hashForEntity(packageRepository)).thenReturn('etag')
        when(entityHashingService.hashForEntity(updatedPackageRepository)).thenReturn('updated-etag')
        when(packageRepositoryService.updatePackageRepository(eq(updatedPackageRepository), eq(currentUsername()), anyString(), any(), anyString())).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments[3]
            result.setMessage("Package repository was created successfully")
        })

        putWithApiHeader(controller.controllerPath('repo-id'), ['if-match': 'etag'], json)

        assertThatResponse()
          .isOk()
          .hasEtag('"updated-etag"')
          .hasBodyWithJson(json)
      }

      @Test
      void 'should not update requested package repository if etag does not match'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

        def updatedConfiguration = new Configuration(ConfigurationPropertyMother.create('updated-key', 'updated-value'))
        def updatedPackageRepository = PackageRepositoryMother.create('repo-id', 'updated-repo-name', 'updated-plugin-id', '2.0.0', updatedConfiguration)

        def json = toObjectString({ PackageRepositoryRepresenter.toJSON(it, updatedPackageRepository) })

        when(packageRepositoryService.getPackageRepository('repo-id')).thenReturn(packageRepository)
        when(entityHashingService.hashForEntity(packageRepository)).thenReturn('etag')
        when(entityHashingService.hashForEntity(updatedPackageRepository)).thenReturn('updated-etag')

        putWithApiHeader(controller.controllerPath('repo-id'), ['if-match': 'invalid-etag'], json)

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage(etagDoesNotMatch("package repository", "repo-id"))
      }

      @Test
      void 'should error out if there are errors in parsing the repository config'() {

        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

        def updatedConfiguration = new Configuration(ConfigurationPropertyMother.create('updated-key', ''))
        def updatedPackageRepository = PackageRepositoryMother.create('repo-id', 'updated-repo-name', 'updated-plugin-id', '2.0.0', updatedConfiguration)

        def expectedResponse = [
          "message": "Validation error.",
          "data"   : [
            "repo_id"        : "repo-id",
            "name"           : "updated-repo-name",
            "plugin_metadata": [
              "id"     : "updated-plugin-id",
              "version": "2.0.0"
            ],
            "configuration"  : [
              [
                "key": "updated-key"
              ]
            ],
            "_embedded"      : [
              "packages": []
            ]
          ]
        ]

        when(packageRepositoryService.getPackageRepository('repo-id')).thenReturn(packageRepository)
        when(entityHashingService.hashForEntity(packageRepository)).thenReturn('etag')
        when(entityHashingService.hashForEntity(updatedPackageRepository)).thenReturn('updated-etag')
        when(packageRepositoryService.updatePackageRepository(any(), any(), any(), any(), any())).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments[3]
            result.unprocessableEntity("Validation error.")
        })

        def json = toObjectString({ PackageRepositoryRepresenter.toJSON(it, updatedPackageRepository) })

        putWithApiHeader(controller.controllerPath('repo-id'), ['if-match': 'etag'], json)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonBody(expectedResponse)
      }
    }

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath('repo_id'), [])
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
        deleteWithApiHeader(controller.controllerPath('repo_id'))
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
      void 'should remove the package repository as part of delete call'() {
        def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
        def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

        when(packageRepositoryService.getPackageRepository('repo-id')).thenReturn(packageRepository)

        when(packageRepositoryService.deleteRepository(eq(currentUsername()), eq(packageRepository), any(HttpLocalizedOperationResult.class))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Package repository was deleted successfully")
        })

        deleteWithApiHeader(controller.controllerPath('repo-id'))

        assertThatResponse()
          .isOk()
          .hasJsonMessage('Package repository was deleted successfully')
      }

      @Test
      void 'should return 404 if entity not found'() {
        deleteWithApiHeader(controller.controllerPath('unknown-id'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("unknown-id"))
      }
    }
  }
}
