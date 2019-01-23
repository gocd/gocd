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
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.apiv1.authToken.representers.AuthTokenRepresenter
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.AuthTokenService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.AuthTokenMother.authTokenWithName
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AuthTokenControllerV1Test implements ControllerTrait<AuthTokenControllerV1>, SecurityServiceTrait {
  def username = currentUsername()

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
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @BeforeEach
      void setUp() {
        when(authTokenService.find(eq(tokenName), any(Username.class))).thenReturn(token)
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
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        when(authTokenService.find(eq(tokenName), any(Username.class))).thenReturn(token)
      }

      @Test
      void 'should render the auth token'() {
        getWithApiHeader(controller.controllerPath(tokenName))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AuthTokenRepresenter.toJSON(it, token, false) }))
      }

      @Test
      void 'should render not found when the specified auth token does not exists'() {
        when(authTokenService.find(eq(tokenName), any(Username.class))).thenReturn(null)

        getWithApiHeader(controller.controllerPath(tokenName))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(HaltApiMessages.notFoundMessage())
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Nested
  class Create {
    def tokenName = "token1"
    def token = authTokenWithName(tokenName)

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "createAuthToken"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), [:])
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        when(authTokenService.create(eq(tokenName), eq(token.description), any(Username.class), any(HttpLocalizedOperationResult.class))).thenReturn(token)
      }

      @Test
      void 'should create a new auth token'() {
        def requestBody = [
          name       : token.name,
          description: token.description
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AuthTokenRepresenter.toJSON(it, token, true) }))
      }

      @Test
      void 'should create a new auth token without providing token description'() {
        token.setDescription(null)
        when(authTokenService.create(eq(tokenName), eq(token.description), any(Username.class), any(HttpLocalizedOperationResult.class))).thenReturn(token)

        def requestBody = [
          name: token.name
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AuthTokenRepresenter.toJSON(it, token, true) }))
      }

      @Test
      void 'should fail to create a new auth token when no token name is specified'() {
        def requestBody = [
          no_name: token.name
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isUnprocessableEntity()
      }

      @Test
      void 'should show errors occurred while creating a new auth token'() {
        when(authTokenService.create(eq(tokenName), eq(token.description), any(Username.class), any(HttpLocalizedOperationResult.class))).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("Boom!")
        })

        def requestBody = [
          name       : token.name,
          description: token.description
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Boom!")
      }
    }
  }
}
