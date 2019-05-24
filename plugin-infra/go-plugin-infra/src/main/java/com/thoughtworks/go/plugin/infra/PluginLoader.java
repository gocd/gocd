/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This knows what needs to be done when a plugin is loaded (and unloaded).
 */

@Component
public class PluginLoader {
    private Collection<PluginChangeListener> pluginChangeListeners = new ConcurrentLinkedQueue<>();
    private List<PluginPostLoadHook> pluginPostLoadHooks = new ArrayList<>();
    private GoPluginOSGiFramework pluginOSGiFramework;

    @Autowired
    public PluginLoader(GoPluginOSGiFramework pluginOSGiFramework) {
        this.pluginOSGiFramework = pluginOSGiFramework;
    }

    public void addPluginChangeListener(PluginChangeListener pluginChangeListener) {
        pluginOSGiFramework.addPluginChangeListener(pluginChangeListener);
    }

    public PluginPostLoadHook addPluginPostLoadHook(PluginPostLoadHook pluginPostLoadHook) {
        return pluginOSGiFramework.addPostLoadHook(pluginPostLoadHook);
    }

    public void loadPlugin(GoPluginDescriptor descriptor) {
        pluginOSGiFramework.loadPlugin(descriptor);
    }

    public void unloadPlugin(GoPluginDescriptor descriptorOfRemovedPlugin) {
        pluginOSGiFramework.unloadPlugin(descriptorOfRemovedPlugin);
    }
}
