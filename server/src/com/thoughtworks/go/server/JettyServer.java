/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server;

import org.mortbay.component.Container;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.webapp.WebAppContext;

public class JettyServer {
    private Server server;
    private WebAppContext webAppContext;

    public JettyServer(Server server) {
        this.server = server;
    }

    public Container getContainer() {
        return server.getContainer();
    }

    public void addConnector(Connector selectChannelConnector) {
        server.addConnector(selectChannelConnector);
    }

    public void addHandler(Handler handler) {
        server.addHandler(handler);
    }

    public Server getServer() {
        return server;
    }

    public void setStopAtShutdown(boolean stopAtShutdown) {
        server.setStopAtShutdown(stopAtShutdown);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public void addWebAppHandler(WebAppContext webAppContext) {
        addHandler(webAppContext);
        this.webAppContext = webAppContext;
    }

    public WebAppContext webAppContext() {
        return webAppContext;
    }
}
