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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.ResourceConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.util.Arrays.asList;

public class AgentMother {

    public static Agent elasticAgent() {
        Agent agent = new Agent(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "127.0.0.1", UUID.randomUUID().toString());
        agent.setElasticAgentId(UUID.randomUUID().toString());
        agent.setElasticPluginId(UUID.randomUUID().toString());
        return agent;
    }

    public static Agent localhost() {
        return new Agent("1234", "localhost", "10.10.1.1", UUID.randomUUID().toString());
    }

    public static Agent approvedAgent() {
        return new Agent("uuid", "approvedAgent", "192.168.0.1", UUID.randomUUID().toString());
    }

    public static Agent approvedLocalAgent() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        return new Agent("uuid", localHost.getHostName(), localHost.getHostAddress(), UUID.randomUUID().toString());
    }

    public static Agent deniedAgent() {
        Agent agent = new Agent("uuid", "deniedAgent", "192.168.0.1", UUID.randomUUID().toString());
        agent.disable();
        return agent;
    }

    public static Agent localAgent() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return new Agent("uuid-local" + UUID.randomUUID(), localHost.getHostName(), localHost.getHostAddress(), UUID.randomUUID().toString());
        } catch (UnknownHostException e) {
            throw bomb(e);
        }
    }

    public static Agent remoteAgent() {
        return new Agent("uuid-remote-" + UUID.randomUUID(), "remoteAgent", "254.254.254.254", UUID.randomUUID().toString());
    }

    public static Agent localAgentWithResources(String... resources) {
        Agent agent = localAgent();
        agent.setResourcesFromList(asList(resources));

        return agent;
    }
}
