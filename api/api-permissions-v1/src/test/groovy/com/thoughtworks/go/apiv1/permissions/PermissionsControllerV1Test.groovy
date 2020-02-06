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

package com.thoughtworks.go.apiv1.permissions

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.server.service.PermissionsService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class PermissionsControllerV1Test implements SecurityServiceTrait, ControllerTrait<PermissionsControllerV1> {

  @Mock
  PermissionsService permissionsService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PermissionsControllerV1 createControllerInstance() {
    new PermissionsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), permissionsService)
  }

  @Nested
  class Index {

    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @Test
    void 'should represent user permissions'() {
      def permissions = [
        'environments': [
          'view'      : ['QA', 'UAT', 'PROD'],
          'administer': ['QA', 'UAT']
        ]
      ]

      when(permissionsService.getPermissionsForCurrentUser()).thenReturn(permissions)

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse()
        .isOk()
        .hasJsonBody([
        "_links"     : [
          "self": ["href": "http://test.host/go/api/auth/permissions"],
          "doc" : ["href": apiDocsUrl("#permissions")]
        ],
        "permissions": permissions
      ])
    }
  }
}
