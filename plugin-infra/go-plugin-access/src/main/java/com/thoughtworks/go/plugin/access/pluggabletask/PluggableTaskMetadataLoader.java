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
package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.access.common.MetadataLoader;
import com.thoughtworks.go.plugin.domain.pluggabletask.PluggableTaskPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluggableTaskMetadataLoader extends MetadataLoader<PluggableTaskPluginInfo> {
    @Autowired
    public PluggableTaskMetadataLoader(PluginManager pluginManager, PluggableTaskPluginInfoBuilder builder, TaskExtension extension) {
        this(pluginManager, PluggableTaskMetadataStore.instance(), builder, extension);
    }

    protected PluggableTaskMetadataLoader(PluginManager pluginManager, PluggableTaskMetadataStore metadataStore, PluggableTaskPluginInfoBuilder builder, TaskExtension extension) {
        super(pluginManager, builder, metadataStore, extension);
    }

}
