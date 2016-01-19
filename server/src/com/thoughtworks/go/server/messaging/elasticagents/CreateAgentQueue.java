/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.messaging.elasticagents;

import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.server.messaging.GoMessageQueue;
import com.thoughtworks.go.server.messaging.MessagingService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateAgentQueue extends GoMessageQueue<CreateAgentMessage> {
    @Autowired
    public CreateAgentQueue(MessagingService messaging, ElasticAgentPluginRegistry elasticAgentPluginRegistry, SystemEnvironment systemEnvironment) {
        super(messaging, "create-agent-queue");
        addWorkers(elasticAgentPluginRegistry, systemEnvironment);
    }

    private void addWorkers(ElasticAgentPluginRegistry elasticAgentPluginRegistry, SystemEnvironment systemEnvironment) {
        Integer workerThreads = systemEnvironment.get(SystemEnvironment.GO_ELASTIC_PLUGIN_CREATE_AGENT_THREADS);

        for (int i = 0; i < workerThreads; i++) {
            addListener(new CreateAgentListener(elasticAgentPluginRegistry));
        }
    }
}
