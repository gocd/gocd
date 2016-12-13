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

package com.thoughtworks.go.plugin.access.common;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public abstract class PluginConfigMetadataStore<T extends PluginChangeListener> {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public abstract void add(PluginDescriptor plugin, T extension);

    public abstract void remove(PluginDescriptor plugin);

    public abstract Collection<PluginDescriptor> getPlugins();

    public PluginDescriptor find(final String pluginId) {
        return ListUtil.find(getPlugins(), new ListUtil.Condition() {
            @Override
            public <Q> boolean isMet(Q item) {
                return ((PluginDescriptor) item).id().equals(pluginId);
            }
        });
    }

}
