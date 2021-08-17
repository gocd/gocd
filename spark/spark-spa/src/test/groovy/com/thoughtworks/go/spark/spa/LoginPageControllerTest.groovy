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
package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.http.mocks.HttpRequestBuilder
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.TestingClock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class LoginPageControllerTest implements ControllerTrait<LoginPageController>, SecurityServiceTrait {
  private TestingClock clock

  @BeforeEach
  void setUp() {
    clock = new TestingClock()
  }

  @Override
  LoginPageController createControllerInstance() {
    return new LoginPageController(templateEngine, mock(LoginLogoutHelper.class), securityService, clock, systemEnvironment)
  }

  @Nested
  class RenderLoginPage {
    @Nested
    class SecurityDisabled {
      @BeforeEach
      void setUp() {
        when(securityService.isSecurityEnabled()).thenReturn(false)
      }

      @Test
      void 'should redirect to home page on form submit'() {
        get("/auth/login")

        assertThatResponse()
          .redirectsTo("/go/pipelines")
      }
    }

    @Nested
    class SecurityEnabled {
      @BeforeEach
      void setUp() {
        when(securityService.isSecurityEnabled()).thenReturn(true)
      }

      @Test
      void 'should redirect to homepage if user is already authenticated'() {
        loginAsUser()
        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true)
        when(systemEnvironment.getReAuthenticationTimeInterval()).thenReturn(15000L)

        get("/auth/login")

        assertThatResponse()
          .redirectsTo("/go/pipelines")
        assertThat(session).isSameAs(request.getSession(false))
      }

      @Test
      void 'should stay on login page if authentication token is expired'() {
        loginAsUser()
        when(systemEnvironment.isReAuthenticationEnabled()).thenReturn(true)
        when(systemEnvironment.getReAuthenticationTimeInterval()).thenReturn(5000L)

        GoUserPrinciple goUserPrinciple = SessionUtils.getCurrentUser()
        AuthenticationToken<UsernamePassword> usernamePasswordAuthenticationToken = new AuthenticationToken<>(goUserPrinciple, new UsernamePassword("bob@example.com", "p@ssw0rd"), null, clock.currentTimeMillis(), null)
        clock.addMillis(10000)

        SessionUtils.setAuthenticationTokenWithoutRecreatingSession(usernamePasswordAuthenticationToken, HttpRequestBuilder.GET("/").withSession(session).build())

        get("/auth/login")

        assertThatResponse()
          .isOk()
          .hasBodyContaining('"viewTitle":"Login"')
      }

      @Test
      void 'should render login page'() {
        loginAsAnonymous()
        get("/auth/login")

        assertThatResponse()
          .isOk()
          .hasBodyContaining('"viewTitle":"Login"')
      }
    }
  }
}
