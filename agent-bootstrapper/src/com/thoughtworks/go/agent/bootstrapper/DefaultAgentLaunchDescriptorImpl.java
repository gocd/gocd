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

package com.thoughtworks.go.agent.bootstrapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptor;

import static com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptorKeys.HOSTNAME;
import static com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptorKeys.PORT;

class DefaultAgentLaunchDescriptorImpl implements AgentLaunchDescriptor {

    private final AgentBootstrapper bootstrapper;
    private final Map context = new ConcurrentHashMap();

    public DefaultAgentLaunchDescriptorImpl(String hostname, int port, AgentBootstrapper self) {
        this.bootstrapper = self;
        buildContext(hostname, port);
    }

    private void buildContext(String hostname, int port) {
        context.putAll(System.getenv());
        context.putAll(System.getProperties());
        context.put(HOSTNAME, hostname);
        context.put(PORT, port);
    }

    @Override public Map context() {
        return context;
    }

    @Override
    public AgentBootstrapper getBootstrapper() {
        return bootstrapper;
    }
}
