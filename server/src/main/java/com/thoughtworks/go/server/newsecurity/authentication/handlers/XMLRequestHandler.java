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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class XMLRequestHandler implements RequestHandler {
    private final MediaType type;

    XMLRequestHandler(MediaType type) {
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
