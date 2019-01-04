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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;

public class CRFetchArtifactTask extends CRAbstractFetchTask {
    public static final String ARTIFACT_ORIGIN = "gocd";

    private String source;
    private boolean is_source_a_file;
    private String destination;

    public CRFetchArtifactTask() {
        super(TYPE_NAME, ArtifactOrigin.gocd);
    }

    public CRFetchArtifactTask(String stage, String job, String source) {
        super(stage, job, TYPE_NAME, ArtifactOrigin.gocd);
        this.source = source;
    }

    public CRFetchArtifactTask(CRRunIf runIf, CRTask onCancel,
                               String pipelineName, String stage, String job,
                               String source, String destination, boolean sourceIsDir) {
        super(runIf, onCancel);
        this.pipeline = pipelineName;
        this.stage = stage;
        this.job = job;
        this.source = source;
        this.is_source_a_file = !sourceIsDir;
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String dest) {
        this.destination = dest;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String src) {
        this.source = src;
    }

    public boolean sourceIsDirectory() {
        return !is_source_a_file;
    }

    public void setSourceIsDirectory(boolean srcIsDirectory) {
        this.is_source_a_file = !srcIsDirectory;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CRFetchArtifactTask fetchTask = (CRFetchArtifactTask) o;

        if (is_source_a_file != fetchTask.is_source_a_file) {
            return false;
        }
        if (getSource() != null ? !getSource().equals(fetchTask.getSource()) : fetchTask.getSource() != null) {
            return false;
        }
        return getDestination() != null ? getDestination().equals(fetchTask.getDestination()) : fetchTask.getDestination() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getSource() != null ? getSource().hashCode() : 0);
        result = 31 * result + (is_source_a_file ? 1 : 0);
        result = 31 * result + (getDestination() != null ? getDestination().hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location, "source", source);
        errors.checkMissing(location, "stage", stage);
        errors.checkMissing(location, "job", job);
    }
}
