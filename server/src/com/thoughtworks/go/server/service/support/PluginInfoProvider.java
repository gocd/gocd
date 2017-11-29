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

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PluginInfoProvider implements ServerInfoProvider {

    private final DefaultPluginInfoFinder pluginInfoFinder;

    @Autowired
    public PluginInfoProvider(DefaultPluginInfoFinder pluginInfoFinder) {
        this.pluginInfoFinder = pluginInfoFinder;
    }

    @Override
    public double priority() {
        return 14.0;
    }

    @Override
    public Map<String, Object> asJson() {
        List<Map<String, Object>> plugins = new ArrayList<>();
        Collection<PluginInfo> pluginInfos = pluginInfoFinder.allPluginInfos(null);
        for (PluginInfo pluginInfo : pluginInfos) {
            Map<String, Object> pluginJson = getPluginJson(pluginInfo);
            plugins.add(pluginJson);
        }
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("plugins", plugins);
        return json;
    }

    private Map<String, Object> getPluginJson(PluginInfo pluginInfo) {
        Map<String, Object> pluginJson = new LinkedHashMap<>();
        pluginJson.put("id", pluginInfo.getDescriptor().id());
        pluginJson.put("type", pluginInfo.getExtensionName());
        return pluginJson;
    }

    @Override
    public String name() {
        return "Plugin information";
    }
}
