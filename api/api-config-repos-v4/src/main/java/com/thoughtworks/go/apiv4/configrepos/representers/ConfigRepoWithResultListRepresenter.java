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
import com.thoughtworks.go.apiv4.configrepos.ConfigRepoWithResult;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.spark.Routes.ConfigRepos.BASE;

public class ConfigRepoWithResultListRepresenter {
    public static void toJSON(OutputWriter outputWriter, List<ConfigRepoWithResult> repos, Map<String, List<String>> autoSuggestions) {
        attachLinks(outputWriter);
        outputWriter.addChild("_embedded", w -> w.addChildList(
                "config_repos", all -> repos.forEach(
                        repo -> all.addChild(el -> ConfigRepoWithResultRepresenter.toJSON(el, repo))
                )
        )).addChildList("auto_completion", (suggestionWriter) -> autoSuggestions.forEach(
                (key, value) -> suggestionWriter.addChild(childWriter -> childWriter
                        .add("key", key)
                        .addChildList("value", value))
        ));
    }

    private static void attachLinks(OutputWriter json) {
        json.addLinks(links -> links.addLink("self", BASE));
    }

}

