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
package com.thoughtworks.go.apiv1.pluginsettings.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.spark.Routes;

public class PluginSettingsRepresenter {
    public static void toJSON(OutputWriter outputWriter, PluginSettings pluginSettings) {
        outputWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.PluginSettingsAPI.id(pluginSettings.getPluginId()))
                .addAbsoluteLink("doc", Routes.PluginSettingsAPI.DOC)
                .addLink("find", Routes.PluginSettingsAPI.find()));

        outputWriter.add("plugin_id", pluginSettings.getPluginId());
        outputWriter.addChildList("configuration", w -> ConfigurationPropertyRepresenter.toJSON(w, pluginSettings.getPluginSettingsProperties()));
    }

    public static PluginSettings fromJSON(PluginInfo pluginInfo, JsonReader jsonReader) {
        PluginSettings pluginSettings = new PluginSettings(jsonReader.getString("plugin_id"));
        pluginSettings.addConfigurations(pluginInfo, ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "configuration"));
        return pluginSettings;
    }
}
