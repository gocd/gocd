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
package com.thoughtworks.go.plugin.access.common;

import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public abstract class MetadataStore<T extends PluginInfo> {
    protected Map<String, T> pluginInfos = new ConcurrentHashMap<>();

    public T getPluginInfo(String pluginId) {
        if (isEmpty(pluginId)) {
            return null;
        }
        return pluginInfos.get(pluginId);
    }

    public void setPluginInfo(T pluginInfo) {
        if (pluginInfo != null && pluginInfo.getDescriptor() != null) {
            pluginInfos.put(pluginInfo.getDescriptor().id(), pluginInfo);
        }
    }

    public void remove(String pluginId) {
        pluginInfos.remove(pluginId);
    }

    @Deprecated
    public Collection<T> allPluginInfos() {
        return pluginInfos.values();
    }

    public void clear() {
        pluginInfos.clear();
    }
}
