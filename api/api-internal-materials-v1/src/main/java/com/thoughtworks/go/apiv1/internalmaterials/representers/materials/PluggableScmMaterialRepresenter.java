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
package com.thoughtworks.go.apiv1.internalmaterials.representers.materials;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;

import java.util.function.Consumer;

public class PluggableScmMaterialRepresenter implements MaterialRepresenter<PluggableSCMMaterialConfig> {

    @Override
    public Consumer<OutputWriter> toJSON(PluggableSCMMaterialConfig pluggableSCMMaterialConfig) {
        return jsonWriter -> {
            jsonWriter.add("ref", pluggableSCMMaterialConfig.getScmId())
                    .add("auto_update", pluggableSCMMaterialConfig.isAutoUpdate())
                    .add("scm_name", pluggableSCMMaterialConfig.getSCMConfig().getName())
                    .addChild("origin", (writer) -> renderOrigin(writer, pluggableSCMMaterialConfig.getSCMConfig().getOrigin()));
        };
    }

    private void renderOrigin(OutputWriter outputWriter, ConfigOrigin origin) {
        if (origin instanceof FileConfigOrigin || origin == null) {
            outputWriter.add("type", "gocd");
        } else if (origin instanceof RepoConfigOrigin) {
            outputWriter.add("type", "config_repo")
                    .add("id", ((RepoConfigOrigin) origin).getConfigRepo().getId());
        }
    }
}
