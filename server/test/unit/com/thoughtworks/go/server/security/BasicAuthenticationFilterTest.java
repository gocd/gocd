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

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BasicAuthenticationFilterTest {
    @Test
    public void shouldConvey_itsBasicProcessingFilter() throws IOException, ServletException {
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter();
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
}
