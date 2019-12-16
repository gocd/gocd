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
import com.thoughtworks.go.config.remote.ConfigReposConfig;

import static com.thoughtworks.go.spark.Routes.ConfigRepos.BASE;

public class ConfigReposConfigRepresenterV3 {
    public static void toJSON(OutputWriter json, ConfigReposConfig repos) {
        attachLinks(json);
        json.addChild("_embedded", w -> w.addChildList(
                "config_repos", all -> repos.forEach(
                        repo -> all.addChild(el -> ConfigRepoConfigRepresenterV3.toJSON(el, repo))
                )
        ));
    }

    private static void attachLinks(OutputWriter json) {
        json.addLinks(links -> links.addLink("self", BASE));
    }
}
