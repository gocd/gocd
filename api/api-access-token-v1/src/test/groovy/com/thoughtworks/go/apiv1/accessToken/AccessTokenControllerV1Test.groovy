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

package com.thoughtworks.go.apiv1.accessToken

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.apiv1.accessToken.representers.AccessTokenRepresenter
import com.thoughtworks.go.apiv1.accessToken.representers.AccessTokensRepresenter
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.AccessTokenService
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
import static com.thoughtworks.go.helper.AccessTokenMother.accessTokenWithName
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AccessTokenControllerV1Test implements ControllerTrait<AccessTokenControllerV1>, SecurityServiceTrait {
  @Mock
  AccessTokenService accessTokenService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  AccessTokenControllerV1 createControllerInstance() {
    return new AccessTokenControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), accessTokenService)
  }

  @Nested
  class Show {
    def tokenName = "token1"
    def token = accessTokenWithName(tokenName)

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @BeforeEach
      void setUp() {
        when(accessTokenService.find(eq(tokenName), any(Username.class))).thenReturn(token)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "getAccessToken"
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

        when(accessTokenService.find(eq(tokenName), any(String.class))).thenReturn(token)
      }

      @Test
      void 'should render the access token'() {
        getWithApiHeader(controller.controllerPath(tokenName))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, token, false) }))
      }

      @Test
      void 'should render not found when the specified access token does not exists'() {
        when(accessTokenService.find(eq(tokenName), any(String.class))).thenReturn(null)

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
    def authConfigId = 'authConfigId'
    def tokenName = "token1"
    def token = accessTokenWithName(tokenName)

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "createAccessToken"
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

        when(accessTokenService.create(eq(tokenName), eq(token.description), any(Username.class), eq(authConfigId), any(HttpLocalizedOperationResult.class))).thenReturn(token)
      }

      @Test
      void 'should create a new access token'() {
        def requestBody = [
          name       : token.name,
          description: token.description
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, token, true) }))
      }

      @Test
      void 'should create a new access token without providing token description'() {
        token.setDescription(null)
        when(accessTokenService.create(eq(tokenName), eq(token.description), any(Username.class), eq(authConfigId), any(HttpLocalizedOperationResult.class))).thenReturn(token)

        def requestBody = [
          name: token.name
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, token, true) }))
      }

      @Test
      void 'should fail to create a new access token when no token name is specified'() {
        def requestBody = [
          no_name: token.name
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isUnprocessableEntity()
      }

      @Test
      void 'should show errors occurred while creating a new access token'() {
        when(accessTokenService.create(eq(tokenName), eq(token.description), any(Username.class), eq(authConfigId), any(HttpLocalizedOperationResult.class))).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last() as HttpLocalizedOperationResult
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

  @Nested
  class Index {
    def token = accessTokenWithName("token1")

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @BeforeEach
      void setUp() {
        when(accessTokenService.findAllTokensForUser(any(Username.class))).thenReturn([token])
      }

      @Override
      String getControllerMethodUnderTest() {
        return "getAllAccessTokens"
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
        enableSecurity()
        loginAsAdmin()

        when(accessTokenService.findAllTokensForUser(any(Username.class))).thenReturn([token])
      }

      @Test
      void 'should render all the access tokens'() {
        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokensRepresenter.toJSON(it, [token]) }))
      }
    }
  }

  @Nested
  class Revoke {
    def tokenName = "token1"
    def userName = "bob"
    def token = accessTokenWithName(tokenName)

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "revokeAccessToken"
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath(userName, tokenName, 'revoke'), [:])
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
      void 'should revoke the access token'() {
        when(accessTokenService.find(tokenName, userName)).thenReturn(token)
        patchWithApiHeader(controller.controllerPath(userName, tokenName, 'revoke'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, token, true) }))
      }

      @Test
      void 'should show errors occurred while revoking a new access token'() {
        when(accessTokenService.revokeAccessToken(eq(tokenName) as String, eq(userName) as String, any() as HttpLocalizedOperationResult)).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last() as HttpLocalizedOperationResult
          result.unprocessableEntity("Boom!")
        })

        patchWithApiHeader(controller.controllerPath(userName, tokenName, 'revoke'), [:])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Boom!")
      }
    }
  }
}
