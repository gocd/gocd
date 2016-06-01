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

package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.config.PluginPreferenceStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;

import static com.thoughtworks.go.util.StringUtil.isBlank;

public class ConfigurationDisplayUtil {
    public static List<ConfigurationProperty> getConfigurationPropertiesToBeUsedForDisplay(PluginPreferenceStore metadataStore, String pluginId, final Configuration configuration) {
        List<ConfigurationProperty> keysForDisplay = new ArrayList<>();
        boolean pluginDoesNotExist = !metadataStore.hasPreferenceFor(pluginId);

        for (ConfigurationProperty property : configuration) {
            boolean isNotASecureProperty = !property.isSecure();
            boolean isPartOfIdentity = metadataStore.hasOption(pluginId, property.getConfigurationKey().getName(), PackageConfiguration.PART_OF_IDENTITY);
            if (isNotASecureProperty && !isBlank(property.getValue()) && (pluginDoesNotExist || isPartOfIdentity)) {
                keysForDisplay.add(property);
            }
        }
        return keysForDisplay;
    }
}
