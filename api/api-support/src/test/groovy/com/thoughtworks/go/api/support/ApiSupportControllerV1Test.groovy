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
package com.thoughtworks.go.api.support

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper
import com.thoughtworks.go.server.service.support.ServerStatusService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class ApiSupportControllerV1Test implements SecurityServiceTrait, ControllerTrait<ApiSupportController> {
  @Mock
  private ServerStatusService serverStatusService


  @Override
  ApiSupportController createControllerInstance() {
    new ApiSupportController(new ApiAuthorizationHelper(securityService, goConfigService), serverStatusService)
  }

  @Nested
  class Show {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Delegate SecurityServiceTrait s = ApiSupportControllerV1Test.this
      @Delegate ControllerTrait<ApiSupportController> c = ApiSupportControllerV1Test.this

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }

      @Test
      void 'should return support information'() {
        loginAsAdmin()
        def res = [foo: "bar"]
        when(serverStatusService.asJsonCompatibleMap()).thenReturn(res)

        get(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasJsonBody(res)
          .hasContentType("application/json")
      }

      @Test
      void 'should return error response'() {
        loginAsAdmin()
        def message = "Failed to get information"
        when(serverStatusService.asJsonCompatibleMap()).thenThrow(new RuntimeException(message))

        get(controller.controllerPath())

        assertThatResponse()
          .isInternalServerError()
          .hasJsonAttribute("error", message)
          .hasContentType("application/json")
      }
    }
  }

  @Nested
  class ProcessList {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Delegate SecurityServiceTrait s = ApiSupportControllerV1Test.this
      @Delegate ControllerTrait<ApiSupportController> c = ApiSupportControllerV1Test.this

      @Override
      String getControllerMethodUnderTest() {
        return "processList"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(Routes.Support.PROCESS_LIST))
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }

      @Test
      void 'should return process list json'() {
        get(controller.controllerPath(Routes.Support.PROCESS_LIST))

        assertThatResponse()
          .isOk()
          .hasContentType("application/json")
      }
    }
  }
}
