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
package com.thoughtworks.go.apiv7.plugininfos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;

public class PluggableInstanceSettingsRepresenter {
    public static void toJSON(OutputWriter writer, PluggableInstanceSettings pluggableInstanceSettings) {
        if (pluggableInstanceSettings.getConfigurations() != null) {
            writer.addChildList("configurations", configurationsWriter -> pluggableInstanceSettings.getConfigurations().forEach(
                    configuration -> configurationsWriter.addChild(configurationWriter -> {
                        configurationWriter.add("key", configuration.getKey());
                        configurationWriter.addChild("metadata", metadataWriter -> MetadataRepresenterResolver.resolve(configuration.getMetadata()).toJSON(metadataWriter, configuration.getMetadata()));
                    })));
        }

        if (pluggableInstanceSettings.getView() != null) {
            writer.addChild("view", viewWriter -> viewWriter.add("template", pluggableInstanceSettings.getView().getTemplate()));
        }
    }
}
