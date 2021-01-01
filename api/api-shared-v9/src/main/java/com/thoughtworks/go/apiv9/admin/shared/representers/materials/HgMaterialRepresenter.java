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
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;

public class HgMaterialRepresenter extends ScmMaterialRepresenter<HgMaterialConfig> {

    @Override
    public void toJSON(OutputWriter jsonWriter, HgMaterialConfig hgMaterialConfig) {
        super.toJSON(jsonWriter, hgMaterialConfig);
        jsonWriter.addIfNotNull("branch", hgMaterialConfig.getBranchAttribute());
    }

    @Override
    public HgMaterialConfig fromJSON(JsonReader jsonReader, ConfigHelperOptions options) {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig();
        jsonReader.readStringIfPresent("url", hgMaterialConfig::setUrl);
        super.fromJSON(jsonReader, hgMaterialConfig, options);
        jsonReader.readStringIfPresent("branch", hgMaterialConfig::setBranchAttribute);
        return hgMaterialConfig;
    }
}
