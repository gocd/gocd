/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 *
 */

package com.thoughtworks.go.server.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.security.AuthenticationException;
import org.springframework.security.ui.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class BasicProcessingFilterEntryPoint implements AuthenticationEntryPoint, InitializingBean {

    public void afterPropertiesSet() throws Exception {
    }

    public void commence(ServletRequest request, ServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"GoCD\"");

        if (hasAccept(request, "application/vnd.go.cd.v1+json")) {
            httpResponse.setContentType("application/vnd.go.cd.v1+json");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getOutputStream().print("{\n");
            httpResponse.getOutputStream().print("  \"message\": \"You are not authorized to access this resource!\"\n");
            httpResponse.getOutputStream().print("}\n");
            return;
        }
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
    }

    private boolean hasAccept(ServletRequest request, String expectedContentType) {
        if (request instanceof HttpServletRequest) {
            String accept = ((HttpServletRequest) request).getHeader("Accept");
            if (accept != null) {
                List<MediaType> mediaTypes = MediaType.parseMediaTypes(accept);
                for (MediaType mediaType : mediaTypes) {
                    String type = mediaType.getType() + "/" + mediaType.getSubtype();
                    if (type.equals(expectedContentType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
