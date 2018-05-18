/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv6.shared.representers.stages.tasks;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.FetchTask;

import java.util.HashMap;
import java.util.Optional;

public class FetchTaskRepresenter {
    public static void toJSON(OutputWriter jsonWriter, FetchTask fetchTask) {
        BaseTaskRepresenter.toJSON(jsonWriter, fetchTask);
        jsonWriter.add("pipeline", fetchTask.getPipelineName());
        jsonWriter.add("stage", fetchTask.getStage());
        jsonWriter.add("job", fetchTask.getJob());
        jsonWriter.add("is_source_a_file", fetchTask.isSourceAFile());
        if (fetchTask.isSourceAFile()) {
            jsonWriter.add("source", fetchTask.getRawSrcfile());
        }
        else {
            jsonWriter.add("source", fetchTask.getRawSrcdir());
        }
        jsonWriter.add("destination", fetchTask.getDest());
    }

    public static FetchTask fromJSON(JsonReader jsonReader) {
        FetchTask fetchTask = new FetchTask();
        BaseTaskRepresenter.fromJSON(jsonReader, fetchTask);
        jsonReader.readCaseInsensitiveStringIfPresent("pipeline", fetchTask::setPipelineName);
        jsonReader.readCaseInsensitiveStringIfPresent("stage", fetchTask::setStage);
        jsonReader.readCaseInsensitiveStringIfPresent("job", fetchTask::setJob);
        Optional<Boolean> isSourceAFileValue = jsonReader.optBoolean("is_source_a_file");
        Boolean isSourceAFile = isSourceAFileValue.get();
        if (isSourceAFile) {
            jsonReader.readStringIfPresent("source", fetchTask::setSrcfile);
        }
        else {
            jsonReader.readStringIfPresent("source", fetchTask::setSrcdir);
        }
        jsonReader.readStringIfPresent("destination", fetchTask::setDest);

        return fetchTask;
    }
}
