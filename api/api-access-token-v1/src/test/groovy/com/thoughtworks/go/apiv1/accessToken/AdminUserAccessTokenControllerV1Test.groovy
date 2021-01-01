/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.api.mocks.MockHttpServletResponseAssert
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.accessToken.representers.AccessTokenRepresenter
import com.thoughtworks.go.apiv1.accessToken.representers.AccessTokensRepresenter
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException
import com.thoughtworks.go.domain.AccessToken
import com.thoughtworks.go.http.mocks.HttpRequestBuilder
import com.thoughtworks.go.http.mocks.MockHttpServletResponse
import com.thoughtworks.go.server.newsecurity.models.AccessTokenCredential
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils
import com.thoughtworks.go.server.service.AccessTokenFilter
import com.thoughtworks.go.server.service.AccessTokenService
import com.thoughtworks.go.spark.AdminUserOnlyIfSecurityEnabled
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.mocks.TestApplication
import com.thoughtworks.go.spark.mocks.TestSparkPreFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import spark.servlet.SparkFilter

import javax.servlet.FilterConfig
import java.util.stream.Stream

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv1.accessToken.representers.AccessTokenRepresenterTest.randomAccessToken
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class AdminUserAccessTokenControllerV1Test implements ControllerTrait<AdminUserAccessTokenControllerV1>, SecurityServiceTrait {
  @Mock
  AccessTokenService accessTokenService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  AdminUserAccessTokenControllerV1 createControllerInstance() {
    return new AdminUserAccessTokenControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), accessTokenService)
  }


  @Nested
  class APIAccessUsingAccessToken {
    private preFilter
    private authenticationToken

    @BeforeEach
    void setUp() {
      enableSecurity()
      def filterConfig = mock(FilterConfig.class)
      when(filterConfig.getInitParameter(SparkFilter.APPLICATION_CLASS_PARAM)).thenReturn(TestApplication.class.getName())
      preFilter = new TestSparkPreFilter(new TestApplication(getController()))
      this.preFilter.init(filterConfig)
      this.authenticationToken = new AuthenticationToken<>(null, new AccessTokenCredential(null), null, 0, null)
    }

    @ParameterizedTest
    @MethodSource("allRequests")
    void 'should return 409 when called with another access token'(String method, String path) {
      def request = new HttpRequestBuilder()
        .withMethod(method)
        .withPath(getController()
        .controllerPath(path))
        .build()
      SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request)
      def response = new MockHttpServletResponse()

      preFilter.doFilter(request, response, null)

      MockHttpServletResponseAssert.assertThat(response)
        .isForbidden()
        .hasJsonMessage("Unsupported operation: Accessing this API using access token is forbidden.")
    }

    static Stream<Arguments> allRequests() {
      return Stream.of(
        Arguments.of("GET", ""),
        Arguments.of("GET", "some-id"),
        Arguments.of("POST", "some-id/revoke")
      )
    }
  }

  @Nested
  class Show {
    def token = randomAccessToken()

    @Nested
    class Security implements SecurityTestTrait, AdminUserOnlyIfSecurityEnabled {
      @BeforeEach
      void setUp() {
        when(accessTokenService.find(token.id, currentUsernameString())).thenReturn(token)
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
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, controller.urlContext(), token) }))
      }

      @Test
      void 'should render not found when the specified access token does not exists'() {
        when(accessTokenService.find(eq(token.id), any(String.class))).thenThrow(new RecordNotFoundException(EntityType.AccessToken, token.id))

        getWithApiHeader(controller.controllerPath(token.id))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(EntityType.AccessToken.notFoundMessage(token.id))
          .hasContentType(controller.mimeType)
      }
    }

  }

  @Nested
  class Index {
    AccessToken.AccessTokenWithDisplayValue token = randomAccessToken()

    @Nested
    class Security implements SecurityTestTrait, AdminUserOnlyIfSecurityEnabled {
      @BeforeEach
      void setUp() {
        when(accessTokenService.findAllTokensForUser(currentUsernameString(), filter)).thenReturn([token])
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

      }

      @Test
      void 'should render active access tokens by default'() {
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn([token])

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokensRepresenter.toJSON(it, controller.urlContext(), [token]) }))
      }


      @Test
      void 'should render all the access tokens when filter is `all`'() {
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.all)).thenReturn([token])

        getWithApiHeader(controller.controllerPath([filter: 'all']))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokensRepresenter.toJSON(it, controller.urlContext(), [token]) }))
      }

      @Test
      void 'should render active the access tokens when filter is `active`'() {
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.active)).thenReturn([token])

        getWithApiHeader(controller.controllerPath([filter: 'active']))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokensRepresenter.toJSON(it, controller.urlContext(), [token]) }))
      }

      @Test
      void 'should render revoked the access tokens when filter is `revoked`'() {
        when(accessTokenService.findAllTokensForAllUsers(AccessTokenFilter.revoked)).thenReturn([token])

        getWithApiHeader(controller.controllerPath([filter: 'revoked']))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokensRepresenter.toJSON(it, controller.urlContext(), [token]) }))
      }

      @Test
      void 'should render 400 if bad filter is provided'() {
        getWithApiHeader(controller.controllerPath([filter: 'bad-value']))

        assertThatResponse()
          .isBadRequest()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Value `bad-value` is not allowed for query parameter named `filter`. Valid values are active, all, revoked.")
      }
    }
  }

  @Nested
  class Revoke {
    def token = randomAccessToken()

    @Nested
    class NormalSecurity implements SecurityTestTrait, AdminUserOnlyIfSecurityEnabled {
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
        when(accessTokenService.find(token.id, currentUsernameString())).thenReturn(token)
        when(accessTokenService.revokeAccessToken(token.id, currentUsernameString(), "blah")).thenReturn(token)

        postWithApiHeader(controller.controllerPath(token.id, 'revoke'), [revoke_cause: 'blah'])

        verify(accessTokenService).revokeAccessToken(token.id, currentUsernameString(), "blah")

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ AccessTokenRepresenter.toJSON(it, controller.urlContext(), token) }))
      }

      @Test
      void 'should show errors occurred while revoking a new access token'() {
        when(accessTokenService.revokeAccessToken(token.id, currentUsernameString(), null)).thenThrow(new UnprocessableEntityException("Boom!"))

        postWithApiHeader(controller.controllerPath(token.id, 'revoke'), [:])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Your request could not be processed. Boom!")
      }
    }
  }
}
