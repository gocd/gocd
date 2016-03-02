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
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.messaging.GoMessageQueue;
import com.thoughtworks.go.server.messaging.MessagingService;
import com.thoughtworks.go.server.messaging.activemq.ActiveMqMessagingService;
import com.thoughtworks.go.server.messaging.activemq.JMSMessageListenerAdapter;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Component
public class CreateAgentQueueHandler implements PluginChangeListener {
    private final MessagingService messaging;
    private final ElasticAgentPluginRegistry elasticAgentPluginRegistry;
    private final ElasticAgentExtension elasticAgentExtension;
    protected final static String QUEUE_NAME_PREFIX = "create-agent-queue-for-";
    protected HashMap<String, GoMessageQueue> queues = new HashMap<>();
    protected HashMap<String, ArrayList<JMSMessageListenerAdapter>> listeners = new HashMap<>();
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CreateAgentQueueHandler.class.getName());

    @Autowired
    public CreateAgentQueueHandler(MessagingService messaging, ElasticAgentPluginRegistry elasticAgentPluginRegistry, ElasticAgentExtension elasticAgentExtension, PluginManager pluginManager) {
        this.messaging = messaging;
        this.elasticAgentPluginRegistry = elasticAgentPluginRegistry;
        this.elasticAgentExtension = elasticAgentExtension;
        pluginManager.addPluginChangeListener(this, GoPlugin.class);

    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (elasticAgentExtension.canHandlePlugin(pluginDescriptor.id())) {
            GoMessageQueue<CreateAgentMessage> queue = new GoMessageQueue<>(messaging, QUEUE_NAME_PREFIX + pluginDescriptor.id());
            JMSMessageListenerAdapter listener = queue.addListener(new CreateAgentListener(elasticAgentPluginRegistry));
            if (!listeners.containsKey(pluginDescriptor.id())) {
                this.listeners.put(pluginDescriptor.id(), new ArrayList<JMSMessageListenerAdapter>());
            }
            this.listeners.get(pluginDescriptor.id()).add(listener);
            this.queues.put(pluginDescriptor.id(), queue);
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        if (this.queues.containsKey(pluginDescriptor.id())) {
            try {
                GoMessageQueue queue = queues.get(pluginDescriptor.id());
                queue.stop();
                ArrayList<JMSMessageListenerAdapter> listenerAdapters = listeners.get(pluginDescriptor.id());
                for (JMSMessageListenerAdapter listenerAdapter : listenerAdapters) {
                    try {
                        listenerAdapter.stop();
                    } catch (JMSException e) {
                        LOGGER.error("Unable to stop create-agent-listener {}, ERROR: {}", listenerAdapter.thread.getName(), e.getMessage(), e);
                        bomb(e);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Unable to stop create-agent-queue {}, ERROR: {}", pluginDescriptor.id(), e.getMessage(), e);
                bomb(e);
            } finally {
                this.queues.remove(pluginDescriptor.id());
                this.listeners.remove(pluginDescriptor.id());
            }
        }
    }

    public void post(CreateAgentMessage createAgentMessage) {
        String pluginId = createAgentMessage.getPluginId();
        queues.get(pluginId).post(createAgentMessage);
    }

}
