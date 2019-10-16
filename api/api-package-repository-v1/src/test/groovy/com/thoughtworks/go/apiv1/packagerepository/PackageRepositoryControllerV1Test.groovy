/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
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
    class Security implements SecurityTestTrait, AdminUserSecurity {

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

        when(entityHashingService.md5ForEntity(packageRepository)).thenReturn('etag')
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

        when(entityHashingService.md5ForEntity(packageRepository)).thenReturn('etag')
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

        when(entityHashingService.md5ForEntity(packageRepository)).thenReturn('etag')
        when(packageRepositoryService.getPackageRepository('repo-id')).thenReturn(packageRepository)

        getWithApiHeader(controller.controllerPath('repo-id'), ['if-none-match': 'another-etag'])

        assertThatResponse()
          .isOk()
          .hasEtag('"etag"')
          .hasBodyWithJsonObject(packageRepository, PackageRepositoryRepresenter)
      }
    }

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

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
}
