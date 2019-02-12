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
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException
import com.thoughtworks.go.domain.AccessToken
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension
import com.thoughtworks.go.server.service.AccessTokenService
import com.thoughtworks.go.server.service.SecurityAuthConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserOnlyIfSecurityEnabled
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv1.accessToken.representers.AccessTokenRepresenterTest.randomAccessToken
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AccessTokenControllerV1Test implements ControllerTrait<AccessTokenControllerV1>, SecurityServiceTrait {
  @Mock
  AccessTokenService accessTokenService
  @Mock
  SecurityAuthConfigService authConfigService
  @Mock
  AuthorizationExtension extension

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  AccessTokenControllerV1 createControllerInstance() {
    return new AccessTokenControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), accessTokenService, authConfigService, extension)
  }

  @Nested
  class Show {
    def token = randomAccessToken()

    @Nested
    class Security implements SecurityTestTrait, NormalUserOnlyIfSecurityEnabled {
      @BeforeEach
      void setUp() {
        when(accessTokenService.find(token.id, currentUserLoginName().toString())).thenReturn(token)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "getAccessToken"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(token.id))
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        when(accessTokenService.find(eq(token.id), any(String.class))).thenReturn(token)
      }

      @Test
      void 'should render the access token'() {
        getWithApiHeader(controller.controllerPath(token.id))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, token) }))
      }

      @Test
      void 'should render not found when the specified access token does not exists'() {
        when(accessTokenService.find(eq(token.id), any(String.class))).thenThrow(new RecordNotFoundException("blah!"))

        getWithApiHeader(controller.controllerPath(token.id))

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

    @Nested
    class Security implements SecurityTestTrait, NormalUserOnlyIfSecurityEnabled {
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
    class AsAuthenticatedUser {
      def token = randomAccessToken()

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsUser()

        when(accessTokenService.create(token.description, currentUserLoginName().toString(), authConfigId)).thenReturn(token)
      }

      @Test
      void 'should create a new access token'() {
        def requestBody = [
          description: token.description
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, token) }))
      }

      @Test
      void 'should create a new access token without providing token description'() {
        token.description = null
        when(accessTokenService.create(eq(token.description), eq(currentUserLoginName().toString()), eq(authConfigId))).thenReturn(token)

        def requestBody = [
          description: null
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, token) }))
      }

      @Test
      void 'should show errors occurred while creating a new access token'() {
        when(accessTokenService.create(token.description, currentUserLoginName().toString(), authConfigId)).thenThrow(new UnprocessableEntityException("Boom!"))

        def requestBody = [
          description: token.description
        ]

        postWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. Boom!")
      }
    }
  }

  @Nested
  class Index {
    AccessToken.AccessTokenWithDisplayValue token = randomAccessToken()

    @Nested
    class Security implements SecurityTestTrait, NormalUserOnlyIfSecurityEnabled {
      @BeforeEach
      void setUp() {
        when(accessTokenService.findAllTokensForUser(currentUserLoginName().toString())).thenReturn([token])
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

        when(accessTokenService.findAllTokensForUser(currentUserLoginName().toString())).thenReturn([token])
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
    def token = randomAccessToken()

    @Nested
    class NormalSecurity implements SecurityTestTrait, NormalUserOnlyIfSecurityEnabled {
      @Override
      String getControllerMethodUnderTest() {
        return "revokeAccessToken"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(token.id, 'revoke'), [:])
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
        when(accessTokenService.find(token.id, currentUserLoginName().toString())).thenReturn(token)
        when(accessTokenService.revokeAccessToken(token.id, currentUserLoginName().toString(), "blah")).thenReturn(token)

        postWithApiHeader(controller.controllerPath(token.id, 'revoke'), [cause: 'blah'])

        verify(accessTokenService).revokeAccessToken(token.id, currentUserLoginName().toString(), "blah")

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, token) }))
      }

      @Test
      void 'should show errors occurred while revoking a new access token'() {
        when(accessTokenService.revokeAccessToken(token.id, currentUserLoginName().toString(), null)).thenThrow(new UnprocessableEntityException("Boom!"))

        postWithApiHeader(controller.controllerPath(token.id, 'revoke'), [:])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. Boom!")
      }
    }
  }
}
