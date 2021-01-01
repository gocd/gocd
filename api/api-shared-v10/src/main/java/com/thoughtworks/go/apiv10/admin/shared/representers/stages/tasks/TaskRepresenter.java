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
package com.thoughtworks.go.apiv10.admin.shared.representers.stages.tasks;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.Task;

import java.util.HashMap;
import java.util.Optional;

public class TaskRepresenter {

    public static void toJSONArray(OutputListWriter tasksWriter, Tasks tasks) {
        tasks.forEach(task -> {
            tasksWriter.addChild(taskWriter -> toJSON(taskWriter, task));
        });
    }

    public static void toJSON(OutputWriter jsonWriter, Task task) {
        if (!task.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                errorMapping.put("buildFile", "build_file");
                errorMapping.put("onCancelConfig", "on_cancel");
                errorMapping.put("runIf", "run_if");
                errorMapping.put("argListString", "arguments");
                errorMapping.put("src", "source");
                errorMapping.put("dest", "destination");
                errorMapping.put("pipelineName", "pipeline");

                new ErrorGetter(errorMapping).toJSON(errorWriter, task);
            });
        }
        jsonWriter.add("type", task instanceof PluggableTask ? "pluggable_task" : task.getTaskType());
        if (task instanceof PluggableTask) {
            jsonWriter.addChild("attributes", attributeWriter -> PluggableTaskRepresenter.toJSON(attributeWriter, (PluggableTask) task));
            return;
        }
        switch (task.getTaskType()) {
            case AntTask.TYPE:
                jsonWriter.addChild("attributes", attributeWriter -> AntTaskRepresenter.toJSON(attributeWriter, (AntTask) task));
                break;
            case NantTask.TYPE:
                jsonWriter.addChild("attributes", attributeWriter -> NantTaskRepresenter.toJSON(attributeWriter, (NantTask) task));
                break;
            case RakeTask.TYPE:
                jsonWriter.addChild("attributes", attributeWriter -> RakeTaskRepresenter.toJSON(attributeWriter, (RakeTask) task));
                break;
            case ExecTask.TYPE:
                jsonWriter.addChild("attributes", attributeWriter -> ExecTaskRepresenter.toJSON(attributeWriter, (ExecTask) task));
                break;
            case FetchTask.TYPE:
                jsonWriter.addChild("attributes", attributeWriter -> FetchTaskRepresenter.toJSON(attributeWriter, (AbstractFetchTask) task));
                break;
        }
    }

    public static Tasks fromJSONArray(JsonReader jsonReader) {
        Tasks allTasks = new Tasks();
        jsonReader.readArrayIfPresent("tasks", tasks -> {
            tasks.forEach(task -> {
                allTasks.add(fromJSON(new JsonReader(task.getAsJsonObject())));
            });
        });

        return allTasks;
    }

    public static Task fromJSON(JsonReader jsonReader) {
        JsonReader attributes = null;
        String type = jsonReader.getString("type");
        Optional<JsonReader> attributesObject = jsonReader.optJsonObject("attributes");
        if (attributesObject.isPresent()) {
            attributes = attributesObject.get();
        }
        switch (type) {
            case AntTask.TYPE:
                return AntTaskRepresenter.fromJSON(attributes);
            case NantTask.TYPE:
                return NantTaskRepresenter.fromJSON(attributes);
            case RakeTask.TYPE:
                return RakeTaskRepresenter.fromJSON(attributes);
            case ExecTask.TYPE:
                return ExecTaskRepresenter.fromJSON(attributes);
            case FetchTask.TYPE:
                return FetchTaskRepresenter.fromJSON(attributes);
            case PluggableTask.TYPE:
                return PluggableTaskRepresenter.fromJSON(attributes);
            default:
                throw new UnprocessableEntityException(String.format("Invalid task type %s. It has to be one of '%s'.", type, String.join(",", ExecTask.TYPE, AntTask.TYPE, NantTask.TYPE, RakeTask.TYPE, FetchTask.TYPE, PluggableTask.TYPE)));

        }
    }
}
