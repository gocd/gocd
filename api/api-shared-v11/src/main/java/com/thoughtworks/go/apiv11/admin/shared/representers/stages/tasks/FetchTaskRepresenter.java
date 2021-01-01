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
package com.thoughtworks.go.apiv11.admin.shared.representers.stages.tasks;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.AbstractFetchTask;
import com.thoughtworks.go.config.FetchPluggableArtifactTask;
import com.thoughtworks.go.config.FetchTask;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;

import java.util.Optional;

public class FetchTaskRepresenter {
    public static void toJSON(OutputWriter jsonWriter, AbstractFetchTask abstractFetchTask) {
        jsonWriter.add("artifact_origin", abstractFetchTask.getArtifactOrigin());
        jsonWriter.add("pipeline", abstractFetchTask.getPipelineName());
        jsonWriter.add("stage", abstractFetchTask.getStage());
        jsonWriter.add("job", abstractFetchTask.getJob());
        BaseTaskRepresenter.toJSON(jsonWriter, abstractFetchTask);

        switch (abstractFetchTask.getArtifactOrigin()) {
            case "gocd":
                representFetchTask(jsonWriter, (FetchTask) abstractFetchTask);
                break;
            case "external":
                representFetchExternalArtifactTask(jsonWriter, (FetchPluggableArtifactTask) abstractFetchTask);
                break;

        }

    }

    private static void representFetchExternalArtifactTask(OutputWriter jsonWriter, FetchPluggableArtifactTask fetchExternalArtifact) {
        jsonWriter.add("artifact_id", fetchExternalArtifact.getArtifactId());
        jsonWriter.addChildList("configuration", configurationWriter -> ConfigurationPropertyRepresenter.toJSON(configurationWriter, fetchExternalArtifact.getConfiguration()));
    }

    private static void representFetchTask(OutputWriter jsonWriter, FetchTask fetchTask) {
        jsonWriter.add("is_source_a_file", fetchTask.isSourceAFile());
        if (fetchTask.isSourceAFile()) {
            jsonWriter.add("source", fetchTask.getRawSrcfile());
        } else {
            jsonWriter.add("source", fetchTask.getRawSrcdir());
        }
        jsonWriter.add("destination", fetchTask.getDest());
    }

    public static AbstractFetchTask fromJSON(JsonReader jsonReader) {
        if (jsonReader == null) {
            return null;
        }
        String origin = jsonReader.getString("artifact_origin");
        switch (origin) {
            case "gocd":
                return fetchTaskFromJson(jsonReader);
            case "external":
                return fetchExternalTaskFromJson(jsonReader);
            default:
                throw new UnprocessableEntityException(String.format("Invalid task type %s. It has to be one of '%s'.", origin, String.join(",", "gocd", "external")));

        }
    }

    private static AbstractFetchTask fetchTaskFromJson(JsonReader jsonReader) {
        FetchTask fetchTask = new FetchTask();
        if (jsonReader == null) {
            return fetchTask;
        }
        setBaseTask(jsonReader, fetchTask);

        Optional<Boolean> isSourceAFileValue = jsonReader.optBoolean("is_source_a_file");
        Boolean isSourceAFile = isSourceAFileValue.orElse(false);
        if (isSourceAFile) {
            jsonReader.readStringIfPresent("source", fetchTask::setSrcfile);
        } else {
            jsonReader.readStringIfPresent("source", fetchTask::setSrcdir);
        }
        jsonReader.readStringIfPresent("destination", fetchTask::setDest);

        return fetchTask;
    }

    private static AbstractFetchTask fetchExternalTaskFromJson(JsonReader jsonReader) {
        FetchPluggableArtifactTask fetchExternalArtifactTask = new FetchPluggableArtifactTask();
        if (jsonReader == null) {
            return fetchExternalArtifactTask;
        }
        setBaseTask(jsonReader, fetchExternalArtifactTask);
        jsonReader.readStringIfPresent("artifact_id", fetchExternalArtifactTask::setArtifactId);
        fetchExternalArtifactTask.addConfigurations(ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "configuration"));

        return fetchExternalArtifactTask;
    }

    private static void setBaseTask(JsonReader jsonReader, AbstractFetchTask abstractFetchArtifactTask) {
        BaseTaskRepresenter.fromJSON(jsonReader, abstractFetchArtifactTask);
        jsonReader.readCaseInsensitiveStringIfPresent("pipeline", abstractFetchArtifactTask::setPipelineName);
        jsonReader.readCaseInsensitiveStringIfPresent("stage", abstractFetchArtifactTask::setStage);
        jsonReader.readCaseInsensitiveStringIfPresent("job", abstractFetchArtifactTask::setJob);
    }
}
