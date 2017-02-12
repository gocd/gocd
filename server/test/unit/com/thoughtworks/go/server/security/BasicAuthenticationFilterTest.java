/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.i18n.Localizer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class BasicAuthenticationFilterTest {
    private String errorMessage;
    private MockHttpServletRequest httpRequest;
    private MockHttpServletResponse httpResponse;
    private BasicAuthenticationEntryPoint basicAuthenticationEntryPoint;
    private Localizer localizer;

    @Before
    public void setUp() throws Exception {
        errorMessage = "There was an error authenticating you. Please check the go server logs, or contact the go server administrator.";
        httpRequest = new MockHttpServletRequest();
        httpResponse = new MockHttpServletResponse();
        localizer = mock(Localizer.class);
        basicAuthenticationEntryPoint = mock(BasicAuthenticationEntryPoint.class);
        when(localizer.localize("INVALID_LDAP_ERROR")).thenReturn(errorMessage);
    }

    private AuthenticationManager getAuthenticationManager(final Boolean[] hadBasicMarkOnInsideAuthenticationManager) {
        return new AuthenticationManager() {
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                hadBasicMarkOnInsideAuthenticationManager[0] = BasicAuthenticationFilter.isProcessingBasicAuth();
                return new UsernamePasswordAuthenticationToken("school-principal", "u can be principal if you know this!");
            }
        };
    }

    @Test
    public void shouldConvey_itsBasicProcessingFilter() throws IOException, ServletException {
        final Boolean[] hadBasicMarkOnInsideAuthenticationManager = new Boolean[]{false};
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(getAuthenticationManager(hadBasicMarkOnInsideAuthenticationManager), basicAuthenticationEntryPoint, localizer);

        assertThat(BasicAuthenticationFilter.isProcessingBasicAuth(), is(false));

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("loser:boozer".getBytes()));
        filter.doFilterInternal(httpRequest, new MockHttpServletResponse(), new FilterChain() {
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {

            }
        });
        assertThat(BasicAuthenticationFilter.isProcessingBasicAuth(), is(false));
        assertThat(hadBasicMarkOnInsideAuthenticationManager[0], is(true));
    }

    @Test
    public void testShouldRender500WithHTMLTextBodyWithApiAcceptHeaderWithHTML() throws IOException {
        final Boolean[] hadBasicMarkOnInsideAuthenticationManager = new Boolean[]{false};
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(getAuthenticationManager(hadBasicMarkOnInsideAuthenticationManager), basicAuthenticationEntryPoint, localizer);

        httpRequest.addHeader("Accept", "text/html");
        SecurityContext context = SecurityContextHolder.getContext();

        filter.handleException(httpRequest, httpResponse, new Exception("some error"));
        verify(localizer).localize("INVALID_LDAP_ERROR");

        assertThat(((Exception) (httpRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION))).getMessage(), is(errorMessage));
        assertThat(httpRequest.getAttribute(SessionDenialAwareAuthenticationProcessingFilterEntryPoint.SESSION_DENIED).toString(), is("true"));
        assertThat(context.getAuthentication(), is(nullValue()));
        assertThat(httpResponse.getRedirectedUrl(), is("/go/auth/login?login_error=1"));
    }

    @Test
    public void testShouldRender500WithJSONBodyWithApiAcceptHeaderWithJSON() throws Exception {
        final Boolean[] hadBasicMarkOnInsideAuthenticationManager = new Boolean[]{false};
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(getAuthenticationManager(hadBasicMarkOnInsideAuthenticationManager), basicAuthenticationEntryPoint, localizer);

        httpRequest.addHeader("Accept", "application/vnd.go.cd.v1+json");

        filter.handleException(httpRequest, httpResponse, null);
        verify(localizer).localize("INVALID_LDAP_ERROR");
        assertEquals("application/vnd.go.cd.v1+json; charset=utf-8", httpResponse.getContentType());
        assertEquals("Basic realm=\"GoCD\"", httpResponse.getHeader("WWW-Authenticate"));
        assertEquals(500, httpResponse.getStatus());
        assertEquals(httpResponse.getContentAsString(), String.format("{\n \"message\": \"%s\"\n}\n", errorMessage));
    }

    @Test
    public void testShouldRender500WithXMLBodyWithApiAcceptHeaderWithXML() throws Exception {
        final Boolean[] hadBasicMarkOnInsideAuthenticationManager = new Boolean[]{false};
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(getAuthenticationManager(hadBasicMarkOnInsideAuthenticationManager), basicAuthenticationEntryPoint, localizer);

        httpRequest.addHeader("Accept", "application/XML");

        filter.handleException(httpRequest, httpResponse, null);
        verify(localizer).localize("INVALID_LDAP_ERROR");
        assertEquals("application/xml; charset=utf-8", httpResponse.getContentType());
        assertEquals("Basic realm=\"GoCD\"", httpResponse.getHeader("WWW-Authenticate"));
        assertEquals(500, httpResponse.getStatus());
        assertEquals(httpResponse.getContentAsString(), String.format("<message>%s</message>\n", errorMessage));
    }

    @Test
    public void testShouldRender500WithWithHTMLWithNoAcceptHeader() throws Exception {
        final Boolean[] hadBasicMarkOnInsideAuthenticationManager = new Boolean[]{false};
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(getAuthenticationManager(hadBasicMarkOnInsideAuthenticationManager), basicAuthenticationEntryPoint, localizer);

        filter.handleException(httpRequest, httpResponse, new Exception("foo"));
        verify(localizer).localize("INVALID_LDAP_ERROR");
        assertEquals(500, httpResponse.getStatus());
        assertEquals("foo", httpResponse.getErrorMessage());
    }

}
