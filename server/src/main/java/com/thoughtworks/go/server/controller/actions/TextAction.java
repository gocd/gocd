/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.controller.actions;

import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

public class TextAction implements RestfulAction {
    public static final String CONTENT_TYPE = "text/plain; charset=utf-8";

    private final int status;
    private final String contentType;
    private final String message;

    protected TextAction(int status, String message) {
        this(status, CONTENT_TYPE, message);
    }

    protected TextAction(int status, String contentType, String message) {
        this.status = status;
        this.contentType = contentType;
        this.message = String.valueOf(message);
    }

    public static RestfulAction notFound(String errorMessage) {
        return new TextAction(HTTP_NOT_FOUND, errorMessage);
    }

    public static RestfulAction forbidden(String errorMessage) {
        return new TextAction(HTTP_FORBIDDEN, errorMessage);
    }

    @Override
    public ModelAndView respond(HttpServletResponse response) throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        response.getWriter().write(message);
        return null;
    }
}
