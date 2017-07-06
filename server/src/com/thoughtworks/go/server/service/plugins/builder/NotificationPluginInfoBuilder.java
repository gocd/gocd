/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.NotificationPluginInfo;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

@Deprecated
public class NotificationPluginInfoBuilder implements NewPluginInfoBuilder<NotificationPluginInfo> {
    private final PluginManager pluginManager;
    private final NotificationPluginRegistry registry;

    public NotificationPluginInfoBuilder(PluginManager pluginManager, NotificationPluginRegistry registry) {
        this.pluginManager = pluginManager;
        this.registry = registry;
    }

    @Override
    public NotificationPluginInfo pluginInfoFor(String pluginId) {
        if (!registry.getNotificationPlugins().contains(pluginId)) {
            return null;
        }

        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(pluginId);
        return new NotificationPluginInfo(plugin);
    }

    @Override
    public Collection<NotificationPluginInfo> allPluginInfos() {
        return registry.getNotificationPlugins().stream().map(new Function<String, NotificationPluginInfo>() {
            @Override
            public NotificationPluginInfo apply(String pluginId) {
                return NotificationPluginInfoBuilder.this.pluginInfoFor(pluginId);
            }
        }).collect(Collectors.toList());
    }
}
