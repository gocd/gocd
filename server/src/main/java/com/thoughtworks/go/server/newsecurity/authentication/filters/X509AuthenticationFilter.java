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

package com.thoughtworks.go.server.newsecurity.authentication.filters;

import com.thoughtworks.go.server.security.GoAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;
import java.util.Collections;

@Component
public class X509AuthenticationFilter extends AbstractAuthenticationFilter {
    private static final String X509_HEADER_KEY = "javax.servlet.request.X509Certificate";
    private final CachingSubjectDnX509PrincipalExtractor subjectDnX509PrincipalExtractor;

    @Autowired
    public X509AuthenticationFilter(CachingSubjectDnX509PrincipalExtractor subjectDnX509PrincipalExtractor) {
        this.subjectDnX509PrincipalExtractor = subjectDnX509PrincipalExtractor;
    }

    private X509Certificate extractClientCertificate(HttpServletRequest request) {
        final X509Certificate[] certs = (X509Certificate[]) request.getAttribute(X509_HEADER_KEY);

        if (certs != null && certs.length > 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("[X509 Client Authentication] Client certificate found in request: %s", certs[0]));
            }

            return certs[0];
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[X509 Client Authentication] No client certificate found in request.");
        }

        return null;
    }

    @Override
    protected User attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        final X509Certificate x509Certificate = extractClientCertificate(request);

        if (x509Certificate == null) {
            throw new BadCredentialsException("No certificate provided by client!");
        } else {
            String subjectDN = (String) subjectDnX509PrincipalExtractor.extractPrincipal(x509Certificate);
            return new User("_go_agent_" + subjectDN, "", Collections.singletonList(GoAuthority.ROLE_AGENT.asAuthority()));
        }
    }

    @Override
    protected boolean canHandleRequest(HttpServletRequest request) {
        return true;
    }

    @Override
    protected boolean isSecurityEnabled() {
        return true;
    }

    @Override
    protected void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        response.setStatus(403);
    }
}
