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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class CRAbstractFetchTask extends CRTask {
    public static final String TYPE_NAME = "fetch";
    @SerializedName("pipeline")
    @Expose
    protected String pipeline;
    @SerializedName("stage")
    @Expose
    protected String stage;
    @SerializedName("job")
    @Expose
    protected String job;
    @SerializedName("artifact_origin")
    @Expose
    protected ArtifactOrigin artifactOrigin;

    protected CRAbstractFetchTask(String stage, String job, ArtifactOrigin artifactOrigin, CRRunIf runIf, CRTask onCancel) {
        super(TYPE_NAME, runIf, onCancel);
        this.stage = stage;
        this.job = job;
        this.artifactOrigin = artifactOrigin;
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String pipe = getPipeline() != null ? getPipeline() : "unknown pipeline";
        String stage = getStage() != null ? getStage() : "unknown stage";
        String job = getJob() != null ? getJob() : "unknown job";

        return String.format("%s; fetch artifacts task from %s %s %s", myLocation, pipe, stage, job);
    }

    public enum ArtifactOrigin {
        gocd {
            @Override
            public Class<? extends CRAbstractFetchTask> getArtifactTaskClass() {
                return CRFetchArtifactTask.class;
            }
        }, external {
            @Override
            public Class<? extends CRAbstractFetchTask> getArtifactTaskClass() {
                return CRFetchPluggableArtifactTask.class;
            }
        };

        public abstract Class<? extends CRAbstractFetchTask> getArtifactTaskClass();

        public static ArtifactOrigin getArtifactOrigin(String origin) {
            return Arrays.stream(values())
                    .filter(item -> item.toString().equals(origin))
                    .findFirst()
                    .orElseThrow(() -> new JsonParseException(String.format("Invalid artifact origin '%s' for fetch task.", origin)));
        }
    }
}
