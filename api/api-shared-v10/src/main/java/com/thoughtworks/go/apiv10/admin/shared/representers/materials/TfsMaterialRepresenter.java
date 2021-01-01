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
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;

public class TfsMaterialRepresenter extends ScmMaterialRepresenter<TfsMaterialConfig> {

    @Override
    public void toJSON(OutputWriter jsonWriter, TfsMaterialConfig tfsMaterialConfig) {
        super.toJSON(jsonWriter, tfsMaterialConfig);
        jsonWriter.add("domain", tfsMaterialConfig.getDomain());
        jsonWriter.add("project_path", tfsMaterialConfig.getProjectPath());
    }

    @Override
    public TfsMaterialConfig fromJSON(JsonReader jsonReader, ConfigHelperOptions options) {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig();
        super.fromJSON(jsonReader, tfsMaterialConfig, options);
        jsonReader.readStringIfPresent("url", tfsMaterialConfig::setUrl);
        jsonReader.readStringIfPresent("domain", tfsMaterialConfig::setDomain);
        jsonReader.readStringIfPresent("project_path", tfsMaterialConfig::setProjectPath);
        return tfsMaterialConfig;
    }
}
