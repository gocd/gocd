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
package com.thoughtworks.go.apiv1.shared.representers.materials;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import org.apache.commons.lang3.StringUtils;

public class GitMaterialRepresenter {

    public static void toJSON(OutputWriter jsonWriter, GitMaterialConfig gitMaterialConfig) {
        ScmMaterialRepresenter.toJSON(jsonWriter, gitMaterialConfig);
        jsonWriter.add("url", gitMaterialConfig.getUrl());
        jsonWriter.addWithDefaultIfBlank("branch", gitMaterialConfig.getBranch(), "master");
        jsonWriter.add("submodule_folder", gitMaterialConfig.getSubmoduleFolder());
        jsonWriter.add("shallow_clone", gitMaterialConfig.isShallowClone());
    }

    public static GitMaterialConfig fromJSON(JsonReader jsonReader) {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig();
        jsonReader.readStringIfPresent("url", gitMaterialConfig::setUrl);
        ScmMaterialRepresenter.fromJSON(jsonReader, gitMaterialConfig);
        jsonReader.optString("branch").ifPresent(branch -> {
            if (StringUtils.isNotBlank(branch)) {
                gitMaterialConfig.setBranch(branch);
            } else {
                gitMaterialConfig.setBranch("master");
            }
        });
        jsonReader.readStringIfPresent("submodule_folder", gitMaterialConfig::setSubmoduleFolder);
        jsonReader.optBoolean("shallow_clone").ifPresent(gitMaterialConfig::setShallowClone);

        return gitMaterialConfig;
    }
}
