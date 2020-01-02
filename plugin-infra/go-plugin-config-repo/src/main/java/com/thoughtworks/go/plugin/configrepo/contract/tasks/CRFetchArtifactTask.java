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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRFetchArtifactTask extends CRAbstractFetchTask {
    public static final String ARTIFACT_ORIGIN = "gocd";

    @SerializedName("source")
    @Expose
    private String source;
    @SerializedName("is_source_a_file")
    @Expose
    private boolean isSourceAFile;
    @SerializedName("destination")
    @Expose
    private String destination;

    public CRFetchArtifactTask() {
        this(null, null, null, null, null, null, null, true);
    }

    public CRFetchArtifactTask(CRRunIf runIf, CRTask onCancel, String pipelineName, String stage, String job, String source, String destination, boolean sourceIsDir) {
        super(stage, job, ArtifactOrigin.gocd, runIf, onCancel);
        this.pipeline = pipelineName;
        this.source = source;
        this.isSourceAFile = !sourceIsDir;
        this.destination = destination;
    }

    public boolean sourceIsDirectory() {
        return !isSourceAFile;
    }

    public void setSourceIsDirectory(boolean srcIsDirectory) {
        this.isSourceAFile = !srcIsDirectory;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location, "source", source);
        errors.checkMissing(location, "stage", stage);
        errors.checkMissing(location, "job", job);
    }
}
