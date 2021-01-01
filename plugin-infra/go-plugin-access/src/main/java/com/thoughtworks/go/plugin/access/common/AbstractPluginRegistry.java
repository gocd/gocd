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

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractPluginRegistry<Extension extends AbstractExtension> implements PluginChangeListener {
    protected final Extension extension;
    protected final List<PluginDescriptor> plugins;

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public AbstractPluginRegistry(PluginManager pluginManager, Extension extension) {
        this.extension = extension;
        this.plugins = new ArrayList<>();
        pluginManager.addPluginChangeListener(this);
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        if (extension.canHandlePlugin(pluginDescriptor.id())) {
            plugins.add(pluginDescriptor);
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        plugins.remove(pluginDescriptor);
    }

    public List<PluginDescriptor> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public PluginDescriptor findPlugin(final String pluginId) {
        return plugins.stream().filter(descriptor -> descriptor.id().equals(pluginId)).findFirst().orElse(null);
    }
}
