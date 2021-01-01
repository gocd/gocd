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
package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;

/**
 * Handles upgrade request for console log WebSocket connections. Validates that user is authorized to
 * view the pipeline.
 */
public class ConsoleLogSocketServlet extends WebSocketServlet {

    private SecurityService securityService;
    private ConsoleLogSocketCreator socketCreator;

    @Override
    public void init() throws ServletException {
        WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        socketCreator = wac.getBean(ConsoleLogSocketCreator.class);
        securityService = wac.getBean(SecurityService.class);

        super.init();
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.setCreator(socketCreator);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Username userName = SessionUtils.currentUsername();
        String pipeline = pipeline(request);

        if (authorizedToViewPipeline(userName, pipeline)) {
            super.service(request, response);
            return;
        }

        response.sendError(SC_FORBIDDEN, String.format("%s is not authorized to view the pipeline %s", userName.getDisplayName(), pipeline));
    }

    private boolean authorizedToViewPipeline(Username username, String pipelineName) {
        return securityService.hasViewPermissionForPipeline(username, pipelineName);
    }

    private String pipeline(HttpServletRequest request) {
        return request.getPathInfo().split("/")[1];
    }
}
