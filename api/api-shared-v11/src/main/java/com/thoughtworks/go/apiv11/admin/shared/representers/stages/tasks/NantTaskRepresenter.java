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
import com.thoughtworks.go.config.NantTask;

public class NantTaskRepresenter extends BaseTaskRepresenter {
    public static void toJSON(OutputWriter jsonWriter, NantTask nantTask) {
        BaseTaskRepresenter.toJSON(jsonWriter, nantTask);
        jsonWriter.addIfNotNull("working_directory", nantTask.workingDirectory());
        jsonWriter.addIfNotNull("build_file", nantTask.getBuildFile());
        jsonWriter.addIfNotNull("target", nantTask.getTarget());
        jsonWriter.addIfNotNull("nant_path", nantTask.getNantPath());
    }

    public static NantTask fromJSON(JsonReader jsonReader) {
        NantTask nantTask = new NantTask();
        if (jsonReader == null) {
            return nantTask;
        }
        BaseTaskRepresenter.fromJSON(jsonReader, nantTask);
        jsonReader.readStringIfPresent("working_directory", nantTask::setWorkingDirectory);
        jsonReader.readStringIfPresent("build_file", nantTask::setBuildFile);
        jsonReader.readStringIfPresent("target", nantTask::setTarget);
        jsonReader.readStringIfPresent("nant_path", nantTask::setNantPath);

        return nantTask;
    }
}
