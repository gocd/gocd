/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.analytics;

import com.thoughtworks.go.plugin.access.common.MetadataLoader;
import com.thoughtworks.go.plugin.access.common.PluginMetadataChangeListener;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AnalyticsMetadataLoader extends MetadataLoader<AnalyticsPluginInfo> {
    private List<PluginMetadataChangeListener> listeners = new ArrayList<>();

    @Autowired
    public AnalyticsMetadataLoader(PluginManager pluginManager, AnalyticsPluginInfoBuilder builder,
                                   AnalyticsExtension extension) {
        this(pluginManager, AnalyticsMetadataStore.instance(), builder, extension);
    }

    protected AnalyticsMetadataLoader(PluginManager pluginManager, AnalyticsMetadataStore metadataStore,
                                      AnalyticsPluginInfoBuilder builder, AnalyticsExtension extension) {
        super(pluginManager, builder, metadataStore, extension);
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        super.pluginLoaded(pluginDescriptor);

        if(extension.canHandlePlugin(pluginDescriptor.id())) {
            for (PluginMetadataChangeListener listener: listeners) {
                listener.onPluginMetadataCreate(pluginDescriptor.id());
            }
        }
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        super.pluginUnLoaded(pluginDescriptor);

        if (extension.canHandlePlugin(pluginDescriptor.id())) {
            for (PluginMetadataChangeListener listener : listeners) {
                listener.onPluginMetadataRemove(pluginDescriptor.id());
            }
        }
    }


    public void registerListeners(PluginMetadataChangeListener listener) {
        listeners.add(listener);
    }
}
