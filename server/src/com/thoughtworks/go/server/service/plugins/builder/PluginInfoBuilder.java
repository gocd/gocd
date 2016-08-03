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

import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.access.elastic.Constants;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.plugin.access.packagematerial.JsonBasedPackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.JsonBasedTaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.service.plugins.InvalidPluginTypeException;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PluginInfoBuilder {

    private Map<String, ViewModelBuilder> pluginExtensionToBuilder = new HashMap<>();

    @Autowired
    public PluginInfoBuilder(AuthenticationPluginRegistry authenticationPluginRegistry,
                             NotificationPluginRegistry notificationPluginRegistry,
                             ElasticAgentPluginRegistry elasticAgentPluginRegistry,
                             PluginManager pluginManager) {
        pluginExtensionToBuilder.put(AuthenticationExtension.EXTENSION_NAME, new AuthenticationViewModelBuilder(pluginManager, authenticationPluginRegistry));
        pluginExtensionToBuilder.put(NotificationExtension.EXTENSION_NAME, new NotificationViewModelBuilder(pluginManager, notificationPluginRegistry));
        pluginExtensionToBuilder.put(JsonBasedPackageRepositoryExtension.EXTENSION_NAME, new PackageViewModelBuilder(pluginManager));
        pluginExtensionToBuilder.put(JsonBasedTaskExtension.TASK_EXTENSION, new PluggableTaskViewModelBuilder(pluginManager));
        pluginExtensionToBuilder.put(SCMExtension.EXTENSION_NAME, new SCMViewModelBuilder(pluginManager));
        pluginExtensionToBuilder.put(Constants.EXTENSION_NAME, new ElasticAgentViewViewModelBuilder(pluginManager, elasticAgentPluginRegistry));
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

            if(pluginInfo != null) {
                return pluginInfo;
            }
        }
        return null;
    }
}
