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
package com.thoughtworks.go.apiv10.admin.shared.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.TimerConfig;

import java.util.HashMap;

public class TimerRepresenter {

    public static void toJSON(OutputWriter jsonWriter, TimerConfig timerConfig) {
        if (timerConfig == null) {
            return;
        }
        if (!timerConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> mapping = new HashMap<>();
                mapping.put("timerSpec", "spec");
                new ErrorGetter(mapping).toJSON(errorWriter, timerConfig);
            });
        }

        jsonWriter.add("spec", timerConfig.getTimerSpec());
        jsonWriter.add("only_on_changes", timerConfig.getOnlyOnChanges());
    }

    public static TimerConfig fromJSON(JsonReader jsonReader) {
        TimerConfig timerConfig = new TimerConfig();
        jsonReader.readStringIfPresent("spec", timerConfig::setTimerSpec);
        jsonReader.optBoolean("only_on_changes").ifPresent(timerConfig::setOnlyOnChanges);

        return timerConfig;
    }
}
