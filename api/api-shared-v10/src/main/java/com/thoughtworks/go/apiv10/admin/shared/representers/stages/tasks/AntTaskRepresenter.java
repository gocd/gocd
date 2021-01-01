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

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.AntTask;

public class AntTaskRepresenter {

    public static void toJSON(OutputWriter jsonWriter, AntTask antTask) {
        BaseTaskRepresenter.toJSON(jsonWriter, antTask);
        jsonWriter.add("working_directory", antTask.workingDirectory());
        jsonWriter.add("build_file", antTask.getBuildFile());
        jsonWriter.add("target", antTask.getTarget());
    }

    public static AntTask fromJSON(JsonReader jsonReader) {
        AntTask antTask = new AntTask();
        if (jsonReader == null) {
            return antTask;
        }
        BaseTaskRepresenter.fromJSON(jsonReader, antTask);
        jsonReader.readStringIfPresent("working_directory", antTask::setWorkingDirectory);
        jsonReader.readStringIfPresent("build_file", antTask::setBuildFile);
        jsonReader.readStringIfPresent("target", antTask::setTarget);

        return antTask;
    }
}
