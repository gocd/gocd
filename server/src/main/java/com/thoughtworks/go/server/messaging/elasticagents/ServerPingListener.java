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
import com.thoughtworks.go.server.messaging.GoMessageListener;

public class ServerPingListener implements GoMessageListener<ServerPingMessage> {

    private final ElasticAgentPluginRegistry elasticAgentPluginRegistry;

    public ServerPingListener(ElasticAgentPluginRegistry elasticAgentPluginRegistry) {
        this.elasticAgentPluginRegistry = elasticAgentPluginRegistry;
    }

    @Override
    public void onMessage(final ServerPingMessage message) {
        elasticAgentPluginRegistry.serverPing(message.pluginId());
    }

}
