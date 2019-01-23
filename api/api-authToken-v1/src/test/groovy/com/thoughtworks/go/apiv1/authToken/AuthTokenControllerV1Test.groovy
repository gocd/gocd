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

package com.thoughtworks.go.apiv1.authToken

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.AdminRole
import com.thoughtworks.go.config.AdminUser
import com.thoughtworks.go.config.AdminsConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.server.service.AuthTokenService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv1.authToken.representers.AuthTokenRepresenterTest.authTokenWithName
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AuthTokenControllerV1Test implements ControllerTrait<AuthTokenControllerV1>, SecurityServiceTrait {

  @Mock
  AuthTokenService authTokenService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  AuthTokenControllerV1 createControllerInstance() {
    return new AuthTokenControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), authTokenService)
  }

  @Nested
  class Show {
    def tokenName = "token1"
    def token = authTokenWithName(tokenName)

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @BeforeEach
      void setUp() {
        when(authTokenService.find(tokenName)).thenReturn(token)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "getAuthToken"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(tokenName))
      }
    }

    @Nested
    class AsAdmin {
      HttpLocalizedOperationResult result

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        this.result = new HttpLocalizedOperationResult()
      }

      @Test
      void 'should render the security admins config'() {
        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(config, AdminsConfigRepresenter)
      }

    }
  }


  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      AdminsConfig config

      @BeforeEach
      void setUp() {
        config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        when(adminsConfigService.systemAdmins()).thenReturn(config)
        when(entityHashingService.md5ForEntity(config)).thenReturn('cached-md5')
      }

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        sendRequest('put', controller.controllerPath(), [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-md5',
          'content-type': 'application/json'
        ], toObjectString({ AdminsConfigRepresenter.toJSON(it, this.config) }))
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
      void 'should update the system admins'() {
        AdminsConfig configInServer = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        AdminsConfig configFromRequest = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")),
          new AdminUser(new CaseInsensitiveString("new_admin")))

        when(adminsConfigService.systemAdmins()).thenReturn(configInServer)
        when(entityHashingService.md5ForEntity(configInServer)).thenReturn("cached-md5")

        putWithApiHeader(controller.controllerPath(), ['if-match': 'cached-md5'], toObjectString({
          AdminsConfigRepresenter.toJSON(it, configFromRequest)
        }))

        verify(adminsConfigService).update(any(), eq(configFromRequest), eq("cached-md5"), any(HttpLocalizedOperationResult.class));
        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(configFromRequest, AdminsConfigRepresenter)
      }

      @Test
      void 'should return a response with errors if update fails'() {
        AdminsConfig configInServer = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
        AdminsConfig configFromRequest = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")),
          new AdminUser(new CaseInsensitiveString("new_admin")))


        when(adminsConfigService.systemAdmins()).thenReturn(configInServer)
        when(entityHashingService.md5ForEntity(configInServer)).thenReturn("cached-md5")
        when(adminsConfigService.update(any(), eq(configFromRequest), eq("cached-md5"), any(HttpLocalizedOperationResult.class))).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        putWithApiHeader(controller.controllerPath(), ['if-match': 'cached-md5'], toObjectString({
          AdminsConfigRepresenter.toJSON(it, configFromRequest)
        }))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("validation failed")
      }

      @Test
      void 'should not update a stale system admins request'() {
        AdminsConfig systemAdminsRequest = new AdminsConfig(new AdminRole(new CaseInsensitiveString("admin")))
        AdminsConfig systemAdminsInServer = new AdminsConfig(new AdminRole(new CaseInsensitiveString("role1")))

        when(adminsConfigService.systemAdmins()).thenReturn(systemAdminsInServer)
        when(entityHashingService.md5ForEntity(systemAdminsInServer)).thenReturn('cached-md5')

        putWithApiHeader(controller.controllerPath(), ['if-match': 'some-string'], toObjectString({
          AdminsConfigRepresenter.toJSON(it, systemAdminsRequest)
        }))

        verify(adminsConfigService, Mockito.never()).update(any(), any(), any(), any())
        assertThatResponse().isPreconditionFailed()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Someone has modified the entity. Please update your copy with the changes and try again.")
      }
    }
  }
}
