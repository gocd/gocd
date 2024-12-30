/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public abstract class PluginMessageQueueHandler<T extends PluginAwareMessage> implements PluginChangeListener {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final MessagingService<T> messaging;
    protected final GoPluginExtension extension;
    protected final Map<String, PluginAwareMessageQueue<T>> queues = new ConcurrentHashMap<>();
    private final QueueFactory<T> queueFactory;

    @SuppressWarnings("unchecked")
    public PluginMessageQueueHandler(GoPluginExtension extension, MessagingService<? extends GoMessage> messaging, PluginManager pluginManager, QueueFactory<T> queueFactory) {
        this.messaging = (MessagingService<T>) messaging;
        this.extension = extension;
        pluginManager.addPluginChangeListener(this);
        this.queueFactory = queueFactory;
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (extension.canHandlePlugin(pluginDescriptor.id())) {
            PluginAwareMessageQueue<T> queue = queueFactory.create(pluginDescriptor);
            this.queues.put(pluginDescriptor.id(), queue);
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        if (this.queues.containsKey(pluginDescriptor.id())) {
            try {
                PluginAwareMessageQueue<T> queue = queues.get(pluginDescriptor.id());
                queue.stop();
            } catch (Exception e) {
                LOGGER.error("Unable to stop queue for {}, ERROR: {}", pluginDescriptor.id(), e.getMessage(), e);
                bomb(e);
            } finally {
                this.queues.remove(pluginDescriptor.id());
            }
        }
    }

    public void post(T message, long timeToLive) {
        String pluginId = message.pluginId();
        try {
            if (queues.containsKey(pluginId)) {
                PluginAwareMessageQueue<T> queue = queues.get(pluginId);
                LOGGER.debug("Posting message {} to queue {}", message, queue.queueName);
                queue.post(message, timeToLive);
                LOGGER.debug("Message {} posted to queue {}", message, queue.queueName);
            } else {
                LOGGER.error("Could not find a queue for {}", pluginId);
                //TODO: Add server health error
            }
        } catch (Exception e) {
            LOGGER.error("Failed while posting to queue for plugin {}. The error was {}", pluginId, e.getMessage(), e);
        }
    }
}
