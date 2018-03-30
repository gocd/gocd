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

package com.thoughtworks.go.server.newsecurity.authentication.handlers;

import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class GoAccessDeniedHandler implements RequestHandler {
    private static RequestHandler DEFAULT_ACCESS_DENIED_HANDLER = new GenericRequestHandler();
    private static Map<MediaType, RequestHandler> ACCESS_DENIED_HANDLER_MAP = new HashMap<>();
    private static final RequestMatcher AJAX_MATCHER = new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest");

    static {
        ACCESS_DENIED_HANDLER_MAP.put(MediaType.TEXT_XML, new XMLRequestHandler(MediaType.TEXT_XML));
        ACCESS_DENIED_HANDLER_MAP.put(MediaType.APPLICATION_XML, new XMLRequestHandler(MediaType.APPLICATION_XML));
        ACCESS_DENIED_HANDLER_MAP.put(MediaType.APPLICATION_JSON, new JsonRequestHandler(MediaType.APPLICATION_JSON));

        for (int i = 1; i < 100; i++) {
            final MediaType mediaType = MediaType.parseMediaType("application/vnd.go.cd.v" + i + "+json");
            ACCESS_DENIED_HANDLER_MAP.put(mediaType, new JsonRequestHandler(mediaType));
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isAjaxRequest(request)) {
            response.addHeader("WWW-Authenticate", "Basic realm=\"GoCD\"");
        }

        final String acceptHeader = request.getHeader("Accept");
        if (isNotBlank(acceptHeader)) {
            final RequestHandler handler = getHandler(acceptHeader);
            handler.handle(request, response);
        }

        DEFAULT_ACCESS_DENIED_HANDLER.handle(request, response);
    }

    private RequestHandler getHandler(String acceptHeader) {
        try {
            List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
            MediaType.sortBySpecificityAndQuality(mediaTypes);

            for (MediaType mediaType : mediaTypes) {
                final RequestHandler accessDeniedHandler = ACCESS_DENIED_HANDLER_MAP.get(mediaType);
                if (accessDeniedHandler != null) {
                    return accessDeniedHandler;
                }
            }
        } catch (Exception ignore) {

        }
        return DEFAULT_ACCESS_DENIED_HANDLER;
    }


    private boolean isAjaxRequest(HttpServletRequest request) {
        return AJAX_MATCHER.matches(request);
    }
}
