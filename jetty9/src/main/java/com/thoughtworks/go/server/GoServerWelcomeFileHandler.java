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

package com.thoughtworks.go.server;

import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

class GoServerWelcomeFileHandler extends ContextHandler {
    private final SystemEnvironment systemEnvironment;

    GoServerWelcomeFileHandler(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        setContextPath("/");
        setHandler(new Handler());
    }

    private class Handler extends AbstractHandler {
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {

            if (request.getPathInfo().equals("/") || request.getPathInfo().equals("/go") || request.getPathInfo().equals("/go/")) {
                response.sendRedirect(GoConstants.GO_URL_CONTEXT + systemEnvironment.landingPage());
                return;
            }

            if ("/go".equals(request.getPathInfo()) || request.getPathInfo().startsWith("/go/")) {
                return;
            }

            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
            response.setHeader("X-UA-Compatible", "chrome=1");

            if ("/".equals(target)) {
                response.setHeader("Location", GoConstants.GO_URL_CONTEXT + systemEnvironment.landingPage());
                response.setStatus(301);
                response.setHeader("Content-Type", "text/html");
                PrintWriter writer = response.getWriter();
                writer.write("redirecting..");
                writer.close();
            }
        }
    }
}
