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
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

public abstract class MetadataLoader<T extends PluginInfo> implements PluginChangeListener {
    private final PluginInfoBuilder<T> builder;
    protected final MetadataStore<T> metadataStore;
    protected final AbstractExtension extension;

    public MetadataLoader(PluginManager pluginManager, PluginInfoBuilder<T> builder, MetadataStore<T> metadataStore, AbstractExtension extension) {
        this.builder = builder;
        this.metadataStore = metadataStore;
        this.extension = extension;

        pluginManager.addPluginChangeListener(this);
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if(extension.canHandlePlugin(pluginDescriptor.id())) {
            metadataStore.setPluginInfo(builder.pluginInfoFor(pluginDescriptor));
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        metadataStore.remove(pluginDescriptor.id());
    }
}

