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

package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Resource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class AgentMother {

    public static AgentConfig elasticAgent() {
        AgentConfig agentConfig = new AgentConfig(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "127.0.0.1");
        agentConfig.setElasticAgentId(UUID.randomUUID().toString());
        agentConfig.setElasticPluginId(UUID.randomUUID().toString());
        return agentConfig;
    }

    public static AgentConfig localhost() {
        return new AgentConfig("1234", "localhost", "10.10.1.1");
    }

    public static AgentConfig approvedAgent() {
        return new AgentConfig("uuid", "approvedAgent", "192.168.0.1");
    }

    public static AgentConfig approvedLocalAgent() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        return new AgentConfig("uuid", localHost.getHostName(), localHost.getHostAddress());
    }

    public static AgentConfig deniedAgent() {
        AgentConfig agentConfig = new AgentConfig("uuid", "deniedAgent", "192.168.0.1");
        agentConfig.disable();
        return agentConfig;
    }

    public static AgentConfig localAgent() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return new AgentConfig("uuid-local" + UUID.randomUUID(), localHost.getHostName(), localHost.getHostAddress());
        } catch (UnknownHostException e) {
            throw bomb(e);
        }
    }

    public static AgentConfig remoteAgent() {
        return new AgentConfig("uuid-remote-" + UUID.randomUUID(), "remoteAgent", "254.254.254.254");
    }

    public static AgentConfig localAgentWithResources(String... resources) {
        AgentConfig agentConfig = localAgent();
        for (String resource : resources) {
            agentConfig.addResource(new Resource(resource));
        }
        return agentConfig;
    }
}
