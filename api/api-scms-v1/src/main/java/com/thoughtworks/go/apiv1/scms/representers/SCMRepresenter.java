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
package com.thoughtworks.go.apiv1.scms.representers;


import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.representers.PluginConfigurationRepresenter;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;


public class SCMRepresenter {
    public static void toJSON(OutputWriter jsonWriter, SCM scm) {
        jsonWriter.addLinks(
            outputLinkWriter -> outputLinkWriter.addAbsoluteLink("doc", Routes.SCM.DOC)
                .addLink("self", Routes.SCM.name(scm.getName()))
                .addLink("find", Routes.SCM.find()))
            .add("id", scm.getId())
            .add("name", scm.getName())
            .add("auto_update", scm.isAutoUpdate())
            .addChild("plugin_metadata",  pluginMetadataWriter -> PluginConfigurationRepresenter.toJSON(pluginMetadataWriter, scm.getPluginConfiguration()))
        .addChildList("configuration", configWriter -> ConfigurationPropertyRepresenter.toJSON(configWriter, scm.getConfiguration()));

        if (scm.errors() != null && (!scm.errors().isEmpty())) {
            jsonWriter.addChild("errors", errorWriter -> new ErrorGetter(Collections.singletonMap("autoUpdate", "auto_update"))
                .toJSON(errorWriter, scm));
        }
    }

    public static SCM fromJSON(JsonReader jsonReader) {
       return fromJSON(jsonReader, true);
    }

    public static SCM fromJSON(JsonReader jsonReader, boolean mustHaveId) {
        String id = mustHaveId ? jsonReader.getString("id") : jsonReader.getStringOrDefault("id", null);
        String name = jsonReader.getString("name");
        boolean autoUpdate = jsonReader.getBooleanOrDefault("auto_update", true);

        SCM scm = new SCM(id, name);
        scm.setAutoUpdate(autoUpdate);

        if (jsonReader.hasJsonObject("plugin_metadata")) {
            PluginConfiguration pluginMetadata = PluginConfigurationRepresenter.fromJSON(jsonReader.readJsonObject("plugin_metadata"));
            scm.setPluginConfiguration(pluginMetadata);
        }

        if (jsonReader.hasJsonArray("configuration")) {
            Configuration configuration = new Configuration(ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "configuration"));
            scm.setConfiguration(configuration);
        }

        return scm;
    }
}
