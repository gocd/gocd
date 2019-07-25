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

package com.thoughtworks.go.apiv1.internalresources

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.server.service.GoConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalResourcesControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalResourcesControllerV1> {
  @Mock
  GoConfigService goConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalResourcesControllerV1 createControllerInstance() {
    new InternalResourcesControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), goConfigService)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'index'
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
        loginAsAdmin()
      }

      @Test
      void 'test should return resources fetched from go config service'() {
        when(goConfigService.getResourceList()).thenReturn(['firefox', 'jdk'])

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJson('["firefox", "jdk"]')
      }

      @Test
      void 'test should return empty resources list when no resources exists'() {
        when(goConfigService.getResourceList()).thenReturn([])

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasBody('[]')
      }
    }
  }

}
