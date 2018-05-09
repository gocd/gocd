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

import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.X509Credential;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.newsecurity.x509.CachingSubjectDnX509PrincipalExtractor;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.util.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;

@Component
public class X509AuthenticationFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(X509AuthenticationFilter.class);

    private static final String X509_HEADER_KEY = "javax.servlet.request.X509Certificate";
    private final CachingSubjectDnX509PrincipalExtractor subjectDnX509PrincipalExtractor;
    private final Clock clock;

    @Autowired
    public X509AuthenticationFilter(Clock clock) {
        this.clock = clock;
        this.subjectDnX509PrincipalExtractor = new CachingSubjectDnX509PrincipalExtractor();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SessionUtils.hasAuthenticationToken(request)) {
            filterChain.doFilter(request, response);
        } else {
            performAuthentication(request, response, filterChain);
        }
    }

    private void performAuthentication(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain filterChain) throws IOException, ServletException {
        final X509Certificate x509Certificate = extractClientCertificate(request);
        if (x509Certificate == null) {
            LOGGER.debug("Denying access, certificate is not provided.");
            response.setStatus(403);
        } else {
            LOGGER.debug("Adding agent user to current session and proceeding.");
            String subjectDN = (String) subjectDnX509PrincipalExtractor.extractPrincipal(x509Certificate);
            final GoUserPrinciple agentUser = new GoUserPrinciple("_go_agent_" + subjectDN, "", GoAuthority.ROLE_AGENT.asAuthority());

            final AuthenticationToken<X509Credential> authentication = new AuthenticationToken<>(agentUser, new X509Credential(x509Certificate), null, clock.currentTimeMillis(), null);

            SessionUtils.setAuthenticationTokenAfterRecreatingSession(authentication, request);
            filterChain.doFilter(request, response);
        }
    }

    private X509Certificate extractClientCertificate(HttpServletRequest request) {
        final X509Certificate[] certs = (X509Certificate[]) request.getAttribute(X509_HEADER_KEY);

        if (certs != null && certs.length > 0) {
            LOGGER.debug("Client certificate found in request: {}", certs[0]);
            return certs[0];
        }

        LOGGER.debug("No client certificate found in request.");

        return null;
    }
}
