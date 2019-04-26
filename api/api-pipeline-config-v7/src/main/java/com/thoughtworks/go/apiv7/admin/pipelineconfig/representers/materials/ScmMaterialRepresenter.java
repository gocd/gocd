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

package com.thoughtworks.go.apiv7.admin.pipelineconfig.representers.materials;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;

public class ScmMaterialRepresenter {

    public static void toJSON(OutputWriter jsonWriter, ScmMaterialConfig scmMaterialConfig) {
        if (!(scmMaterialConfig instanceof P4MaterialConfig)) {
            jsonWriter.add("url", scmMaterialConfig.getUrl());
        }
        jsonWriter.add("destination", scmMaterialConfig.getFolder());

        if (scmMaterialConfig.filter().isEmpty()) {
            jsonWriter.renderNull("filter");
        } else {
            jsonWriter.addChild("filter", filterWriter -> FilterRepresenter.toJSON(filterWriter, scmMaterialConfig.filter()));
        }
        jsonWriter.add("invert_filter", scmMaterialConfig.isInvertFilter());
        jsonWriter.add("name", scmMaterialConfig.getName());
        jsonWriter.add("auto_update", scmMaterialConfig.isAutoUpdate());
        jsonWriter.addIfNotNull("username", scmMaterialConfig.getUserName());
        jsonWriter.addIfNotNull("encrypted_password", scmMaterialConfig.getEncryptedPassword());
    }

    public static void fromJSON(JsonReader jsonReader, ScmMaterialConfig scmMaterialConfig) {
        jsonReader.readStringIfPresent("url", scmMaterialConfig::setUrl);
        jsonReader.readStringIfPresent("destination", scmMaterialConfig::setFolder);
        jsonReader.optBoolean("invert_filter").ifPresent(scmMaterialConfig::setInvertFilter);
        jsonReader.optJsonObject("filter").ifPresent(filterReader -> {
            scmMaterialConfig.setFilter(FilterRepresenter.fromJSON(filterReader));
        });
        jsonReader.readCaseInsensitiveStringIfPresent("name", scmMaterialConfig::setName);
        jsonReader.optBoolean("auto_update").ifPresent(scmMaterialConfig::setAutoUpdate);
    }
}
