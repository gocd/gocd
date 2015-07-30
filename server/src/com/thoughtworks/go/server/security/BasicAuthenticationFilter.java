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
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.ui.AbstractProcessingFilter;
import org.springframework.security.ui.basicauth.BasicProcessingFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class BasicAuthenticationFilter extends BasicProcessingFilter {

    private static ThreadLocal<Boolean> isProcessingBasicAuth = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    private static final Logger LOG = Logger.getLogger(BasicAuthenticationFilter.class);
    private Localizer localizer;

    @Autowired
    public BasicAuthenticationFilter(Localizer localizer) {
        this.localizer = localizer;
    }

    @Override
    public void doFilterHttp(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain) throws IOException, ServletException {
        try {
            isProcessingBasicAuth.set(true);
            super.doFilterHttp(httpRequest, httpResponse, chain);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            LOG.debug(e.getMessage(), e);
            handleException(httpRequest, httpResponse, e);
        } finally {
            isProcessingBasicAuth.set(false);
        }
    }

    public void handleException(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Exception e) throws IOException {
        String message = localizer.localize("INVALID_LDAP_ERROR");
        if (hasAccept(httpRequest, "text/html") || hasAccept(httpRequest, "application/xhtml")) {
            httpRequest.getSession().setAttribute(AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY, new RuntimeException(message));
            httpRequest.setAttribute(SessionDenialAwareAuthenticationProcessingFilterEntryPoint.SESSION_DENIED, true);

            httpResponse.sendRedirect("/go/auth/login?login_error=1");
            return;
        }
        if (hasAccept(httpRequest, "application/vnd.go.cd.v1+json") || hasAccept(httpRequest, "application/json")) {
            String msg = String.format("{\n \"message\": \"%s\"\n}\n", message);
            generateResponse(httpResponse, "application/vnd.go.cd.v1+json; charset=utf-8", msg);
            return;
        }
        if (hasAccept(httpRequest, "application/xml")) {
            String msg = String.format("<message>%s</message>\n", message);
            generateResponse(httpResponse, "application/xml; charset=utf-8", msg);
            return;
        }

        httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private void generateResponse(HttpServletResponse httpResponse, String type, String msg) throws IOException {
        httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"GoCD\"");
        httpResponse.setContentType(type);
        httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        httpResponse.getOutputStream().print(msg);
    }

    public static boolean isProcessingBasicAuth() {
        return isProcessingBasicAuth.get();
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
