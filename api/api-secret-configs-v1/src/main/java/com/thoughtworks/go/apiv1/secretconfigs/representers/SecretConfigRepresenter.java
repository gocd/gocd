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

package com.thoughtworks.go.apiv1.secretconfigs.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.SecretConfig;
import org.apache.commons.collections4.CollectionUtils;

public class SecretConfigRepresenter {
    public static void toJSON(OutputWriter jsonWriter, SecretConfig secretConfig) {
        if (secretConfig == null)
            return;

        jsonWriter.add("id", secretConfig.getId());
        jsonWriter.add("plugin_id", secretConfig.getPluginId());
        jsonWriter.addIfNotNull("description", secretConfig.getDescription());

        jsonWriter.addChildList("properties", listWriter -> {
            ConfigurationPropertyRepresenter.toJSON(listWriter, secretConfig);
        });

        if (!CollectionUtils.isEmpty(secretConfig.getRules())) {
            jsonWriter.addChild("rules", rulesWriter -> RulesRepresenter.toJSON(rulesWriter, secretConfig.getRules()));
        }
    }

    public static SecretConfig fromJSON(JsonReader jsonReader) {
        SecretConfig secretConfig = new SecretConfig(jsonReader.getString("id"), jsonReader.getString("plugin_id"));
        jsonReader.optString("description").ifPresent(description -> secretConfig.setDescription(description));
        secretConfig.addConfigurations(ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "properties"));

        if (jsonReader.optJsonObject("rules").isPresent()) {
            secretConfig.setRules(RulesRepresenter.fromJSON(jsonReader.readJsonObject("rules")));
        }
        return secretConfig;
    }
}
