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

import org.springframework.http.MediaType;

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

    static {
        ACCESS_DENIED_HANDLER_MAP.put(MediaType.TEXT_XML, new XMLRequestHandler(MediaType.TEXT_XML));
        ACCESS_DENIED_HANDLER_MAP.put(MediaType.APPLICATION_XML, new XMLRequestHandler(MediaType.APPLICATION_XML));
        ACCESS_DENIED_HANDLER_MAP.put(MediaType.APPLICATION_JSON, new JsonRequestHandler(MediaType.APPLICATION_JSON));

        for (int i = 1; i < 100; i++) {
            final MediaType mediaType = MediaType.parseMediaType("application/vnd.go.cd.v" + i + "+json");
            ACCESS_DENIED_HANDLER_MAP.put(mediaType, new JsonRequestHandler(mediaType));
        }
    }

    private final RequestHandler authenticationHandler;

    public GoAccessDeniedHandler(RequestHandler authenticationHandler) {
        this.authenticationHandler = authenticationHandler;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        authenticationHandler.handle(request, response);
        final String acceptHeader = request.getHeader("Accept");
        if (isNotBlank(acceptHeader)) {
            try {
                List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
                MediaType.sortBySpecificityAndQuality(mediaTypes);

                for (MediaType mediaType : mediaTypes) {
                    final RequestHandler accessDeniedHandler = ACCESS_DENIED_HANDLER_MAP.get(mediaType);
                    if (accessDeniedHandler != null) {
                        accessDeniedHandler.handle(request, response);
                        return;
                    }
                }
            } catch (Exception ignore) {

            }
        }

        DEFAULT_ACCESS_DENIED_HANDLER.handle(request, response);
    }

    private static class GenericRequestHandler implements RequestHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to access this resource!");
        }
    }

    private static class XMLRequestHandler implements RequestHandler {
        private final MediaType type;

        private XMLRequestHandler(MediaType type) {
            this.type = type;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType(type.toString());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getOutputStream().print("<access-denied>");
            response.getOutputStream().print("  <message>You are not authorized to access this resource!</message>");
            response.getOutputStream().print("</access-denied>\n");
        }
    }

    private static class JsonRequestHandler implements RequestHandler {
        private final MediaType type;

        private JsonRequestHandler(MediaType type) {
            this.type = type;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType(type.toString());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getOutputStream().print("{\n");
            response.getOutputStream().print("  \"message\": \"You are not authorized to access this resource!\"\n");
            response.getOutputStream().print("}\n");
        }
    }
}
