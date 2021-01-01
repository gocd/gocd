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
package com.thoughtworks.go.apiv9.admin.shared.representers.materials;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv9.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import org.apache.commons.lang3.StringUtils;

public class GitMaterialRepresenter extends ScmMaterialRepresenter<GitMaterialConfig> {
    @Override
    public void toJSON(OutputWriter jsonWriter, GitMaterialConfig gitMaterialConfig) {
        super.toJSON(jsonWriter, gitMaterialConfig);
        jsonWriter.addWithDefaultIfBlank("branch", gitMaterialConfig.getBranch(), "master");
        jsonWriter.add("submodule_folder", gitMaterialConfig.getSubmoduleFolder());
        jsonWriter.add("shallow_clone", gitMaterialConfig.isShallowClone());
    }

    @Override
    public GitMaterialConfig fromJSON(JsonReader jsonReader, ConfigHelperOptions options) {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig();
        jsonReader.readStringIfPresent("url", gitMaterialConfig::setUrl);
        super.fromJSON(jsonReader, gitMaterialConfig, options);
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
