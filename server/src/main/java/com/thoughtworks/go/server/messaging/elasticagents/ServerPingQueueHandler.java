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

import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.messaging.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerPingQueueHandler extends PluginMessageQueueHandler<ServerPingMessage> {
    final static String QUEUE_NAME_PREFIX = ServerPingQueueHandler.class.getSimpleName() + ".";

    @Autowired
    public ServerPingQueueHandler(final MessagingService messaging, final ElasticAgentPluginRegistry elasticAgentPluginRegistry, ElasticAgentExtension elasticAgentExtension, PluginManager pluginManager, final SystemEnvironment systemEnvironment) {
        super(elasticAgentExtension, messaging, pluginManager, new QueueFactory() {
            @Override
            public PluginAwareMessageQueue create(GoPluginDescriptor pluginDescriptor) {
                return new PluginAwareMessageQueue(messaging, pluginDescriptor.id(), QUEUE_NAME_PREFIX + pluginDescriptor.id(), systemEnvironment.get(SystemEnvironment.GO_ELASTIC_PLUGIN_SERVER_PING_THREADS), listener());
            }

            public ListenerFactory listener() {
                return new ListenerFactory() {
                    @Override
                    public GoMessageListener create() {
                        return new ServerPingListener(elasticAgentPluginRegistry);
                    }
                };
            }
        });
    }
}
