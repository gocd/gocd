/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.newsecurity.handlers.renderer.ContentTypeNegotiationMessageRenderer;
import com.thoughtworks.go.server.newsecurity.models.ContentTypeAwareResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DenyIfRefererIsNotFilesFilter extends OncePerRequestFilter {

    private static final ContentTypeNegotiationMessageRenderer CONTENT_TYPE_NEGOTIATION_MESSAGE_HANDLER = new ContentTypeNegotiationMessageRenderer();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getServletPath().startsWith("/files/")) {
            throw new UnsupportedOperationException("Filter should not be invoked for `/files/` urls.");
        }

        if (isRequestFromArtifact(request)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            ContentTypeAwareResponse contentTypeAwareResponse = CONTENT_TYPE_NEGOTIATION_MESSAGE_HANDLER.getResponse(request);
            response.setCharacterEncoding("utf-8");
            response.setContentType(contentTypeAwareResponse.getContentType().toString());
            response.getOutputStream().print(contentTypeAwareResponse.getFormattedMessage("Denied GoCD access for requests from artifacts."));
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isRequestFromArtifact(HttpServletRequest request) {
        final String referer = request.getHeader("Referer");
        try {
            return isNotBlank(referer) && new URI(referer).getPath().startsWith("/go/files/");
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
