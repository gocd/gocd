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

package com.thoughtworks.go.server.security;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class GoAccessDeniedHandler implements AccessDeniedHandler {
    private final AccessDeniedHandler DEFAULT_ACCESS_DENIED_HANDLER = new GenericAccessDeniedHandler();
    private final Map<MediaType, AccessDeniedHandler> ACCESS_DENIED_HANDLER_MAP = accessDeniedHandlers();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        final String acceptHeader = request.getHeader("Accept");
        if (isNotBlank(acceptHeader)) {
            try {
                List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
                MediaType.sortBySpecificityAndQuality(mediaTypes);


                for (MediaType mediaType : mediaTypes) {
                    final AccessDeniedHandler accessDeniedHandler = ACCESS_DENIED_HANDLER_MAP.get(mediaType);
                    if (accessDeniedHandler != null) {
                        accessDeniedHandler.handle(request, response, accessDeniedException);
                        return;
                    }
                }
            } catch (Exception ignore) {

            }
        }

        DEFAULT_ACCESS_DENIED_HANDLER.handle(request, response, accessDeniedException);
    }

    public static Map<MediaType, AccessDeniedHandler> accessDeniedHandlers() {
        final HashMap<MediaType, AccessDeniedHandler> handlerHashMap = new HashMap<>();
        handlerHashMap.put(MediaType.TEXT_XML, new XMLAccessDeniedHandler(MediaType.TEXT_XML));
        handlerHashMap.put(MediaType.APPLICATION_XML, new XMLAccessDeniedHandler(MediaType.APPLICATION_XML));
        handlerHashMap.put(MediaType.APPLICATION_JSON, new JsonAccessDeniedHandler(MediaType.APPLICATION_JSON));

        for (int i = 1; i < 100; i++) {
            final MediaType mediaType = MediaType.parseMediaType("application/vnd.go.cd.v" + i + "+json");
            handlerHashMap.put(mediaType, new JsonAccessDeniedHandler(mediaType));
        }
        return handlerHashMap;
    }

    private static class GenericAccessDeniedHandler implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, accessDeniedException.getMessage());
        }
    }

    private static class XMLAccessDeniedHandler implements AccessDeniedHandler {
        private final MediaType type;

        private XMLAccessDeniedHandler(MediaType type) {
            this.type = type;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
            response.setContentType(type.toString());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getOutputStream().print("<access-denied>");
            response.getOutputStream().print("  <message>You are not authorized to access this resource!</message>");
            response.getOutputStream().print("</access-denied>\n");
        }
    }

    private static class JsonAccessDeniedHandler implements AccessDeniedHandler {
        private final MediaType type;

        private JsonAccessDeniedHandler(MediaType type) {
            this.type = type;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
            response.setContentType(type.toString());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getOutputStream().print("{\n");
            response.getOutputStream().print("  \"message\": \"You are not authorized to access this resource!\"\n");
            response.getOutputStream().print("}\n");
        }
    }
}
