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
package com.thoughtworks.go.server.newsecurity.handlers;

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert;
import com.thoughtworks.go.server.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicAuthenticationWithChallengeFailureResponseHandlerTest {

    private SecurityService securityService;
    private MockHttpServletResponse response;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        securityService = mock(SecurityService.class);
        response = new MockHttpServletResponse();
    }

    @Nested
    class NotAnAjaxRequest {
        @BeforeEach
        void setUp() {
            request = HttpRequestBuilder.GET("/")
                    .build();

        }

        @Test
        void shouldNotChallengeWhenSecurityIsDisabled() throws IOException {
            when(securityService.isSecurityEnabled()).thenReturn(false);
            new BasicAuthenticationWithChallengeFailureResponseHandler(securityService)
                    .handle(request, response, SC_UNAUTHORIZED, "some-error-message");

            MockHttpServletResponseAssert.assertThat(response)
                    .isUnauthorized()
                    .hasHeader("Content-Type", "application/json;charset=utf-8")
                    .doesNotContainHeader("WWW-Authenticate")
                    .hasBody("{\n  \"message\": \"some-error-message\"\n}");
        }

        @Test
        void shouldShowChallengeWhenSecurityIsEnabled() throws IOException {
            when(securityService.isSecurityEnabled()).thenReturn(true);
            new BasicAuthenticationWithChallengeFailureResponseHandler(securityService)
                    .handle(request, response, SC_FORBIDDEN, "some-error-message");

            MockHttpServletResponseAssert.assertThat(response)
                    .isForbidden()
                    .hasHeader("Content-Type", "application/json;charset=utf-8")
                    .hasHeader("WWW-Authenticate", "Basic realm=\"GoCD\"")
                    .hasBody("{\n  \"message\": \"some-error-message\"\n}");
        }

    }

    @Nested
    class AjaxRequest {
        @Test
        void shouldNotChallenge() throws IOException {
            final MockHttpServletRequest request = HttpRequestBuilder.GET("/")
                    .usingAjax()
                    .build();

            final MockHttpServletResponse response = new MockHttpServletResponse();

            new BasicAuthenticationWithChallengeFailureResponseHandler(securityService)
                    .handle(request, response, SC_UNAUTHORIZED, "some-error-message");

            MockHttpServletResponseAssert.assertThat(response)
                    .isUnauthorized()
                    .hasHeader("Content-Type", "application/json;charset=utf-8")
                    .doesNotContainHeader("WWW-Authenticate")
                    .hasBody("{\n  \"message\": \"some-error-message\"\n}");
        }
    }
}
