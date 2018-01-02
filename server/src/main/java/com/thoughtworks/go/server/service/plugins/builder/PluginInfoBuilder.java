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

import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.service.plugins.InvalidPluginTypeException;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.*;

@Component
@Deprecated
public class PluginInfoBuilder {

    private Map<String, ViewModelBuilder> pluginExtensionToBuilder = new LinkedHashMap<>();

    @Autowired
    public PluginInfoBuilder(NotificationPluginRegistry notificationPluginRegistry,
                             PluginManager pluginManager) {
        pluginExtensionToBuilder.put(NOTIFICATION_EXTENSION, new NotificationViewModelBuilder(pluginManager, notificationPluginRegistry));
        pluginExtensionToBuilder.put(PACKAGE_MATERIAL_EXTENSION, new PackageViewModelBuilder(pluginManager));
        pluginExtensionToBuilder.put(PLUGGABLE_TASK_EXTENSION, new PluggableTaskViewModelBuilder(pluginManager));
        pluginExtensionToBuilder.put(SCM_EXTENSION, new SCMViewModelBuilder(pluginManager));
        pluginExtensionToBuilder.put(ELASTIC_AGENT_EXTENSION, new ElasticAgentViewModelBuilder(ElasticAgentMetadataStore.instance()));
        pluginExtensionToBuilder.put(AUTHORIZATION_EXTENSION, new AuthorizationViewModelBuilder(AuthorizationMetadataStore.instance()));
    }

    public List<PluginInfo> allPluginInfos(String type) {
        List<PluginInfo> plugins = new ArrayList<>();

        if (type != null) {
            if (!pluginExtensionToBuilder.containsKey(type)) {
                throw new InvalidPluginTypeException();
            }

            return pluginExtensionToBuilder.get(type).allPluginInfos();
        }

        for (ViewModelBuilder builder : pluginExtensionToBuilder.values()) {
            plugins.addAll(builder.allPluginInfos());
        }

        return plugins;
    }

    public PluginInfo pluginInfoFor(String pluginId) {
        for (ViewModelBuilder builder : pluginExtensionToBuilder.values()) {
            PluginInfo pluginInfo = builder.pluginInfoFor(pluginId);

            if (pluginInfo != null) {
                return pluginInfo;
            }
        }
        return null;
    }
}
