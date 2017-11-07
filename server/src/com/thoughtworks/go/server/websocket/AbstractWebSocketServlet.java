/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;

public abstract class AbstractWebSocketServlet<SocketCreater extends WebSocketCreator> extends WebSocketServlet {
    private WebApplicationContext wac;
    private SocketCreater socketCreater;

    @Override
    public void init() throws ServletException {
        this.wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        this.socketCreater = wac.getBean(socketCreater());
        super.init();
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(new SystemEnvironment().getWebsocketMaxIdleTime());
        factory.setCreator(socketCreater);
    }

    protected abstract Class<SocketCreater> socketCreater();
}
