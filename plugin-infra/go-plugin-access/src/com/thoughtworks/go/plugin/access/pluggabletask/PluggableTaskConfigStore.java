/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.access.config.PluginPreferenceStore;
import com.thoughtworks.go.plugin.api.config.Option;

import java.util.Set;

public class PluggableTaskConfigStore extends PluginPreferenceStore<TaskPreference> {
    private static PluggableTaskConfigStore PLUGGABLE_TASK_CONFIG_STORE = new PluggableTaskConfigStore();

    public static PluggableTaskConfigStore store() {
        return PLUGGABLE_TASK_CONFIG_STORE;
    }

    @Deprecated
    // only for test usage
    public void clear() {
        Set<String> plugins = pluginIds();
        for (String pluginId : plugins) {
            removePreferenceFor(pluginId);
        }
    }

    @Override
    public boolean hasOption(String pluginId, String name, Option<Boolean> partOfIdentity) {
        throw new UnsupportedOperationException();
    }
}
