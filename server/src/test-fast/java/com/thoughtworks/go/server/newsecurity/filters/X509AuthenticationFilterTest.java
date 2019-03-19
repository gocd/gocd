/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.http.mocks.*;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.X509CertificateGenerator;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.X509Credential;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.util.TestingClock;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class X509AuthenticationFilterTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain filterChain = mock(FilterChain.class);
    private final TestingClock clock = new TestingClock();

    @Test
    void shouldPopulateAgentUserInSession() throws Exception {
        final Registration registration = createRegistration("blah");
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/")
                .withX509(registration.getChain())
                .build();

        new X509AuthenticationFilter(clock).doFilter(request, response, filterChain);

        final AuthenticationToken authentication = SessionUtils.getAuthenticationToken(request);
        assertThat(authentication.getUser().getUsername())
                .isEqualTo("_go_agent_blah");
        assertThat(authentication.getUser().getAuthorities())
                .hasSize(1)
                .contains(GoAuthority.ROLE_AGENT.asAuthority());
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldRejectRequestWith403IfCertificateIsNotProvided() throws Exception {
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/").build();
        new X509AuthenticationFilter(clock).doFilter(request, response, filterChain);

        assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
        verifyZeroInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void shouldReauthenticateIfCredentialsAreProvidedInRequestEvenIfRequestWasPreviouslyAuthenticatedAsANormalUser() throws ServletException, IOException {
        final Registration registration = createRegistration("blah");
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/")
                .withX509(registration.getChain())
                .build();

        com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAsRandomUser(request);
        final HttpSession originalSession = request.getSession(true);

        new X509AuthenticationFilter(clock).doFilter(request, response, filterChain);

        final AuthenticationToken authentication = SessionUtils.getAuthenticationToken(request);
        assertThat(authentication.getUser().getUsername())
                .isEqualTo("_go_agent_blah");
        assertThat(authentication.getUser().getAuthorities())
                .hasSize(1)
                .contains(GoAuthority.ROLE_AGENT.asAuthority());
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getSession(false)).isNotSameAs(originalSession);
    }

    @Test
    void shouldReauthenticateIfCredentialsAreProvidedInRequestEvenIfRequestWasPreviouslyAuthenticatedUsingAStolenX509Certificate() throws ServletException, IOException {
        final Registration registration = createRegistration("good");
        GoUserPrinciple goodAgentPrinciple = new GoUserPrinciple("_go_agent_good", "");
        X509Credential x509Credential = new X509Credential(registration.getFirstCertificate());
        MockHttpSession originalSession = new MockHttpSession();
        SessionUtils.setAuthenticationTokenWithoutRecreatingSession(new AuthenticationToken<>(goodAgentPrinciple, x509Credential, null, 0, null), HttpRequestBuilder.GET("/dont-care").withSession(originalSession).build());

        final Registration anotherRegistration = createRegistration("reject-me");
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/")
                .withX509(anotherRegistration.getChain())
                .withSession(originalSession)
                .build();

        new X509AuthenticationFilter(clock).doFilter(request, response, filterChain);

        final AuthenticationToken authentication = SessionUtils.getAuthenticationToken(request);
        assertThat(authentication.getUser().getUsername())
                .isEqualTo("_go_agent_reject-me");
        assertThat(authentication.getUser().getAuthorities())
                .hasSize(1)
                .contains(GoAuthority.ROLE_AGENT.asAuthority());
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getSession(false)).isNotSameAs(originalSession);
    }

    private Registration createRegistration(String hostname) throws IOException {
        File tempKeystoreFile = temporaryFolder.newFile();
        X509CertificateGenerator certificateGenerator = new X509CertificateGenerator();
        certificateGenerator.createAndStoreCACertificates(tempKeystoreFile);
        return certificateGenerator.createAgentCertificate(tempKeystoreFile, hostname);
    }

}
