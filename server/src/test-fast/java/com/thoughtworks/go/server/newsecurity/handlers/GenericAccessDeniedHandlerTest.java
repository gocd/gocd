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
import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.SecurityService;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenericAccessDeniedHandlerTest {

    @Test
    void shouldRedirectToLoginPageWhenSecurityIsEnabledAndUserIsNotAuthenticated() throws IOException {
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/foo").build();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityService securityService = mock(SecurityService.class);
        SessionUtilsHelper.loginAsAnonymous(request);

        when(securityService.isSecurityEnabled()).thenReturn(true);

        new GenericAccessDeniedHandler(securityService)
                .handle(request, response, SC_FORBIDDEN, "Some error");

        MockHttpServletResponseAssert.assertThat(response).redirectsTo("/go/auth/login");
    }

    @Test
    void shouldAccessDeniedWhenUserIsAuthenticated() throws IOException {
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/admin/").build();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityService securityService = mock(SecurityService.class);
        SessionUtilsHelper.loginAsRandomUser(request, GoAuthority.ROLE_USER.asAuthority());

        when(securityService.isSecurityEnabled()).thenReturn(true);

        new GenericAccessDeniedHandler(securityService)
                .handle(request, response, SC_FORBIDDEN, "Some error");

        MockHttpServletResponseAssert.assertThat(response)
                .isForbidden();
    }
}
