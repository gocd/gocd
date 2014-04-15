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

package com.thoughtworks.go.agent.bootstrapper.osx;

import com.thoughtworks.go.agent.bootstrapper.AgentBootstrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * worker thread for Mac only
 */
public class MacBootstrapperThread extends Thread{
    private static final Log LOG = LogFactory.getLog(MacBootstrapperThread.class);
    private final String server;
    private final int port;
    private AgentBootstrapper bootstrapper;

    public MacBootstrapperThread(String server, int port) {
        this.server = server;
        this.port = port;
        setName("MacBootstrapper"+getName() +" "+ server+ ":"+port);
    }

    public void run() {
        LOG.info("Launching Agent Bootstrapper for server " + server + ":" + port);
        bootstrapper = new AgentBootstrapper();
        bootstrapper.go(true, server, port);
    }

    public void stopLooping() {
        bootstrapper.stopLooping();
    }
}
