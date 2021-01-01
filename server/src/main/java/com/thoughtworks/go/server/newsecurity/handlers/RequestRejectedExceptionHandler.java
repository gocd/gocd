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
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestRejectedExceptionHandler {
    private static final ContentTypeNegotiationMessageRenderer CONTENT_TYPE_NEGOTIATION_MESSAGE_HANDLER = new ContentTypeNegotiationMessageRenderer();
    private static final OrRequestMatcher API_REQUEST_MATCHER = new OrRequestMatcher(
            new AntPathRequestMatcher("/remoting/**"),
            new AntPathRequestMatcher("/add-on/*/api/**"),
            new AntPathRequestMatcher("/api/**"),
            new AntPathRequestMatcher("/cctray.xml")
    );

    public void handle(HttpServletRequest request, HttpServletResponse response, String message, HttpStatus httpStatus) throws IOException {
        if (API_REQUEST_MATCHER.matches(request)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            final ContentTypeAwareResponse contentTypeAwareResponse = CONTENT_TYPE_NEGOTIATION_MESSAGE_HANDLER.getResponse(request);
            response.setCharacterEncoding("utf-8");
            response.setContentType(contentTypeAwareResponse.getContentType().toString());
            response.getOutputStream().print(contentTypeAwareResponse.getFormattedMessage(message));
        } else {
            response.sendError(httpStatus.value(), message);
        }
    }
}
