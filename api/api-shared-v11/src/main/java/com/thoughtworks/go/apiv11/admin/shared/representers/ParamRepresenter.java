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
package com.thoughtworks.go.apiv11.admin.shared.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.ParamConfig;
import com.thoughtworks.go.config.ParamsConfig;

import java.util.HashMap;

public class ParamRepresenter {

    public static void toJSONArray(OutputListWriter jsonWriter, ParamsConfig paramConfigs) {
        paramConfigs.forEach(param -> {
            jsonWriter.addChild(paramWriter -> toJSON(paramWriter, param));
        });
    }

    public static void toJSON(OutputWriter jsonWriter, ParamConfig paramConfig) {
        if (!paramConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                new ErrorGetter(new HashMap<>()).toJSON(errorWriter, paramConfig);
            });
        }
        jsonWriter.add("name", paramConfig.getName());
        jsonWriter.add("value", paramConfig.getValue());
    }

    public static ParamsConfig fromJSONArray(JsonReader jsonReader) {
        ParamsConfig paramConfigs = new ParamsConfig();
        jsonReader.readArrayIfPresent("parameters", params -> {
            params.forEach(param -> {
                paramConfigs.add(ParamRepresenter.fromJSON(new JsonReader(param.getAsJsonObject())));
            });
        });
        return paramConfigs;
    }

    private static ParamConfig fromJSON(JsonReader jsonReader) {
        ParamConfig paramConfig = new ParamConfig();
        jsonReader.readStringIfPresent("name", paramConfig::setName);
        jsonReader.readStringIfPresent("value", paramConfig::setValue);
        return paramConfig;
    }

}
