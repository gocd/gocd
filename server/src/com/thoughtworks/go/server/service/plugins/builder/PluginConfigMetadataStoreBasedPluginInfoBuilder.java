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

import com.thoughtworks.go.plugin.access.common.PluginConfigMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.NewPluginInfo;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class PluginConfigMetadataStoreBasedPluginInfoBuilder<T extends NewPluginInfo, V extends PluginConfigMetadataStore<?>> implements NewPluginInfoBuilder<T> {

    protected final V store;

    public PluginConfigMetadataStoreBasedPluginInfoBuilder(V store) {
        this.store = store;
    }

    @Override
    public final Collection<T> allPluginInfos() {
        return store.getPlugins().stream().map(new Function<PluginDescriptor, T>() {
            @Override
            public T apply(PluginDescriptor pluginDescriptor) {
                return PluginConfigMetadataStoreBasedPluginInfoBuilder.this.pluginInfoFor(pluginDescriptor.id());
            }
        }).collect(Collectors.toList());
    }
}
