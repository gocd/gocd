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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;

import java.util.ArrayList;
import java.util.List;

class NotificationViewModelBuilder implements ViewModelBuilder {
    private PluginManager pluginManager;
    private NotificationPluginRegistry notificationPluginRegistry;

    public NotificationViewModelBuilder(PluginManager manager, NotificationPluginRegistry notificationPluginRegistry) {
        this.pluginManager = manager;
        this.notificationPluginRegistry = notificationPluginRegistry;
    }

    public List<PluginInfo> allPluginInfos() {
        List<PluginInfo> pluginInfos = new ArrayList<>();

        for(String pluginId : notificationPluginRegistry.getNotificationPlugins()) {
            GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);

            pluginInfos.add(new PluginInfo(pluginId, descriptor.about().name(), descriptor.about().version(),
                    NotificationExtension.EXTENSION_NAME, null));
        }
        return pluginInfos;
    }

    @Override
    public PluginInfo pluginInfoFor(String pluginId) {
        if(!notificationPluginRegistry.getNotificationPlugins().contains(pluginId)) {
            return null;
        }

        GoPluginDescriptor descriptor = pluginManager.getPluginDescriptorFor(pluginId);

        return new PluginInfo(pluginId, descriptor.about().name(), descriptor.about().version(),
                NotificationExtension.EXTENSION_NAME, null);
    }
}
