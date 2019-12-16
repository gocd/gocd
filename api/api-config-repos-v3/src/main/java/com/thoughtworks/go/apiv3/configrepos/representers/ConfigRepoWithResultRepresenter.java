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
import com.thoughtworks.go.apiv3.configrepos.ConfigRepoWithResult;
import com.thoughtworks.go.config.PartialConfigParseResult;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;

import static com.thoughtworks.go.spark.Routes.ConfigRepos.*;

public class ConfigRepoWithResultRepresenter {
    public static void toJSON(OutputWriter json, ConfigRepoWithResult crwr, boolean canAdminister) {
        ConfigRepoConfig repo = crwr.repo();
        PartialConfigParseResult result = crwr.result();

        attachLinks(json, repo);
        json.add("id", repo.getId());
        json.add("plugin_id", repo.getPluginId());
        json.addChild("material", w -> MaterialsRepresenter.toJSON(w, repo.getMaterialConfig()));
        json.add("can_administer", canAdminister);
        attachConfigurations(json, repo);

        json.add("material_update_in_progress", crwr.isMaterialUpdateInProgress());
        json.addChild("parse_info", w -> PartialConfigParseResultRepresenter.toJSON(w, result));
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
