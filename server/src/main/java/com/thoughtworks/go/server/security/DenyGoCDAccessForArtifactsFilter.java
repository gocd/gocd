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

import org.springframework.security.AccessDeniedException;
import org.springframework.security.ui.SpringSecurityFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.springframework.security.ui.FilterChainOrder.EXCEPTION_TRANSLATION_FILTER;

public class DenyGoCDAccessForArtifactsFilter extends SpringSecurityFilter {

    @Override
    protected void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (isRequestFromArtifact(request) && !requestingAnArtifact(request)) {
            throw new AccessDeniedException("Denied GoCD access for requests from artifacts.");
        }

        chain.doFilter(request, response);
    }

    private boolean requestingAnArtifact(HttpServletRequest request) {
        String requestURI;
        try {
            requestURI = new URI(request.getRequestURI()).normalize().toString();
        } catch (URISyntaxException e) {
            requestURI = request.getRequestURI();
        }
        return requestURI.startsWith("/go/files");
    }

    private boolean isRequestFromArtifact(HttpServletRequest request) throws MalformedURLException {
        final String referer = request.getHeader("Referer");

        if (isBlank(referer)) {
            return false;
        }

        return new URL(referer).getPath().startsWith("/go/files");
    }

    @Override
    public int getOrder() {
        return EXCEPTION_TRANSLATION_FILTER + 1;
    }
}
