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
package com.thoughtworks.go.apiv10.admin.shared.representers.stages.tasks;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.representers.PluginConfigurationRepresenter;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.config.PluginConfiguration;

public class PluggableTaskRepresenter {
    public static void toJSON(OutputWriter jsonWriter, PluggableTask pluggableTask) {
        BaseTaskRepresenter.toJSON(jsonWriter, pluggableTask);
        jsonWriter.addChild("plugin_configuration", attributeWriter -> PluginConfigurationRepresenter.toJSON(attributeWriter, pluggableTask.getPluginConfiguration()));
        jsonWriter.addChildList("configuration", configurationWriter -> ConfigurationPropertyRepresenter.toJSON(configurationWriter, pluggableTask.getConfiguration()));
    }

    public static PluggableTask fromJSON(JsonReader jsonReader) {
        PluggableTask pluggableTask = new PluggableTask();
        if (jsonReader == null) {
            return pluggableTask;
        }
        BaseTaskRepresenter.fromJSON(jsonReader, pluggableTask);
        PluginConfiguration pluginConfiguration = PluginConfigurationRepresenter.fromJSON(jsonReader.readJsonObject("plugin_configuration"));
        pluggableTask.setPluginConfiguration(pluginConfiguration);
        pluggableTask.addConfigurations(ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "configuration"));
        return pluggableTask;
    }
}
