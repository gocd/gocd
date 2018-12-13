/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.web;

import com.google.common.net.HttpHeaders;
import com.thoughtworks.go.util.SystemEnvironment;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Deprecated
//TODO: ketanpkr newer spring has this baked in.
public class DefaultHeadersFilter implements Filter {

    private SystemEnvironment systemEnvironment = new SystemEnvironment();

    public void destroy() {
    }

    public void init(FilterConfig config) {
        // No default config
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {
        HttpServletResponse response = (HttpServletResponse) resp;

        if (!response.isCommitted()) {
            addSecureHeader(response, "X-XSS-Protection", "1; mode=block");
            addSecureHeader(response, "X-Content-Type-Options", "nosniff");
            addSecureHeader(response, "X-Frame-Options", "SAMEORIGIN");
            addSecureHeader(response, "X-UA-Compatible", "chrome=1");
            HstsHeader.fromSystemEnvironment(systemEnvironment)
                    .ifPresent((hstsHeader) -> addSecureHeader(response, hstsHeader.headerName(), hstsHeader.headerValue()));
        }
        chain.doFilter(req, resp);
    }

    private void addSecureHeader(HttpServletResponse response, String securityHeader, String value) {
        if (!response.containsHeader(securityHeader)) {
            response.setHeader(securityHeader, value);
        }
    }

}
