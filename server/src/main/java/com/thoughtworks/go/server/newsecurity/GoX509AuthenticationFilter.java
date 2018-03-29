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

package com.thoughtworks.go.server.newsecurity;

import com.thoughtworks.go.server.security.GoAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collections;

@Component
public class GoX509AuthenticationFilter extends OncePerRequestFilter {
    private final CachingSubjectDnX509PrincipalExtractor subjectDnX509PrincipalExtractor;

    private GoX509AuthenticationFilter(CachingSubjectDnX509PrincipalExtractor subjectDnX509PrincipalExtractor) {
        this.subjectDnX509PrincipalExtractor = subjectDnX509PrincipalExtractor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final X509Certificate x509Certificate = extractClientCertificate(request);

        if (x509Certificate == null) {
            response.setStatus(401);
            request.getSession().invalidate();
        } else {
            String subjectDN = (String) subjectDnX509PrincipalExtractor.extractPrincipal(x509Certificate);
            User user = new User("_go_agent_" + subjectDN, "", Collections.singletonList(GoAuthority.ROLE_AGENT.asAuthority()));

            request.getSession().setAttribute(AuthenticationFilter.CURRENT_USER, user);
        }
    }

    private X509Certificate extractClientCertificate(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

        if (certs != null && certs.length > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("X.509 client authentication certificate:" + certs[0]);
            }

            return certs[0];
        }

        if (logger.isDebugEnabled()) {
            logger.debug("No client certificate found in request.");
        }

        return null;
    }
}
