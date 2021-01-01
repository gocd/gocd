/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.newsecurity.handlers;

import com.thoughtworks.go.server.newsecurity.handlers.renderer.ContentTypeNegotiationMessageRenderer;
import com.thoughtworks.go.server.newsecurity.models.ContentTypeAwareResponse;
import com.thoughtworks.go.server.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class BasicAuthenticationWithChallengeFailureResponseHandler implements ResponseHandler {
    private static final RequestMatcher AJAX_MATCHER = new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest");
    private static final ContentTypeNegotiationMessageRenderer CONTENT_TYPE_NEGOTIATION_MESSAGE_HANDLER = new ContentTypeNegotiationMessageRenderer();
    private final SecurityService securityService;

    @Autowired
    public BasicAuthenticationWithChallengeFailureResponseHandler(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       int statusCode,
                       String errorMessage) throws IOException {

        if (!isAjaxRequest(request)) {
            if (securityService.isSecurityEnabled()) {
                response.addHeader("WWW-Authenticate", "Basic realm=\"GoCD\"");
            }
        }

        response.setStatus(statusCode);
        final ContentTypeAwareResponse contentTypeAwareResponse = CONTENT_TYPE_NEGOTIATION_MESSAGE_HANDLER.getResponse(request);
        response.setCharacterEncoding("utf-8");
        response.setContentType(contentTypeAwareResponse.getContentType().toString());
        response.getOutputStream().print(contentTypeAwareResponse.getFormattedMessage(errorMessage));
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return AJAX_MATCHER.matches(request);
    }
}
