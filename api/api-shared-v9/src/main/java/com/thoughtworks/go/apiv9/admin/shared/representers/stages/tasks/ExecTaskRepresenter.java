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
package com.thoughtworks.go.apiv9.admin.shared.representers.stages.tasks;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.Argument;
import com.thoughtworks.go.config.ExecTask;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExecTaskRepresenter {
    public static void toJSON(OutputWriter jsonWriter, ExecTask execTask) {
        BaseTaskRepresenter.toJSON(jsonWriter, execTask);
        jsonWriter.add("command", execTask.command());
        if (execTask.getArgList().isEmpty()) {
            jsonWriter.addIfNotNull("args", execTask.getArgs());

        } else {
            List<String> arguments = execTask.getArgList().stream().map(Argument::getValue).collect(Collectors.toList());
            jsonWriter.addChildList("arguments", arguments);
        }
        jsonWriter.addIfNotNull("working_directory", execTask.workingDirectory());
    }

    public static ExecTask fromJSON(JsonReader jsonReader) {
        ExecTask execTask = new ExecTask();
        if (jsonReader == null) {
            return execTask;
        }
        BaseTaskRepresenter.fromJSON(jsonReader, execTask);
        jsonReader.readStringIfPresent("command", execTask::setCommand);
        jsonReader.readArrayIfPresent("arguments", arguments -> {
            ArrayList<String> argList = new ArrayList<>();
            arguments.forEach(argument -> {
                argList.add(argument.getAsString());
            });
            execTask.setArgsList(argList.toArray(new String[arguments.size()]));
        });
        jsonReader.readStringIfPresent("args", execTask::setArgs);
        jsonReader.readStringIfPresent("working_directory", execTask::setWorkingDirectory);

        return execTask;
    }
}
