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
package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.config.SecurityAuthConfig
import com.thoughtworks.go.http.mocks.HttpRequestBuilder
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple
import com.thoughtworks.go.server.service.AuthorizationExtensionCacheService
import com.thoughtworks.go.server.service.SecurityAuthConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import spark.ModelAndView
import spark.Request
import spark.Response

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AccessTokensControllerTest implements ControllerTrait<AccessTokensController>, SecurityServiceTrait {
  @Mock
  private AuthorizationExtensionCacheService authorizationExtensionCacheService
  @Mock
  private SecurityAuthConfigService securityAuthConfigService
  @Mock
  private Response response

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  AccessTokensController createControllerInstance() {
    return new AccessTokensController(new SPAAuthenticationHelper(securityService, goConfigService), authorizationExtensionCacheService, securityAuthConfigService, templateEngine)
  }

  @Nested
  class Security implements SecurityTestTrait, NormalUserSecurity {

    @Override
    String getControllerMethodUnderTest() {
      return "index"
    }

    @Override
    void makeHttpCall() {
      get(controller.controllerPath())
    }
  }

  @Test
  void "should add meta with supportsAccessToken true when plugin supports access token"() {
    def request = HttpRequestBuilder.GET("/").build()
    def user = new GoUserPrinciple("bob", "Bob")
    def authenticationToken = new AuthenticationToken<>(user, new UsernamePassword("bob", "some-pass"), "cd.go.ldap-plugin", 0, "ldap")
    SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request)
    when(authorizationExtensionCacheService.isValidUser(any() as String, any() as String, any() as SecurityAuthConfig))
      .thenReturn(true)
    when(securityAuthConfigService.findProfile(any() as String)).thenReturn(mock(SecurityAuthConfig.class))

    ModelAndView modalAndView = controller.index(new Request(request), response)
    Map<Object, Object> model = modalAndView.getModel() as Map<Object, Object>

    Assertions.assertThat(model.get("meta") as Map<String, Object>)
      .containsEntry("pluginId", "cd.go.ldap-plugin")
      .containsEntry("supportsAccessToken", true)
  }

  @Test
  void "should add meta with supportsAccessToken false when plugin supports access token"() {
    def request = HttpRequestBuilder.GET("/").build()
    def user = new GoUserPrinciple("bob", "Bob")
    def authenticationToken = new AuthenticationToken<>(user, new UsernamePassword("bob", "some-pass"), "cd.go.ldap-plugin", 0, "ldap")
    SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request)
    when(authorizationExtensionCacheService.isValidUser(any() as String, any() as String, any() as SecurityAuthConfig))
      .thenReturn(false)
    when(securityAuthConfigService.findProfile(any() as String)).thenReturn(mock(SecurityAuthConfig.class))

    ModelAndView modalAndView = controller.index(new Request(request), response)
    Map<Object, Object> model = modalAndView.getModel() as Map<Object, Object>

    Assertions.assertThat(model.get("meta") as Map<String, Object>)
      .containsEntry("pluginId", "cd.go.ldap-plugin")
      .containsEntry("supportsAccessToken", false)
  }
}

