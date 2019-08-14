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

package com.thoughtworks.go.apiv1.internalenvironments

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalEnvironmentsControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalEnvironmentsControllerV1> {

  @Mock
  EnvironmentConfigService environmentConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalEnvironmentsControllerV1 createControllerInstance() {
    new InternalEnvironmentsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), environmentConfigService)
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
      void 'test should return environments fetched from environments config service'() {
        List<CaseInsensitiveString> envNames = new ArrayList<>()
        envNames.add(new CaseInsensitiveString("environment1"))
        envNames.add(new CaseInsensitiveString("environment2"))

        when(environmentConfigService.environmentNames()).thenReturn(envNames)

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJson('["environment1","environment2"]')
      }

      @Test
      void 'test should return empty environments list when no environment exists'() {
        when(environmentConfigService.environmentNames()).thenReturn([])

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasBody('[]')
      }
    }
  }
}