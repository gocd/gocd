/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.packagematerial;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.plugin.access.config.PluginPreferenceStore;
import com.thoughtworks.go.plugin.api.config.Option;

import static org.apache.commons.lang.StringUtils.isEmpty;

public abstract class AbstractMetaDataStore extends  PluginPreferenceStore<PackageConfigurations> {

    public void addMetadataFor(String pluginId, PackageConfigurations configuration) {
        setPreferenceFor(pluginId, configuration);
    }

    public PackageConfigurations getMetadata(String pluginId) {
        if (isEmpty(pluginId)) {
            return null;
        }
        return preferenceFor(pluginId);
    }

    public void removeMetadata(String pluginId) {
        if (!isEmpty(pluginId)) {
            removePreferenceFor(pluginId);
        }
    }

    public boolean hasOption(String pluginId, String key, Option<Boolean> option) {
        if (!isEmpty(pluginId) && hasPreferenceFor(pluginId)) {
            PackageConfigurations configurations = preferenceFor(pluginId);
            PackageConfiguration configurationForGivenKey = configurations.get(key);
            if (configurationForGivenKey != null) {
                return configurationForGivenKey.hasOption(option);
            }
        }
        return option.getValue();
    }

    public List<String> getPlugins() {
        return new ArrayList<String>(pluginIds());
    }

    public boolean hasPlugin(String pluginId) {
        return hasPreferenceFor(pluginId);
    }
}
