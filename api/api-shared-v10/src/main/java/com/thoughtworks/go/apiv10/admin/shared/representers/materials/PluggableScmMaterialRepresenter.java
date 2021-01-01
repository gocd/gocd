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
package com.thoughtworks.go.apiv10.admin.shared.representers.materials;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv10.admin.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;

public class PluggableScmMaterialRepresenter implements MaterialRepresenter<PluggableSCMMaterialConfig> {

    @Override
    public void toJSON(OutputWriter jsonWriter, PluggableSCMMaterialConfig pluggableSCMMaterialConfig) {
        jsonWriter.add("ref", pluggableSCMMaterialConfig.getScmId());
        if (pluggableSCMMaterialConfig.filter().isEmpty()) {
            jsonWriter.renderNull("filter");
        } else {
            jsonWriter.addChild("filter", filterWriter -> FilterRepresenter.toJSON(filterWriter, pluggableSCMMaterialConfig.filter()));
        }
        jsonWriter.add("destination", pluggableSCMMaterialConfig.getFolder());
    }

    @Override
    public PluggableSCMMaterialConfig fromJSON(JsonReader jsonReader, ConfigHelperOptions options) {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        CruiseConfig cruiseConfig = options.getCruiseConfig();
        if (cruiseConfig != null) {
            String ref = jsonReader.getString("ref");
            pluggableSCMMaterialConfig.setSCMConfig(cruiseConfig.getSCMs().find(ref));
            pluggableSCMMaterialConfig.setScmId(ref);

        }
        jsonReader.readStringIfPresent("destination", pluggableSCMMaterialConfig::setFolder);
        jsonReader.optJsonObject("filter").ifPresent(filterReader -> {
            pluggableSCMMaterialConfig.setFilter(FilterRepresenter.fromJSON(filterReader));
        });
        return pluggableSCMMaterialConfig;
    }
}
