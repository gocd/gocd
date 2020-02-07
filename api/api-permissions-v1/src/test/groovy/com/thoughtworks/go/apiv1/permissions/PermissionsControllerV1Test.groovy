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
import com.thoughtworks.go.server.service.permissions.PermissionsService
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

    @Nested
    class AsNormalUser {
      List permissibleEntities
      Map<String, Object> environmentPermission, configRepoPermission

      @BeforeEach
      void setUp() {
        permissibleEntities = ['environment', 'config_repo']
        environmentPermission = [
          'view'      : ['QA', 'UAT', 'PROD'],
          'administer': ['QA', 'UAT']
        ]
        configRepoPermission = [
          'view'      : ['repo1', 'repo2', 'repo3'],
          'administer': ['repo1']
        ]

        when(permissionsService.allEntitiesSupportsPermission()).thenReturn(permissibleEntities)
      }

      @Test
      void 'should not fail when no type param is specified'() {
        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
      }

      @Test
      void 'should fail when invalid type param is specified'() {
        getWithApiHeader(controller.controllerBasePath() + '?type=everything')

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. Invalid permission type 'everything'. It has to be one of 'environment, config_repo'.")
      }

      @Test
      void 'should fail when one of the specified type param is invalid'() {
        getWithApiHeader(controller.controllerBasePath() + '?type=environment,dashboard,config_repo')

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. Invalid permission type 'dashboard'. It has to be one of 'environment, config_repo'.")
      }

      @Test
      void 'should represent user permissions'() {
        def permissions = [
          'environment': environmentPermission,
          'config_repo': configRepoPermission
        ]

        when(permissionsService.getPermissions(permissibleEntities)).thenReturn(permissions)

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

      @Test
      void 'should represent user permissions for requested type'() {
        def permissions = ['environment': environmentPermission]
        when(permissionsService.getPermissions(['environment'])).thenReturn(permissions)

        getWithApiHeader(controller.controllerPath() + '?type=environment')

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

      @Test
      void 'should represent user permissions for requested multiple type'() {
        def permissions = [
          'environment': environmentPermission,
          'config_repo': configRepoPermission
        ]

        when(permissionsService.getPermissions(['environment', 'config_repo'])).thenReturn(permissions)

        getWithApiHeader(controller.controllerPath() + '?type=environment,config_repo')

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
}
