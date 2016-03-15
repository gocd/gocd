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

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public abstract class PluginMessageQueueHandler<T extends PluginAwareMessage> implements PluginChangeListener {
    private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final MessagingService messaging;
    protected final GoPluginExtension extension;
    protected final Map<String, PluginAwareMessageQueue> queues = new ConcurrentHashMap<>();
    private QueueFactory queueFactory;

    public PluginMessageQueueHandler(GoPluginExtension extension, MessagingService messaging, PluginManager pluginManager, QueueFactory queueFactory) {
        this.extension = extension;
        this.messaging = messaging;
        pluginManager.addPluginChangeListener(this, GoPlugin.class);
        this.queueFactory = queueFactory;
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (extension.canHandlePlugin(pluginDescriptor.id())) {
            PluginAwareMessageQueue queue = queueFactory.create(pluginDescriptor);
            this.queues.put(pluginDescriptor.id(), queue);
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        if (this.queues.containsKey(pluginDescriptor.id())) {
            try {
                PluginAwareMessageQueue queue = queues.get(pluginDescriptor.id());
                queue.stop();
            } catch (Exception e) {
                LOGGER.error("Unable to stop queue for {}, ERROR: {}", pluginDescriptor.id(), e.getMessage(), e);
                bomb(e);
            } finally {
                this.queues.remove(pluginDescriptor.id());
            }
        }
    }

    public void post(T message) {
        String pluginId = message.pluginId();
        try {
            if (queues.containsKey(pluginId)) {
                queues.get(pluginId).post(message);
            } else {
                LOGGER.error("Could not find a queue for {}", pluginId);
            }
        } catch (Exception e) {
            LOGGER.error("Failed while posting to queue for plugin {}. The error was {}", pluginId, e.getMessage(), e);
        }
    }
}
