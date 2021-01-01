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
package com.thoughtworks.go.server.newsecurity.handlers.renderer;


import com.thoughtworks.go.server.newsecurity.models.ContentTypeAwareResponse;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentTypeNegotiationMessageRenderer {
    private static final ContentTypeAwareResponse TEXT_XML_REQUEST_HANDLER = new XMLErrorMessageRenderer(MediaType.TEXT_XML);
    private static final ContentTypeAwareResponse APPLICATION_XML_REQUEST_HANDLER = new XMLErrorMessageRenderer(MediaType.APPLICATION_XML);
    private static final Map<MediaType, ContentTypeAwareResponse> ACCESS_DENIED_HANDLER_MAP = new HashMap<>();
    private static ContentTypeAwareResponse JSON_ACCESS_DENIED_HANDLER = new JsonErrorMessageRenderer(MediaType.APPLICATION_JSON);

    static {
        ACCESS_DENIED_HANDLER_MAP.put(MediaType.TEXT_XML, TEXT_XML_REQUEST_HANDLER);
        ACCESS_DENIED_HANDLER_MAP.put(MediaType.APPLICATION_XML, APPLICATION_XML_REQUEST_HANDLER);
        ACCESS_DENIED_HANDLER_MAP.put(MediaType.APPLICATION_JSON, JSON_ACCESS_DENIED_HANDLER);

        for (int i = 1; i < 100; i++) {
            final MediaType mediaType = MediaType.parseMediaType("application/vnd.go.cd.v" + i + "+json");
            ACCESS_DENIED_HANDLER_MAP.put(mediaType, new JsonErrorMessageRenderer(mediaType));
        }
    }

    public ContentTypeAwareResponse getResponse(HttpServletRequest request) {
        try {
            List<MediaType> mediaTypes = MediaType.parseMediaTypes(request.getHeader("Accept"));
            MediaType.sortBySpecificityAndQuality(mediaTypes);

            for (MediaType mediaType : mediaTypes) {
                final ContentTypeAwareResponse accessDeniedHandler = ACCESS_DENIED_HANDLER_MAP.get(mediaType.removeQualityValue());
                if (accessDeniedHandler != null) {
                    return accessDeniedHandler;
                }
            }
        } catch (Exception ignore) {
        }

        if (request.getRequestURI().endsWith(".xml")) {
            return APPLICATION_XML_REQUEST_HANDLER;
        }
        return JSON_ACCESS_DENIED_HANDLER;
    }
}
