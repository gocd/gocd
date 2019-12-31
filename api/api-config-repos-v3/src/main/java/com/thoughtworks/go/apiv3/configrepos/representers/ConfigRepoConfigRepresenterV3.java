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

package com.thoughtworks.go.apiv3.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.Collections;

import static com.thoughtworks.go.spark.Routes.ConfigRepos.*;

public class ConfigRepoConfigRepresenterV3 {
    public static void toJSON(OutputWriter json, ConfigRepoConfig repo) {
        attachLinks(json, repo);
        json.add("id", repo.getId());
        json.add("plugin_id", repo.getPluginId());
        json.addChild("material", w -> MaterialsRepresenter.toJSON(w, repo.getMaterialConfig()));
        if (!repo.errors().isEmpty()) {
            json.addChild("errors", errorWriter -> new ErrorGetter(Collections.emptyMap()).toJSON(errorWriter, repo));
        }
        attachConfigurations(json, repo);
    }

    public static ConfigRepoConfig fromJSON(JsonReader jsonReader) {
        MaterialConfig material = MaterialsRepresenter.fromJSON(jsonReader.readJsonObject("material"));
        ConfigRepoConfig repo = new ConfigRepoConfig();
        jsonReader.readStringIfPresent("id", repo::setId);
        jsonReader.readStringIfPresent("plugin_id", repo::setPluginId);
        repo.setMaterialConfig(material);

        repo.addConfigurations(ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "configuration"));
        return repo;
    }

    private static void attachLinks(OutputWriter json, ConfigRepoConfig repo) {
        json.addLinks(links -> {
            links.addLink("self", id(repo.getId()));
            links.addAbsoluteLink("doc", DOC);
            links.addLink("find", find());
        });
    }

    private static void attachConfigurations(OutputWriter json, ConfigRepoConfig repo) {
        json.addChildList("configuration", w -> ConfigurationPropertyRepresenter.toJSON(w, repo.getConfiguration()));
    }
}
