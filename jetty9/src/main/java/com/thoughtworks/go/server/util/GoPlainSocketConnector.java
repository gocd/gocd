/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.util;

import com.thoughtworks.go.server.Jetty9Server;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.*;

public class GoPlainSocketConnector implements GoSocketConnector {
    private final Connector httpConnector;
    private final SystemEnvironment systemEnvironment;

    public GoPlainSocketConnector(Jetty9Server server, SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        httpConnector = plainConnector(server);
    }

    private Connector plainConnector(Jetty9Server server) {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(systemEnvironment.get(SystemEnvironment.RESPONSE_BUFFER_SIZE));
        httpConfig.setSendServerVersion(false);
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        ServerConnector httpConnector = new ServerConnector(server.getServer(), new HttpConnectionFactory(httpConfig));
        httpConnector.setHost(systemEnvironment.getListenHost());
        httpConnector.setPort(systemEnvironment.getServerPort());
        httpConnector.setIdleTimeout(systemEnvironment.get(SystemEnvironment.IDLE_TIMEOUT));
        return httpConnector;
    }

    public Connector getConnector() {
        return httpConnector;
    }
}
