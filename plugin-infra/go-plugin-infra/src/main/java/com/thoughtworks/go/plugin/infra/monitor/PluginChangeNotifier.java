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
package com.thoughtworks.go.plugin.infra.monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.subtract;

public class PluginChangeNotifier {
    public void notify(PluginJarChangeListener listener, Collection<BundleOrPluginFileDetails> knowPluginFiles, Collection<BundleOrPluginFileDetails> currentPluginFiles) {
        List<BundleOrPluginFileDetails> oldPlugins = new ArrayList<>(knowPluginFiles);

        subtract(oldPlugins, currentPluginFiles).forEach(listener::pluginJarRemoved);

        currentPluginFiles.forEach(newPlugin -> {
            int index = oldPlugins.indexOf(newPlugin);
            if (index < 0) {
                listener.pluginJarAdded(newPlugin);
            } else if (newPlugin.doesTimeStampDiffer(oldPlugins.get(index))) {
                listener.pluginJarUpdated(newPlugin);
            }
        });
    }
}
