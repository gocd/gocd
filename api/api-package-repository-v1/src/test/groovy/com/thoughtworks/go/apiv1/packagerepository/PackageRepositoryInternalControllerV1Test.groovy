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

package com.thoughtworks.go.apiv1.packagerepository

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper
import com.thoughtworks.go.apiv1.packagerepository.representers.PackageRepositoryRepresenter
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.domain.packagerepository.PackageRepository
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.materials.PackageRepositoryService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AnyGroupAdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.spark.Routes.SecurityAuthConfigAPI.VERIFY_CONNECTION
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class PackageRepositoryInternalControllerV1Test implements SecurityServiceTrait, ControllerTrait<PackageRepositoryInternalControllerV1> {
  @Mock
  private PackageRepositoryService packageRepositoryService

  @Mock
  private EntityHashingService entityHashingService


  @Override
  PackageRepositoryInternalControllerV1 createControllerInstance() {
    new PackageRepositoryInternalControllerV1(new ApiAuthorizationHelper(securityService, goConfigService), entityHashingService, packageRepositoryService)
  }

  @Nested
  class Security implements SecurityTestTrait, AnyGroupAdminUserSecurity {
    @Delegate SecurityServiceTrait s = PackageRepositoryInternalControllerV1Test.this
    @Delegate ControllerTrait<PackageRepositoryInternalControllerV1> c = PackageRepositoryInternalControllerV1Test.this

    @Override
    String getControllerMethodUnderTest() {
      return "verifyConnection"
    }

    @Override
    void makeHttpCall() {
      postWithApiHeader(controller.controllerPath(Routes.PackageRepository.VERIFY_CONNECTION), [:])
    }
  }

  @Nested
  class VerifyConnection {
    @BeforeEach
    void setUp() {
      loginAsAdmin()
    }

    @Test
    void 'should return 200 on successful verify connection'() {
      def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
      def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

      when(packageRepositoryService.checkConnection(any(PackageRepository.class), any())).then({
        InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.getArgument(1)
          result.setMessage("Successful Check Connection!")
      })

      def json = toObjectString({ PackageRepositoryRepresenter.toJSON(it, packageRepository) })

      postWithApiHeader(controller.controllerPath(VERIFY_CONNECTION), json)

      assertThatResponse()
        .isOk()
        .hasJsonMessage("Successful Check Connection!")
    }

    @Test
    void 'should return failure verify connection response'() {
      def configuration = new Configuration(ConfigurationPropertyMother.create('key', 'value'))
      def packageRepository = PackageRepositoryMother.create('repo-id', 'repo-name', 'plugin-id', '1.0.0', configuration)

      when(packageRepositoryService.checkConnection(any(PackageRepository.class), any())).then({
        InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.getArgument(1)
          result.badRequest("Failed Check Connection!")
      })

      def json = toObjectString({ PackageRepositoryRepresenter.toJSON(it, packageRepository) })

      postWithApiHeader(controller.controllerPath(VERIFY_CONNECTION), json)

      assertThatResponse()
        .isBadRequest()
        .hasJsonMessage("Failed Check Connection!")
    }
  }
}
