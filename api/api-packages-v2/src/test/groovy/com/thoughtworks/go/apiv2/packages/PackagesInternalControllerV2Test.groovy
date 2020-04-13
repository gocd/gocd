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
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.config.ConfigurationKey
import com.thoughtworks.go.domain.config.ConfigurationProperty
import com.thoughtworks.go.domain.config.ConfigurationValue
import com.thoughtworks.go.domain.packagerepository.PackageDefinition
import com.thoughtworks.go.domain.packagerepository.PackageRepository
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.materials.PackageDefinitionService
import com.thoughtworks.go.server.service.materials.PackageRepositoryService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.spark.Routes.SecurityAuthConfigAPI.VERIFY_CONNECTION
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class PackagesInternalControllerV2Test implements SecurityServiceTrait, ControllerTrait<PackagesInternalControllerV2> {
  @Mock
  private PackageDefinitionService packageDefinitionService

  @Mock
  private PackageRepositoryService packageRepositoryService

  @Mock
  private EntityHashingService entityHashingService

  def repo = new PackageRepository('package-repo-id-1', 'package-repo-name-1', null, null)

  @BeforeEach
  void setUp() {
    initMocks(this)
    when(packageRepositoryService.getPackageRepository(any())).thenReturn(repo)
  }

  @Override
  PackagesInternalControllerV2 createControllerInstance() {
    new PackagesInternalControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, packageDefinitionService, packageRepositoryService)
  }

  @Nested
  class Security implements SecurityTestTrait, GroupAdminUserSecurity {

    @Override
    String getControllerMethodUnderTest() {
      return "verifyConnection"
    }

    @Override
    void makeHttpCall() {
      postWithApiHeader(controller.controllerPath(Routes.Packages.VERIFY_CONNECTION), [:])
    }
  }

  @Nested
  class VerifyConnection {
    @BeforeEach
    void setUp() {
      enableSecurity()
      loginAsAdmin()
    }

    @Test
    void 'should return 200 on successful verify connection'() {
      def configurationProperties = new Configuration(new ConfigurationProperty(new ConfigurationKey('PACKAGE_NAME'), new ConfigurationValue('foo')))
      def packageDefinition = new PackageDefinition('package-id-1', 'package-1', configurationProperties)
      packageDefinition.setRepository(repo)

      when(packageDefinitionService.checkConnection(any(PackageDefinition.class), any(HttpLocalizedOperationResult.class))).then({
        InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
          result.setMessage("Successful Check Connection!")
      })

      def json = toObjectString({ PackageDefinitionRepresenter.toJSON(it, packageDefinition) })

      postWithApiHeader(controller.controllerPath(VERIFY_CONNECTION), json)

      assertThatResponse()
        .isOk()
        .hasJsonMessage("Successful Check Connection!")
    }

    @Test
    void 'should return failure verify connection response'() {
      def configurationProperties = new Configuration(new ConfigurationProperty(new ConfigurationKey('PACKAGE_NAME'), new ConfigurationValue('foo')))
      def packageDefinition = new PackageDefinition('package-id-1', 'package-1', configurationProperties)
      packageDefinition.setRepository(new PackageRepository('package-repo-id-1', 'package-repo-name-1', null, null))

      when(packageDefinitionService.checkConnection(any(PackageDefinition.class), any(HttpLocalizedOperationResult.class))).then({
        InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
          result.badRequest("Failed Check Connection!")
      })

      def json = toObjectString({ PackageDefinitionRepresenter.toJSON(it, packageDefinition) })

      postWithApiHeader(controller.controllerPath(VERIFY_CONNECTION), json)

      assertThatResponse()
        .isBadRequest()
        .hasJsonMessage("Failed Check Connection!")
    }
  }
}
