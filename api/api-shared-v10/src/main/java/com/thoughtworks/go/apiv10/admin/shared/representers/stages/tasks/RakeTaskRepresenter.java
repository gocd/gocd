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
import com.thoughtworks.go.config.RakeTask;

public class RakeTaskRepresenter {
    public static void toJSON(OutputWriter jsonWriter, RakeTask rakeTask) {
        BaseTaskRepresenter.toJSON(jsonWriter, rakeTask);
        jsonWriter.addIfNotNull("working_directory", rakeTask.workingDirectory());
        jsonWriter.addIfNotNull("build_file", rakeTask.getBuildFile());
        jsonWriter.addIfNotNull("target", rakeTask.getTarget());
    }

    public static RakeTask fromJSON(JsonReader jsonReader) {
        RakeTask rakeTask = new RakeTask();
        if (jsonReader == null) {
            return rakeTask;
        }
        BaseTaskRepresenter.fromJSON(jsonReader, rakeTask);
        jsonReader.readStringIfPresent("working_directory", rakeTask::setWorkingDirectory);
        jsonReader.readStringIfPresent("build_file", rakeTask::setBuildFile);
        jsonReader.readStringIfPresent("target", rakeTask::setTarget);

        return rakeTask;
    }
}
