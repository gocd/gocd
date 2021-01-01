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
package com.thoughtworks.go.apiv4.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.apiv4.configrepos.ConfigRepoWithResult;
import com.thoughtworks.go.config.PartialConfigParseResult;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;

import static com.thoughtworks.go.spark.Routes.ConfigRepos.*;

public class ConfigRepoWithResultRepresenter {
    public static void toJSON(OutputWriter outputWriter, ConfigRepoWithResult crwr) {
        ConfigRepoConfig repo = crwr.repo();
        PartialConfigParseResult result = crwr.result();

        attachLinks(outputWriter, repo);
        outputWriter.add("id", repo.getId());
        outputWriter.add("plugin_id", repo.getPluginId());
        outputWriter.addChild("material", w -> MaterialsRepresenter.toJSON(w, repo.getRepo()));
        attachConfigurations(outputWriter, repo);

        outputWriter.addChildList("rules", rulesWriter -> RulesRepresenter.toJSON(rulesWriter, repo.getRules()));

        outputWriter.add("material_update_in_progress", crwr.isMaterialUpdateInProgress());
        outputWriter.addChild("parse_info", w -> PartialConfigParseResultRepresenter.toJSON(w, result));
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
