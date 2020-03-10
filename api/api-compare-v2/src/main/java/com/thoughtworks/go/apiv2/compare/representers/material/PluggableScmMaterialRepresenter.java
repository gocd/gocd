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
package com.thoughtworks.go.apiv2.compare.representers.material;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv2.compare.representers.FilterRepresenter;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;

public class PluggableScmMaterialRepresenter {

    public static void toJSON(OutputWriter jsonWriter, PluggableSCMMaterialConfig pluggableSCMMaterialConfig) {
        jsonWriter.add("ref", pluggableSCMMaterialConfig.getScmId());
        if (pluggableSCMMaterialConfig.filter().isEmpty()) {
            jsonWriter.renderNull("filter");
        } else {
            jsonWriter.addChild("filter", filterWriter -> FilterRepresenter.toJSON(filterWriter, pluggableSCMMaterialConfig.filter()));
        }
        jsonWriter.add("destination", pluggableSCMMaterialConfig.getFolder())
                .add("display_type", pluggableSCMMaterialConfig.getTypeForDisplay())
                .add("description", pluggableSCMMaterialConfig.getLongDescription());
    }
}
