/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PluginInfoProvider implements ServerInfoProvider {

    private final DefaultPluginInfoFinder pluginInfoFinder;
    private final PluginManager pluginManager;

    @Autowired
    public PluginInfoProvider(DefaultPluginInfoFinder pluginInfoFinder, PluginManager pluginManager) {
        this.pluginInfoFinder = pluginInfoFinder;
        this.pluginManager = pluginManager;
    }

    @Override
    public double priority() {
        return 14.0;
    }

    @Override
    public Map<String, Object> asJson() {
        List<Map<String, Object>> plugins = new ArrayList<>();
        List<GoPluginDescriptor> goPluginDescriptors = pluginManager.plugins();
        for (GoPluginDescriptor goPluginDescriptor : goPluginDescriptors) {
            CombinedPluginInfo combinedPluginInfo = pluginInfoFinder.pluginInfoFor(goPluginDescriptor.id());
            Map<String, Object> pluginJson = getPluginJson(combinedPluginInfo, goPluginDescriptor);
            plugins.add(pluginJson);
        }
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("plugins", plugins);
        return json;
    }

    private Map<String, Object> getPluginJson(CombinedPluginInfo combinedPluginInfo, GoPluginDescriptor goPluginDescriptor) {
        Map<String, Object> pluginJson = new LinkedHashMap<>();
        pluginJson.put("id", goPluginDescriptor.id());
        pluginJson.put("type", combinedPluginInfo == null ? Collections.emptyList() : combinedPluginInfo.extensionNames());
        pluginJson.put("version", goPluginDescriptor.about().version());
        pluginJson.put("bundled_plugin", goPluginDescriptor.isBundledPlugin());
        pluginJson.put("status", goPluginDescriptor.getStatus());
        return pluginJson;
    }

    @Override
    public String name() {
        return "Plugin information";
    }
}
