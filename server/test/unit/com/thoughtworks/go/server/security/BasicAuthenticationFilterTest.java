/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.i18n.Localizer;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.ui.AbstractProcessingFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BasicAuthenticationFilterTest {
    private String errorMessage;
    private MockHttpServletRequest httpRequest;
    private MockHttpServletResponse httpResponse;
    private BasicAuthenticationFilter filter;
    private Localizer localizer;

    @Before
    public void setUp() throws Exception {
        errorMessage = "There was an error authenticating you. Please check the go server logs, or contact the go server administrator.";
        httpRequest = new MockHttpServletRequest();
        httpResponse = new MockHttpServletResponse();
        localizer = mock(Localizer.class);
        filter = new BasicAuthenticationFilter(localizer);
        when(localizer.localize("INVALID_LDAP_ERROR")).thenReturn(errorMessage);
    }

    @Test
    public void shouldConvey_itsBasicProcessingFilter() throws IOException, ServletException {
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(localizer);
        final Boolean[] hadBasicMarkOnInsideAuthenticationManager = new Boolean[]{false};

        filter.setAuthenticationManager(new AuthenticationManager() {
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                hadBasicMarkOnInsideAuthenticationManager[0] = BasicAuthenticationFilter.isProcessingBasicAuth();
                return new UsernamePasswordAuthenticationToken("school-principal", "u can be principal if you know this!");
            }
        });
        assertThat(BasicAuthenticationFilter.isProcessingBasicAuth(), is(false));
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("Authorization", "Basic " + Base64.encodeBase64String("loser:boozer".getBytes()));
        filter.doFilterHttp(httpRequest, new MockHttpServletResponse(), new FilterChain() {
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {

            }
        });
        assertThat(BasicAuthenticationFilter.isProcessingBasicAuth(), is(false));

        assertThat(hadBasicMarkOnInsideAuthenticationManager[0], is(true));
    }

    @Test
    public void testShouldRender500WithHTMLTextBodyWithApiAcceptHeaderWithHTML() throws IOException {

        httpRequest.addHeader("Accept", "text/html");

        SecurityContext context = SecurityContextHolder.getContext();

        filter.handleException(httpRequest, httpResponse, new Exception("some error"));
        verify(localizer).localize("INVALID_LDAP_ERROR");

        assertThat(((Exception) (httpRequest.getSession().getAttribute(AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY))).getMessage(), is(errorMessage));
        assertThat(httpRequest.getAttribute(SessionDenialAwareAuthenticationProcessingFilterEntryPoint.SESSION_DENIED).toString(), is("true"));
        assertThat(context.getAuthentication(), is(nullValue()));
        assertThat(httpResponse.getRedirectedUrl(), is("/go/auth/login?login_error=1"));
    }

    @Test
    public void testShouldRender500WithJSONBodyWithApiAcceptHeaderWithJSON() throws Exception {
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

        filter.handleException(httpRequest, httpResponse, new Exception("foo"));
        verify(localizer).localize("INVALID_LDAP_ERROR");
        assertEquals(500, httpResponse.getStatus());
        assertEquals("foo", httpResponse.getErrorMessage());
    }

}
