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
package com.thoughtworks.go.apiv11.admin.shared.representers.stages.tasks;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.AbstractTask;
import com.thoughtworks.go.config.OnCancelConfig;
import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.domain.RunIfConfigs;

import java.util.stream.Collectors;

public class BaseTaskRepresenter {

    public static void toJSON(OutputWriter jsonWriter, AbstractTask task) {
        jsonWriter.addChildList("run_if", task.getConditions().stream().map(RunIfConfig::toString).collect(Collectors.toList()));
        if (task.hasCancelTask()) {
            jsonWriter.addChild("on_cancel", attributeWriter -> OnCancelRepresenter.toJSON(attributeWriter, task.getOnCancelConfig()));
        }
    }

    public static AbstractTask fromJSON(JsonReader jsonReader, AbstractTask task) {
        RunIfConfigs runIfConfigs = new RunIfConfigs();
        jsonReader.readArrayIfPresent("run_if", configs -> {
            configs.forEach(runIfConfig -> {
                runIfConfigs.add(new RunIfConfig(runIfConfig.getAsString()));
            });
        });
        task.setConditions(runIfConfigs);
        jsonReader.optJsonObject("on_cancel").ifPresent(onCancelReader -> {
            OnCancelConfig onCancelConfig = OnCancelRepresenter.fromJSON(jsonReader.readJsonObject("on_cancel"));
            task.setOnCancelConfig(onCancelConfig);
        });
        return task;
    }
}
