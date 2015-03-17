/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.util;

import com.thoughtworks.go.server.Jetty9Server;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class GoPlainSocketConnector implements GoSocketConnector {
    private final int plainPort;
    private static final int MAX_IDLE_TIME = 30000;
    private static final int RESPONSE_BUFFER_SIZE = 32768;
    private final Connector httpConnector;

    public GoPlainSocketConnector(Jetty9Server server, SystemEnvironment systemEnvironment) {
        this.plainPort = systemEnvironment.getServerPort();
        httpConnector = plainConnector(server);
    }

    private Connector plainConnector(Jetty9Server server) {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(RESPONSE_BUFFER_SIZE); // 32 MB

        ServerConnector httpConnector = new ServerConnector(server.getServer(), new HttpConnectionFactory(httpConfig));
        httpConnector.setPort(plainPort);
        httpConnector.setIdleTimeout(MAX_IDLE_TIME);
        return httpConnector;
    }

    public Connector getConnector() {
        return httpConnector;
    }
}
