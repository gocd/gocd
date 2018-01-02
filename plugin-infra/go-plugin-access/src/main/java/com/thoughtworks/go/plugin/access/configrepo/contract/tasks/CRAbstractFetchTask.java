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

package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

public abstract class CRAbstractFetchTask extends CRTask {
    protected String pipeline;
    protected String stage;
    protected String job;

    public CRAbstractFetchTask(String type) {
        super(type);
    }

    protected CRAbstractFetchTask(String pipeline, String stage, String job, String type) {
        super(type);
        this.pipeline = pipeline;
        this.stage = stage;
        this.job = job;
    }

    protected CRAbstractFetchTask(String stage, String job, String type) {
        super(type);
        this.stage = stage;
        this.job = job;
    }

    public CRAbstractFetchTask(CRRunIf runIf, CRTask onCancel) {
        super(runIf, onCancel);
    }

    public String getPipelineName() {
        return pipeline;
    }

    public void setPipelineName(String pipeline) {
        this.pipeline = pipeline;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CRAbstractFetchTask that = (CRAbstractFetchTask) o;

        if (getPipelineName() != null ? !getPipelineName().equals(that.getPipelineName()) : that.getPipelineName() != null) {
            return false;
        }
        if (getStage() != null ? !getStage().equals(that.getStage()) : that.getStage() != null) {
            return false;
        }
        return getJob() != null ? getJob().equals(that.getJob()) : that.getJob() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getPipelineName() != null ? getPipelineName().hashCode() : 0);
        result = 31 * result + (getStage() != null ? getStage().hashCode() : 0);
        result = 31 * result + (getJob() != null ? getJob().hashCode() : 0);
        return result;
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String pipe = getPipelineName() != null ? getPipelineName() : "unknown pipeline";
        String stage = getStage() != null ? getStage() : "unknown stage";
        String job = getJob() != null ? getJob() : "unknown job";

        return String.format("%s; fetch artifacts task from %s %s %s", myLocation, pipe, stage, job);
    }
}
